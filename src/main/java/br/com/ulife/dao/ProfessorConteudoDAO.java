package br.com.ulife.dao;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Conteúdo publicado pelo professor nas suas turmas: atividades (com as entregas dos alunos e a correção) e materiais.
 * Todas as operações conferem se a turma pertence ao professor.
 */
public class ProfessorConteudoDAO {

    public record TarefaProf(int id, String titulo, String descricao, LocalDateTime prazo, BigDecimal peso,
                             int alunos, int entregas, int corrigidas) {}
    public record EntregaProf(int id, String aluno, String ra, LocalDateTime data, String protocolo, String arquivoNome,
                              String caminho, String status, BigDecimal nota, String feedback) {}
    public record MaterialProf(int id, String titulo, String arquivoNome, String caminho, long tamanho,
                               LocalDateTime publicado) {}

    public static final long TAMANHO_MAX_MB = 50;
    private static final String[] EXTENSOES_MATERIAL = {"pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "zip", "txt",
        "png", "jpg", "jpeg"};

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

    private void exigirDono(int turmaId, int professorId) {
        int n = consultar("SELECT COUNT(*) FROM TURMA WHERE turma_id = ? AND professor_id = ?", rs -> rs.getInt(1),
                turmaId, professorId).get(0);
        if (n == 0) {
            throw new IllegalStateException("Esta turma não pertence a você.");
        }
    }

    // ------------------------------------------------------------------ atividades

    public List<TarefaProf> tarefas(int turmaId, int professorId) {
        exigirDono(turmaId, professorId);
        return consultar("SELECT ta.tarefa_id, ta.titulo, ta.descricao, ta.prazo_entrega, ta.peso, "
                + "(SELECT COUNT(*) FROM MATRICULA m WHERE m.turma_id = ta.turma_id AND m.status = 'ATIVA'), "
                + "(SELECT COUNT(*) FROM ENTREGA e WHERE e.tarefa_id = ta.tarefa_id), "
                + "(SELECT COUNT(*) FROM ENTREGA e WHERE e.tarefa_id = ta.tarefa_id AND e.status = 'CORRIGIDO') "
                + "FROM TAREFA ta WHERE ta.turma_id = ? AND ta.ativa = TRUE ORDER BY ta.prazo_entrega DESC",
                rs -> new TarefaProf(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toLocalDateTime(),
                        rs.getBigDecimal(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)), turmaId);
    }

    public void criarTarefa(int turmaId, int professorId, String titulo, String descricao, LocalDateTime prazo,
                            BigDecimal peso) {
        exigirDono(turmaId, professorId);
        if (titulo == null || titulo.trim().isEmpty() || titulo.length() > 150) {
            throw new IllegalArgumentException("Informe o título (até 150 caracteres).");
        }
        if (prazo == null || !prazo.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("O prazo de entrega deve ser uma data futura.");
        }
        if (peso == null || peso.signum() < 0 || peso.compareTo(BigDecimal.TEN) > 0 || peso.scale() > 2) {
            throw new IllegalArgumentException("O peso deve ficar entre 0 e 10.");
        }
        atualizar("INSERT INTO TAREFA (turma_id, titulo, descricao, tipo, arquivo_obrigatorio, tamanho_max_mb, prazo_entrega, peso) "
                + "VALUES (?, ?, ?, 'Trabalho', TRUE, ?, ?, ?)", turmaId, titulo.trim(),
                descricao == null ? null : descricao.trim(), TAMANHO_MAX_MB, prazo, peso);
    }

    public List<EntregaProf> entregas(int tarefaId, int professorId) {
        return consultar("SELECT e.entrega_id, u.nome, a.ra, e.data_entrega, e.protocolo_digital, e.arquivo_nome, "
                + "e.arquivo_caminho, e.status, e.nota, e.feedback FROM ENTREGA e "
                + "JOIN TAREFA ta ON ta.tarefa_id = e.tarefa_id JOIN TURMA t ON t.turma_id = ta.turma_id "
                + "JOIN ALUNO a ON a.aluno_id = e.aluno_id JOIN USUARIO u ON u.usuario_id = a.aluno_id "
                + "WHERE e.tarefa_id = ? AND t.professor_id = ? ORDER BY u.nome",
                rs -> new EntregaProf(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toLocalDateTime(),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getBigDecimal(9),
                        rs.getString(10)), tarefaId, professorId);
    }

    /** Avalia a entrega: nota de 0 a 10 e comentário; a entrega passa a CORRIGIDO. */
    public void corrigir(int entregaId, int professorId, BigDecimal nota, String feedback) {
        if (nota == null || nota.signum() < 0 || nota.compareTo(BigDecimal.TEN) > 0 || nota.scale() > 2) {
            throw new IllegalArgumentException("A nota deve ficar entre 0 e 10.");
        }
        int n = atualizar("UPDATE ENTREGA e JOIN TAREFA ta ON ta.tarefa_id = e.tarefa_id JOIN TURMA t ON t.turma_id = ta.turma_id "
                + "SET e.nota = ?, e.feedback = ?, e.status = 'CORRIGIDO' WHERE e.entrega_id = ? AND t.professor_id = ?",
                nota, feedback == null || feedback.trim().isEmpty() ? null : feedback.trim(), entregaId, professorId);
        if (n != 1) {
            throw new IllegalStateException("Entrega não encontrada nas suas turmas.");
        }
    }

    // ------------------------------------------------------------------ materiais

    public List<MaterialProf> materiais(int turmaId, int professorId) {
        exigirDono(turmaId, professorId);
        return consultar("SELECT material_id, titulo, arquivo_nome, arquivo_caminho, tamanho_bytes, publicado_em "
                + "FROM MATERIAL WHERE turma_id = ? ORDER BY publicado_em DESC, material_id DESC",
                rs -> new MaterialProf(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5),
                        rs.getTimestamp(6).toLocalDateTime()), turmaId);
    }

    /** Publica um arquivo para os alunos da turma (guarda uma cópia em ./uploads/materiais). */
    public void enviarMaterial(int turmaId, int professorId, String titulo, File arquivo) {
        exigirDono(turmaId, professorId);
        if (titulo == null || titulo.trim().isEmpty() || titulo.length() > 150) {
            throw new IllegalArgumentException("Informe o título (até 150 caracteres).");
        }
        if (arquivo == null || !arquivo.isFile()) {
            throw new IllegalArgumentException("Selecione um arquivo.");
        }
        String nome = arquivo.getName();
        String ext = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (java.util.Arrays.stream(EXTENSOES_MATERIAL).noneMatch(ext::equals)) {
            throw new IllegalArgumentException("Formato não aceito. Use: " + String.join(", ", EXTENSOES_MATERIAL) + ".");
        }
        if (arquivo.length() > TAMANHO_MAX_MB * 1024 * 1024) {
            throw new IllegalArgumentException("O arquivo passa de " + TAMANHO_MAX_MB + " MB.");
        }
        Path destino = Path.of("uploads", "materiais", turmaId + "_" + UUID.randomUUID().toString().substring(0, 8) + "_"
                + nome.replaceAll("[^\\w.\\-]", "_"));
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(arquivo.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível guardar o arquivo: " + e.getMessage(), e);
        }
        atualizar("INSERT INTO MATERIAL (turma_id, titulo, arquivo_nome, arquivo_caminho, tamanho_bytes) VALUES (?, ?, ?, ?, ?)",
                turmaId, titulo.trim(), nome, destino.toString(), arquivo.length());
    }

    public void excluirMaterial(int materialId, int professorId) {
        List<String> caminhos = consultar("SELECT m.arquivo_caminho FROM MATERIAL m JOIN TURMA t ON t.turma_id = m.turma_id "
                + "WHERE m.material_id = ? AND t.professor_id = ?", rs -> rs.getString(1), materialId, professorId);
        if (caminhos.isEmpty()) {
            throw new IllegalStateException("Material não encontrado nas suas turmas.");
        }
        atualizar("DELETE FROM MATERIAL WHERE material_id = ?", materialId);
        try {
            Files.deleteIfExists(Path.of(caminhos.get(0)));
        } catch (IOException ignorada) {
            // o registro já foi removido; um arquivo órfão não afeta o uso
        }
    }
}
