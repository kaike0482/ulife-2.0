package br.com.ulife.dao;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Consultas e ações do aluno nas seções Aulas e Curso (quadro, atividades, avaliações, extensão, documentos...). */
public class PortalAlunoDAO {

    // ------------------------------------------------------------------ tipos

    public record Aula(String codigo, String turma, String professor, int dia, String inicio, String fim,
                       String modalidade, String local) {}
    public record Gravacao(String turma, LocalDate data, String titulo, String url) {}
    public record UC(String codigo, String turma, String professor, int progresso, double horasMinistradas, int cargaHoraria) {}
    public record Atividade(int tarefaId, String turma, String titulo, String descricao, LocalDateTime prazo,
                            BigDecimal peso, String status, String protocolo) {}
    public record Plano(String turma, String professor, String ementa, String objetivos, String conteudo,
                        String criterios, String bibliografia) {}
    public record Horario(int horarioId, LocalDateTime dataHora, int vagas, int ocupadas, boolean meu) {}
    public record Avaliacao(int avaliacaoId, String turma, String descricao, LocalDate agendarAte, List<Horario> horarios) {}
    public record ItemAcervo(String titulo, String autor, String tipo, String url, String localizacao, int exemplares) {}
    public record Extensao(int id, String nome, String descricao, int horas, LocalDate ate, int vagas, int inscritos,
                           String minhaInscricao) {}
    public record AtivComp(int id, String aluno, String descricao, String categoria, int horas, String status, String parecer) {}
    public record ItemCurriculo(int semestre, String nome, int horas, String status) {}
    public record Documento(String tipo, String codigo, LocalDateTime emitidoEm) {}
    public record Hibridez(double horasDigitais, double horasTotais) {
        public double percentual() { return horasTotais <= 0 ? 0 : horasDigitais * 100.0 / horasTotais; }
    }

    public static final long TAMANHO_MAX_MB = 50;
    private static final String[] EXTENSOES = {"pdf", "doc", "docx", "ppt", "pptx", "zip", "txt", "png", "jpg", "jpeg"};

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    @FunctionalInterface
    private interface Mapa<T> { T de(ResultSet rs) throws SQLException; }

    private <T> List<T> consultar(String sql, Mapa<T> mapa, Object... p) {
        List<T> l = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) {
                ps.setObject(i + 1, p[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(mapa.de(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar: " + e.getMessage(), e);
        }
        return l;
    }

    private int atualizar(String sql, Object... p) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) {
                ps.setObject(i + 1, p[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao gravar: " + e.getMessage(), e);
        }
    }

    /** RA do aluno ("—" se o usuário não tem cadastro de aluno). */
    public String ra(int usuarioId) {
        List<String> l = consultar("SELECT ra FROM ALUNO WHERE aluno_id = ?", rs -> rs.getString(1), usuarioId);
        return l.isEmpty() ? "—" : l.get(0);
    }

    // ------------------------------------------------------------------ AULAS

    /** Aulas semanais das turmas em que o aluno está matriculado. */
    public List<Aula> quadro(int usuarioId) {
        return consultar("SELECT t.codigo, t.nome, u.nome, t.dia_semana, t.hora_inicio, t.hora_fim, t.modalidade, t.local_aula "
                + "FROM MATRICULA m JOIN TURMA t ON t.turma_id = m.turma_id JOIN USUARIO u ON u.usuario_id = t.professor_id "
                + "WHERE m.aluno_id = ? AND m.status = 'ATIVA' AND t.ativo = TRUE AND t.dia_semana IS NOT NULL ORDER BY t.dia_semana",
                rs -> new Aula(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                        rs.getString(5).substring(0, 5), rs.getString(6).substring(0, 5), rs.getString(7), rs.getString(8)),
                usuarioId);
    }

    public List<Gravacao> gravacoes(int usuarioId) {
        return consultar("SELECT t.nome, g.aula_data, g.titulo, g.url FROM GRAVACAO g JOIN TURMA t ON t.turma_id = g.turma_id "
                + "JOIN MATRICULA m ON m.turma_id = t.turma_id WHERE m.aluno_id = ? AND m.status = 'ATIVA' "
                + "ORDER BY g.aula_data DESC, t.nome",
                rs -> new Gravacao(rs.getString(1), rs.getDate(2).toLocalDate(), rs.getString(3), rs.getString(4)), usuarioId);
    }

    /** Unidades de curso do aluno, com progresso calculado pelas aulas já lançadas. */
    public List<UC> ucs(int usuarioId) {
        return consultar("SELECT t.codigo, t.nome, u.nome, t.carga_horaria_total, "
                + "(SELECT COUNT(DISTINCT f.aula_data) FROM FREQUENCIA f WHERE f.matricula_id = m.matricula_id AND f.lancada = TRUE) "
                + "* TIMESTAMPDIFF(MINUTE, t.hora_inicio, t.hora_fim) / 60 AS horas "
                + "FROM MATRICULA m JOIN TURMA t ON t.turma_id = m.turma_id JOIN USUARIO u ON u.usuario_id = t.professor_id "
                + "WHERE m.aluno_id = ? AND m.status = 'ATIVA' AND t.ativo = TRUE ORDER BY t.nome",
                rs -> {
                    int carga = rs.getInt(4);
                    double horas = rs.getDouble(5);
                    return new UC(rs.getString(1), rs.getString(2), rs.getString(3),
                            (int) Math.min(100, Math.round(carga <= 0 ? 0 : horas * 100 / carga)), horas, carga);
                }, usuarioId);
    }

    public List<Atividade> atividades(int usuarioId) {
        return consultar("SELECT ta.tarefa_id, t.nome, ta.titulo, ta.descricao, ta.prazo_entrega, ta.peso, e.protocolo_digital "
                + "FROM TAREFA ta JOIN TURMA t ON t.turma_id = ta.turma_id "
                + "JOIN MATRICULA m ON m.turma_id = t.turma_id AND m.aluno_id = ? AND m.status = 'ATIVA' "
                + "LEFT JOIN ENTREGA e ON e.tarefa_id = ta.tarefa_id AND e.aluno_id = ? "
                + "WHERE ta.ativa = TRUE ORDER BY ta.prazo_entrega",
                rs -> {
                    LocalDateTime prazo = rs.getTimestamp(5).toLocalDateTime();
                    String protocolo = rs.getString(7);
                    String status;
                    if (protocolo != null) {
                        status = "Entregue";
                    } else if (prazo.isBefore(LocalDateTime.now())) {
                        status = "Atrasada";
                    } else if (prazo.isBefore(LocalDateTime.now().plusDays(7))) {
                        status = "Pendente";
                    } else {
                        status = "Em breve";
                    }
                    return new Atividade(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), prazo,
                            rs.getBigDecimal(6), status, protocolo);
                }, usuarioId, usuarioId);
    }

    /**
     * Entrega de atividade (RF-003): valida arquivo (formato e tamanho), prazo e matrícula; guarda uma cópia
     * em ./uploads e registra o protocolo digital.
     *
     * @return protocolo digital gerado
     */
    public String entregar(int tarefaId, int usuarioId, File arquivo) {
        if (arquivo == null || !arquivo.isFile()) {
            throw new IllegalArgumentException("Selecione um arquivo.");
        }
        String nome = arquivo.getName();
        String ext = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (java.util.Arrays.stream(EXTENSOES).noneMatch(ext::equals)) {
            throw new IllegalArgumentException("Formato não aceito. Use: " + String.join(", ", EXTENSOES) + ".");
        }
        if (arquivo.length() > TAMANHO_MAX_MB * 1024 * 1024) {
            throw new IllegalArgumentException("O arquivo passa de " + TAMANHO_MAX_MB + " MB.");
        }
        try (Connection c = db.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT ta.prazo_entrega, ta.ativa, "
                    + "(SELECT COUNT(*) FROM ENTREGA e WHERE e.tarefa_id = ta.tarefa_id AND e.aluno_id = ?) "
                    + "FROM TAREFA ta JOIN MATRICULA m ON m.turma_id = ta.turma_id AND m.aluno_id = ? AND m.status = 'ATIVA' "
                    + "WHERE ta.tarefa_id = ?")) {
                ps.setInt(1, usuarioId);
                ps.setInt(2, usuarioId);
                ps.setInt(3, tarefaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !rs.getBoolean(2)) {
                        throw new IllegalStateException("Atividade não disponível para você.");
                    }
                    if (rs.getTimestamp(1).toLocalDateTime().isBefore(LocalDateTime.now())) {
                        throw new IllegalStateException("O prazo de entrega desta atividade expirou.");
                    }
                    if (rs.getInt(3) > 0) {
                        throw new IllegalStateException("Você já entregou esta atividade.");
                    }
                }
            }
            String protocolo = "ULF-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Path destino = Path.of("uploads", protocolo + "_" + nome.replaceAll("[^\\w.\\-]", "_"));
            try {
                Files.createDirectories(destino.getParent());
                Files.copy(arquivo.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Não foi possível guardar o arquivo: " + e.getMessage(), e);
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO ENTREGA (tarefa_id, aluno_id, protocolo_digital, "
                    + "arquivo_nome, arquivo_caminho, tamanho_arquivo_bytes, prazo_valido, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, TRUE, 'ENTREGUE')")) {
                ps.setInt(1, tarefaId);
                ps.setInt(2, usuarioId);
                ps.setString(3, protocolo);
                ps.setString(4, nome);
                ps.setString(5, destino.toString());
                ps.setLong(6, arquivo.length());
                ps.executeUpdate();
            }
            return protocolo;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao registrar a entrega: " + e.getMessage(), e);
        }
    }

    public List<Plano> planos(int usuarioId) {
        return consultar("SELECT t.nome, u.nome, p.ementa, p.objetivos, p.conteudo, p.criterios, p.bibliografia "
                + "FROM PLANO_ENSINO p JOIN TURMA t ON t.turma_id = p.turma_id JOIN USUARIO u ON u.usuario_id = t.professor_id "
                + "JOIN MATRICULA m ON m.turma_id = t.turma_id WHERE m.aluno_id = ? AND m.status = 'ATIVA' ORDER BY t.nome",
                rs -> new Plano(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getString(7)), usuarioId);
    }

    // ------------------------------------------------------------------ avaliações

    public List<Avaliacao> avaliacoes(int usuarioId) {
        List<Avaliacao> lista = new ArrayList<>();
        List<int[]> ids = new ArrayList<>();
        List<Object[]> cab = consultar("SELECT a.avaliacao_id, t.nome, a.descricao, a.agendar_ate FROM AVALIACAO a "
                + "JOIN TURMA t ON t.turma_id = a.turma_id JOIN MATRICULA m ON m.turma_id = t.turma_id "
                + "WHERE m.aluno_id = ? AND m.status = 'ATIVA' ORDER BY a.agendar_ate, t.nome",
                rs -> new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDate(4).toLocalDate()}, usuarioId);
        for (Object[] c : cab) {
            List<Horario> hs = consultar("SELECT h.horario_id, h.data_hora, h.vagas, "
                    + "(SELECT COUNT(*) FROM AGENDAMENTO_AVALIACAO g WHERE g.horario_id = h.horario_id), "
                    + "(SELECT COUNT(*) FROM AGENDAMENTO_AVALIACAO g WHERE g.horario_id = h.horario_id AND g.aluno_id = ?) "
                    + "FROM HORARIO_AVALIACAO h WHERE h.avaliacao_id = ? ORDER BY h.data_hora",
                    rs -> new Horario(rs.getInt(1), rs.getTimestamp(2).toLocalDateTime(), rs.getInt(3), rs.getInt(4),
                            rs.getInt(5) > 0), usuarioId, c[0]);
            lista.add(new Avaliacao((Integer) c[0], (String) c[1], (String) c[2], (LocalDate) c[3], hs));
        }
        return lista;
    }

    /** Agenda (ou remarca) o horário da avaliação, respeitando prazo de agendamento e vagas. */
    public void agendar(int horarioId, int usuarioId) {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                int avaliacaoId;
                try (PreparedStatement ps = c.prepareStatement("SELECT a.avaliacao_id, a.agendar_ate, h.vagas FROM HORARIO_AVALIACAO h "
                        + "JOIN AVALIACAO a ON a.avaliacao_id = h.avaliacao_id JOIN MATRICULA m ON m.turma_id = a.turma_id "
                        + "AND m.aluno_id = ? AND m.status = 'ATIVA' WHERE h.horario_id = ? FOR UPDATE")) {
                    ps.setInt(1, usuarioId);
                    ps.setInt(2, horarioId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Horário não disponível para você.");
                        }
                        if (rs.getDate(2).toLocalDate().isBefore(LocalDate.now())) {
                            throw new IllegalStateException("O prazo de agendamento terminou.");
                        }
                        avaliacaoId = rs.getInt(1);
                        int vagas = rs.getInt(3);
                        try (PreparedStatement q = c.prepareStatement(
                                "SELECT COUNT(*) FROM AGENDAMENTO_AVALIACAO WHERE horario_id = ? AND aluno_id <> ?")) {
                            q.setInt(1, horarioId);
                            q.setInt(2, usuarioId);
                            try (ResultSet r2 = q.executeQuery()) {
                                r2.next();
                                if (r2.getInt(1) >= vagas) {
                                    throw new IllegalStateException("Este horário não tem mais vagas.");
                                }
                            }
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE g FROM AGENDAMENTO_AVALIACAO g "
                        + "JOIN HORARIO_AVALIACAO h ON h.horario_id = g.horario_id WHERE h.avaliacao_id = ? AND g.aluno_id = ?")) {
                    ps.setInt(1, avaliacaoId);
                    ps.setInt(2, usuarioId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO AGENDAMENTO_AVALIACAO (horario_id, aluno_id) VALUES (?, ?)")) {
                    ps.setInt(1, horarioId);
                    ps.setInt(2, usuarioId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao agendar: " + e.getMessage(), e);
        }
    }

    public void cancelarAgendamento(int horarioId, int usuarioId) {
        atualizar("DELETE FROM AGENDAMENTO_AVALIACAO WHERE horario_id = ? AND aluno_id = ?", horarioId, usuarioId);
    }

    // ------------------------------------------------------------------ bibliotecas

    public List<ItemAcervo> acervo(String tipo, String filtro) {
        return consultar("SELECT titulo, autor, tipo, url, localizacao, exemplares FROM ACERVO WHERE tipo = ? "
                + "AND CONCAT_WS(' ', titulo, autor) LIKE ? ORDER BY titulo",
                rs -> new ItemAcervo(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getInt(6)), tipo, "%" + (filtro == null ? "" : filtro.trim()) + "%");
    }

    // ------------------------------------------------------------------ CURSO: extensão

    public List<Extensao> extensao(int usuarioId) {
        return consultar("SELECT e.extensao_id, e.nome, e.descricao, e.carga_horaria, e.inscricoes_ate, e.vagas, "
                + "(SELECT COUNT(*) FROM INSCRICAO_EXTENSAO i WHERE i.extensao_id = e.extensao_id AND i.status <> 'CANCELADO'), "
                + "(SELECT i.status FROM INSCRICAO_EXTENSAO i WHERE i.extensao_id = e.extensao_id AND i.aluno_id = ?) "
                + "FROM EXTENSAO e ORDER BY e.nome",
                rs -> new Extensao(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getDate(5).toLocalDate(),
                        rs.getInt(6), rs.getInt(7), rs.getString(8)), usuarioId);
    }

    public void inscreverExtensao(int extensaoId, int usuarioId) {
        for (Extensao e : extensao(usuarioId)) {
            if (e.id() == extensaoId) {
                if (e.ate().isBefore(LocalDate.now())) {
                    throw new IllegalStateException("As inscrições deste programa terminaram.");
                }
                if ("INSCRITO".equals(e.minhaInscricao()) || "CONCLUIDO".equals(e.minhaInscricao())) {
                    throw new IllegalStateException("Você já está inscrito neste programa.");
                }
                if (e.inscritos() >= e.vagas()) {
                    throw new IllegalStateException("Não há mais vagas.");
                }
                atualizar("INSERT INTO INSCRICAO_EXTENSAO (extensao_id, aluno_id, status) VALUES (?, ?, 'INSCRITO') "
                        + "ON DUPLICATE KEY UPDATE status = 'INSCRITO'", extensaoId, usuarioId);
                return;
            }
        }
        throw new IllegalStateException("Programa não encontrado.");
    }

    public void cancelarExtensao(int extensaoId, int usuarioId) {
        atualizar("UPDATE INSCRICAO_EXTENSAO SET status = 'CANCELADO' WHERE extensao_id = ? AND aluno_id = ? AND status = 'INSCRITO'",
                extensaoId, usuarioId);
    }

    // ------------------------------------------------------------------ CURSO: atividades complementares

    public List<AtivComp> atividadesComplementares(int usuarioId) {
        return consultar("SELECT a.atividade_id, u.nome, a.descricao, a.categoria, a.carga_horaria, a.status, a.parecer "
                + "FROM ATIVIDADE_COMPLEMENTAR a JOIN USUARIO u ON u.usuario_id = a.aluno_id WHERE a.aluno_id = ? "
                + "ORDER BY a.registrado_em DESC, a.atividade_id DESC",
                rs -> new AtivComp(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                        rs.getString(6), rs.getString(7)), usuarioId);
    }

    public void registrarAtividadeComplementar(int usuarioId, String descricao, String categoria, int horas, File comprovante) {
        String caminho = null;
        if (comprovante != null) {
            if (comprovante.length() > TAMANHO_MAX_MB * 1024 * 1024) {
                throw new IllegalArgumentException("O comprovante passa de " + TAMANHO_MAX_MB + " MB.");
            }
            try {
                Path dest = Path.of("uploads", "comprovantes",
                        UUID.randomUUID().toString().substring(0, 8) + "_" + comprovante.getName().replaceAll("[^\\w.\\-]", "_"));
                Files.createDirectories(dest.getParent());
                Files.copy(comprovante.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                caminho = dest.toString();
            } catch (IOException e) {
                throw new RuntimeException("Não foi possível guardar o comprovante: " + e.getMessage(), e);
            }
        }
        atualizar("INSERT INTO ATIVIDADE_COMPLEMENTAR (aluno_id, descricao, categoria, carga_horaria, status, comprovante) "
                + "VALUES (?, ?, ?, ?, 'EM_ANALISE', ?)", usuarioId, descricao, categoria, horas, caminho);
    }

    /** Atividades de todos os alunos aguardando validação (coordenação). */
    public List<AtivComp> atividadesEmAnalise() {
        return consultar("SELECT a.atividade_id, u.nome, a.descricao, a.categoria, a.carga_horaria, a.status, a.parecer "
                + "FROM ATIVIDADE_COMPLEMENTAR a JOIN USUARIO u ON u.usuario_id = a.aluno_id WHERE a.status = 'EM_ANALISE' "
                + "ORDER BY a.registrado_em",
                rs -> new AtivComp(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                        rs.getString(6), rs.getString(7)));
    }

    public void decidirAtividade(int atividadeId, boolean validar, String parecer) {
        atualizar("UPDATE ATIVIDADE_COMPLEMENTAR SET status = ?, parecer = ? WHERE atividade_id = ? AND status = 'EM_ANALISE'",
                validar ? "VALIDADA" : "REJEITADA", parecer, atividadeId);
    }

    public int horasComplementaresValidadas(int usuarioId) {
        List<Integer> l = consultar("SELECT COALESCE(SUM(carga_horaria), 0) FROM ATIVIDADE_COMPLEMENTAR "
                + "WHERE aluno_id = ? AND status = 'VALIDADA'", rs -> rs.getInt(1), usuarioId);
        return l.get(0);
    }

    // ------------------------------------------------------------------ CURSO: plano curricular, hibridez, regularidade

    public List<ItemCurriculo> curriculo(int usuarioId) {
        return consultar("SELECT uc.semestre, uc.nome, uc.carga_horaria, CASE "
                + "WHEN EXISTS (SELECT 1 FROM MATRICULA m JOIN TURMA t ON t.turma_id = m.turma_id WHERE m.aluno_id = ? "
                + "AND m.status = 'ATIVA' AND t.uc_id = uc.uc_id) THEN 'Em andamento' "
                + "WHEN uc.semestre < COALESCE((SELECT semestre_atual FROM ALUNO WHERE aluno_id = ?), 1) THEN 'Concluída' "
                + "ELSE 'A cursar' END FROM UNIDADE_CURRICULAR uc ORDER BY uc.semestre, uc.nome",
                rs -> new ItemCurriculo(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4)), usuarioId, usuarioId);
    }

    /** Horas cursadas em formato digital sobre o total de horas já ministradas nas turmas do aluno. */
    public Hibridez hibridez(int usuarioId) {
        double dig = 0, tot = 0;
        for (UC uc : ucs(usuarioId)) {
            tot += uc.horasMinistradas();
        }
        List<Double> d = consultar("SELECT COALESCE(SUM((SELECT COUNT(DISTINCT f.aula_data) FROM FREQUENCIA f "
                + "WHERE f.matricula_id = m.matricula_id AND f.lancada = TRUE) * TIMESTAMPDIFF(MINUTE, t.hora_inicio, t.hora_fim) / 60), 0) "
                + "FROM MATRICULA m JOIN TURMA t ON t.turma_id = m.turma_id "
                + "WHERE m.aluno_id = ? AND m.status = 'ATIVA' AND t.modalidade = 'DIGITAL'", rs -> rs.getDouble(1), usuarioId);
        dig = d.get(0);
        return new Hibridez(dig, tot);
    }

    /** Aluno regular: matrícula ativa e nenhuma cobrança vencida. */
    public String motivoIrregular(int usuarioId) {
        int mats = consultar("SELECT COUNT(*) FROM MATRICULA WHERE aluno_id = ? AND status = 'ATIVA'", rs -> rs.getInt(1), usuarioId).get(0);
        if (mats == 0) {
            return "Você não possui matrícula ativa.";
        }
        int venc = consultar("SELECT COUNT(*) FROM COBRANCA WHERE usuario_id = ? AND (status = 'VENCIDA' "
                + "OR (status = 'ABERTA' AND vencimento < CURDATE()))", rs -> rs.getInt(1), usuarioId).get(0);
        return venc > 0 ? "Há mensalidade vencida em aberto. Regularize em Pagamentos." : null;
    }

    public boolean curriculoCompleto(int usuarioId) {
        return curriculo(usuarioId).stream().allMatch(i -> "Concluída".equals(i.status()));
    }

    public int extensoesConcluidas(int usuarioId) {
        return consultar("SELECT COUNT(*) FROM INSCRICAO_EXTENSAO WHERE aluno_id = ? AND status = 'CONCLUIDO'",
                rs -> rs.getInt(1), usuarioId).get(0);
    }

    // ------------------------------------------------------------------ documentos emitidos

    public Documento registrarDocumento(int usuarioId, String tipo) {
        String codigo = "ULF-DOC-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        atualizar("INSERT INTO DOCUMENTO_EMITIDO (aluno_id, tipo, codigo) VALUES (?, ?, ?)", usuarioId, tipo, codigo);
        return new Documento(tipo, codigo, LocalDateTime.now());
    }

    public List<Documento> documentos(int usuarioId) {
        return consultar("SELECT tipo, codigo, emitido_em FROM DOCUMENTO_EMITIDO WHERE aluno_id = ? ORDER BY emitido_em DESC",
                rs -> new Documento(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toLocalDateTime()), usuarioId);
    }
}
