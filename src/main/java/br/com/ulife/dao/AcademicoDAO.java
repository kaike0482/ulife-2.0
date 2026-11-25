package br.com.ulife.dao;

import br.com.ulife.model.LinhaFrequencia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Notas (A1–A3), frequência (com sugestão pela videochamada) e indicadores acadêmicos. */
public class AcademicoDAO {
    public static final String[] AVALIACOES = {"A1", "A2", "A3"};
    public static final BigDecimal MEDIA_APROVACAO = new BigDecimal("6.0");

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    // ------------------------------------------------------------------ tipos auxiliares

    public static class Sessao {
        public final LocalDate data;
        public final boolean lancada;
        public Sessao(LocalDate data, boolean lancada) {
            this.data = data;
            this.lancada = lancada;
        }
    }

    public static class Pendencia {
        public final int turmaId;
        public final String turmaNome;
        public final LocalDate data;
        public Pendencia(int turmaId, String turmaNome, LocalDate data) {
            this.turmaId = turmaId;
            this.turmaNome = turmaNome;
            this.data = data;
        }
    }

    public static class Indicadores {
        public int alunos, professores, turmas, matriculas;
        public int alunosComVencidas;
        public BigDecimal valorVencido = BigDecimal.ZERO;
        public BigDecimal valorEmAberto = BigDecimal.ZERO;
        public Double frequenciaMedia;   // % ou null
        public BigDecimal mediaGeral;    // ou null
    }

    // ------------------------------------------------------------------ notas

    /** matriculaId -> (A1|A2|A3 -> valor). */
    public Map<Integer, Map<String, BigDecimal>> notasDaTurma(int turmaId) {
        String sql = "SELECT n.matricula_id, n.tipo_avaliacao, n.valor FROM NOTA n "
                + "JOIN MATRICULA m ON m.matricula_id = n.matricula_id WHERE m.turma_id = ?";
        Map<Integer, Map<String, BigDecimal>> mapa = new HashMap<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turmaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.computeIfAbsent(rs.getInt(1), k -> new HashMap<>()).put(rs.getString(2), rs.getBigDecimal(3));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar notas: " + e.getMessage(), e);
        }
        return mapa;
    }

    /** Grava a nota; valor null remove o lançamento. */
    public void salvarNota(int matriculaId, String tipo, BigDecimal valor) {
        try (Connection conn = db.getConnection()) {
            if (valor == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM NOTA WHERE matricula_id = ? AND tipo_avaliacao = ?")) {
                    ps.setInt(1, matriculaId);
                    ps.setString(2, tipo);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO NOTA (matricula_id, tipo_avaliacao, valor) "
                        + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE valor = VALUES(valor), data_lancamento = NOW()")) {
                    ps.setInt(1, matriculaId);
                    ps.setString(2, tipo);
                    ps.setBigDecimal(3, valor);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar nota: " + e.getMessage(), e);
        }
    }

    /** Média simples de A1, A2 e A3; null enquanto faltar alguma nota. */
    public static BigDecimal media(Map<String, BigDecimal> notas) {
        if (notas == null) {
            return null;
        }
        BigDecimal soma = BigDecimal.ZERO;
        for (String a : AVALIACOES) {
            BigDecimal v = notas.get(a);
            if (v == null) {
                return null;
            }
            soma = soma.add(v);
        }
        return soma.divide(new BigDecimal(AVALIACOES.length), 1, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ frequência

    public List<Sessao> sessoes(int turmaId) {
        String sql = "SELECT f.aula_data, MIN(f.lancada) AS lancada FROM FREQUENCIA f "
                + "JOIN MATRICULA m ON m.matricula_id = f.matricula_id WHERE m.turma_id = ? "
                + "GROUP BY f.aula_data ORDER BY f.aula_data DESC";
        List<Sessao> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turmaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Sessao(rs.getDate(1).toLocalDate(), rs.getInt(2) == 1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar aulas: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<LinhaFrequencia> frequenciaDaSessao(int turmaId, LocalDate data) {
        String sql = "SELECT f.matricula_id, u.nome, a.ra, f.minutos_conectado, f.presenca, f.justificativa, f.lancada "
                + "FROM FREQUENCIA f JOIN MATRICULA m ON m.matricula_id = f.matricula_id "
                + "JOIN ALUNO a ON a.aluno_id = m.aluno_id JOIN USUARIO u ON u.usuario_id = a.aluno_id "
                + "WHERE m.turma_id = ? AND f.aula_data = ? ORDER BY u.nome";
        List<LinhaFrequencia> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turmaId);
            ps.setDate(2, Date.valueOf(data));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LinhaFrequencia l = new LinhaFrequencia();
                    l.setMatriculaId(rs.getInt(1));
                    l.setNome(rs.getString(2));
                    l.setRa(rs.getString(3));
                    int min = rs.getInt(4);
                    l.setMinutos(rs.wasNull() ? null : min);
                    l.setPresenca(rs.getBoolean(5));
                    l.setJustificativa(rs.getString(6));
                    l.setLancada(rs.getBoolean(7));
                    lista.add(l);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar frequência: " + e.getMessage(), e);
        }
        return lista;
    }

    public void lancarFrequencia(List<LinhaFrequencia> linhas, LocalDate data) {
        String sql = "UPDATE FREQUENCIA SET presenca = ?, justificativa = ?, lancada = TRUE "
                + "WHERE matricula_id = ? AND aula_data = ?";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (LinhaFrequencia l : linhas) {
                    ps.setBoolean(1, l.isPresenca());
                    ps.setString(2, l.getJustificativa());
                    ps.setInt(3, l.getMatriculaId());
                    ps.setDate(4, Date.valueOf(data));
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao lançar frequência: " + e.getMessage(), e);
        }
    }

    /** Aulas com frequência ainda não lançada pelo professor. */
    public List<Pendencia> pendenciasDeFrequencia(int professorId) {
        String sql = "SELECT t.turma_id, t.nome, f.aula_data FROM FREQUENCIA f "
                + "JOIN MATRICULA m ON m.matricula_id = f.matricula_id JOIN TURMA t ON t.turma_id = m.turma_id "
                + "WHERE t.professor_id = ? GROUP BY t.turma_id, t.nome, f.aula_data HAVING MIN(f.lancada) = 0 "
                + "ORDER BY f.aula_data DESC";
        List<Pendencia> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, professorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Pendencia(rs.getInt(1), rs.getString(2), rs.getDate(3).toLocalDate()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar pendências: " + e.getMessage(), e);
        }
        return lista;
    }

    /** matriculaId -> {presenças, aulas lançadas}. */
    public Map<Integer, int[]> resumoFrequencia(int turmaId) {
        String sql = "SELECT f.matricula_id, SUM(f.presenca), COUNT(*) FROM FREQUENCIA f "
                + "JOIN MATRICULA m ON m.matricula_id = f.matricula_id "
                + "WHERE m.turma_id = ? AND f.lancada = TRUE GROUP BY f.matricula_id";
        Map<Integer, int[]> mapa = new HashMap<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turmaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.put(rs.getInt(1), new int[]{rs.getInt(2), rs.getInt(3)});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar frequência: " + e.getMessage(), e);
        }
        return mapa;
    }

    // ------------------------------------------------------------------ boletim do aluno

    /** Notas e frequência de um aluno em uma turma. */
    public static class Boletim {
        public String codigo, turma, professor;
        public final Map<String, BigDecimal> notas = new HashMap<>();
        public int presencas, aulas;

        public BigDecimal media() { return AcademicoDAO.media(notas); }
        public int faltas() { return aulas - presencas; }
        public Double frequencia() { return aulas == 0 ? null : presencas * 100.0 / aulas; }
    }

    /** Boletim do aluno: uma linha por turma em que está matriculado, com os lançamentos do professor. */
    public List<Boletim> boletim(int usuarioId) {
        Map<Integer, Boletim> porMatricula = new java.util.LinkedHashMap<>();
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT m.matricula_id, t.codigo, t.nome, u.nome "
                    + "FROM MATRICULA m JOIN TURMA t ON t.turma_id = m.turma_id "
                    + "JOIN USUARIO u ON u.usuario_id = t.professor_id "
                    + "WHERE m.aluno_id = ? AND m.status = 'ATIVA' AND t.ativo = TRUE ORDER BY t.nome")) {
                ps.setInt(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Boletim b = new Boletim();
                        b.codigo = rs.getString(2);
                        b.turma = rs.getString(3);
                        b.professor = rs.getString(4);
                        porMatricula.put(rs.getInt(1), b);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT n.matricula_id, n.tipo_avaliacao, n.valor FROM NOTA n "
                    + "JOIN MATRICULA m ON m.matricula_id = n.matricula_id WHERE m.aluno_id = ?")) {
                ps.setInt(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Boletim b = porMatricula.get(rs.getInt(1));
                        if (b != null) {
                            b.notas.put(rs.getString(2), rs.getBigDecimal(3));
                        }
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT f.matricula_id, SUM(f.presenca), COUNT(*) "
                    + "FROM FREQUENCIA f JOIN MATRICULA m ON m.matricula_id = f.matricula_id "
                    + "WHERE m.aluno_id = ? AND f.lancada = TRUE GROUP BY f.matricula_id")) {
                ps.setInt(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Boletim b = porMatricula.get(rs.getInt(1));
                        if (b != null) {
                            b.presencas = rs.getInt(2);
                            b.aulas = rs.getInt(3);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar o boletim: " + e.getMessage(), e);
        }
        return new ArrayList<>(porMatricula.values());
    }

    // ------------------------------------------------------------------ indicadores

    public Indicadores indicadores() {
        Indicadores r = new Indicadores();
        try (Connection conn = db.getConnection(); Statement st = conn.createStatement()) {
            r.alunos = contar(st, "SELECT COUNT(*) FROM USUARIO WHERE perfil = 'ALUNO' AND ativo = TRUE");
            r.professores = contar(st, "SELECT COUNT(*) FROM USUARIO WHERE perfil = 'PROFESSOR' AND ativo = TRUE");
            r.turmas = contar(st, "SELECT COUNT(*) FROM TURMA WHERE ativo = TRUE");
            r.matriculas = contar(st, "SELECT COUNT(*) FROM MATRICULA WHERE status = 'ATIVA'");
            st.executeUpdate("UPDATE COBRANCA SET status = 'VENCIDA' WHERE status = 'ABERTA' AND vencimento < CURDATE()");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT usuario_id), COALESCE(SUM(valor), 0) "
                    + "FROM COBRANCA WHERE status = 'VENCIDA' AND usuario_id IS NOT NULL")) {
                rs.next();
                r.alunosComVencidas = rs.getInt(1);
                r.valorVencido = rs.getBigDecimal(2);
            }
            try (ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(valor), 0) FROM COBRANCA WHERE status = 'ABERTA'")) {
                rs.next();
                r.valorEmAberto = rs.getBigDecimal(1);
            }
            try (ResultSet rs = st.executeQuery("SELECT AVG(presenca) * 100 FROM FREQUENCIA WHERE lancada = TRUE")) {
                rs.next();
                double v = rs.getDouble(1);
                r.frequenciaMedia = rs.wasNull() ? null : v;
            }
            try (ResultSet rs = st.executeQuery("SELECT AVG(media) FROM (SELECT AVG(valor) AS media FROM NOTA "
                    + "GROUP BY matricula_id HAVING COUNT(*) = 3) x")) {
                rs.next();
                BigDecimal v = rs.getBigDecimal(1);
                r.mediaGeral = v == null ? null : v.setScale(1, RoundingMode.HALF_UP);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao calcular indicadores: " + e.getMessage(), e);
        }
        return r;
    }

    private static int contar(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
