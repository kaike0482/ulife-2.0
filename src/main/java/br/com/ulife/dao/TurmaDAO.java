package br.com.ulife.dao;

import br.com.ulife.model.AlunoTurma;
import br.com.ulife.model.Turma;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Turmas e matrículas (gestão feita pelo coordenador; consulta pelo professor). */
public class TurmaDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /** Lista turmas ativas; se professorId != null, só as desse professor. */
    public List<Turma> listar(String filtro, Integer professorId) {
        String sql = "SELECT t.*, u.nome AS prof_nome, "
                + "(SELECT COUNT(*) FROM MATRICULA m WHERE m.turma_id = t.turma_id AND m.status = 'ATIVA') AS qtd "
                + "FROM TURMA t JOIN USUARIO u ON u.usuario_id = t.professor_id "
                + "WHERE t.ativo = TRUE "
                + (professorId != null ? "AND t.professor_id = ? " : "")
                + "AND CONCAT_WS(' ', t.codigo, t.nome, u.nome, t.semestre) LIKE ? ORDER BY t.codigo";
        List<Turma> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            if (professorId != null) {
                ps.setInt(i++, professorId);
            }
            ps.setString(i, "%" + (filtro == null ? "" : filtro.trim()) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Turma t = new Turma();
                    t.setTurmaId(rs.getInt("turma_id"));
                    t.setCodigo(rs.getString("codigo"));
                    t.setNome(rs.getString("nome"));
                    t.setDescricao(rs.getString("descricao"));
                    t.setProfessorId(rs.getInt("professor_id"));
                    t.setProfessorNome(rs.getString("prof_nome"));
                    t.setSemestre(rs.getString("semestre"));
                    t.setAno(rs.getInt("ano"));
                    t.setCargaHoraria(rs.getInt("carga_horaria_total"));
                    t.setQtdAlunos(rs.getInt("qtd"));
                    lista.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar turmas: " + e.getMessage(), e);
        }
        return lista;
    }

    public Optional<Turma> findByCodigo(String codigo) {
        for (Turma t : listar(codigo, null)) {
            if (t.getCodigo().equalsIgnoreCase(codigo)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public void inserir(Turma t) {
        String sql = "INSERT INTO TURMA (codigo, nome, descricao, professor_id, semestre, ano, carga_horaria_total, ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getCodigo());
            ps.setString(2, t.getNome());
            ps.setString(3, t.getDescricao());
            ps.setInt(4, t.getProfessorId());
            ps.setString(5, t.getSemestre());
            ps.setInt(6, t.getAno());
            ps.setInt(7, t.getCargaHoraria());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar turma: " + e.getMessage(), e);
        }
    }

    public void atualizar(Turma t) {
        String sql = "UPDATE TURMA SET nome = ?, descricao = ?, professor_id = ?, semestre = ?, ano = ?, "
                + "carga_horaria_total = ? WHERE turma_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getNome());
            ps.setString(2, t.getDescricao());
            ps.setInt(3, t.getProfessorId());
            ps.setString(4, t.getSemestre());
            ps.setInt(5, t.getAno());
            ps.setInt(6, t.getCargaHoraria());
            ps.setInt(7, t.getTurmaId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao alterar turma: " + e.getMessage(), e);
        }
    }

    /** Exclui a turma e, em cascata, matrículas, notas e frequências dela. */
    public void excluir(int turmaId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM TURMA WHERE turma_id = ?")) {
            ps.setInt(1, turmaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir turma: " + e.getMessage(), e);
        }
    }

    public List<AlunoTurma> alunosDaTurma(int turmaId) {
        String sql = "SELECT m.matricula_id, u.usuario_id, u.nome, u.email, a.ra FROM MATRICULA m "
                + "JOIN ALUNO a ON a.aluno_id = m.aluno_id JOIN USUARIO u ON u.usuario_id = a.aluno_id "
                + "WHERE m.turma_id = ? AND m.status = 'ATIVA' ORDER BY u.nome";
        return consultarAlunos(sql, turmaId, true);
    }

    /** Alunos cadastrados que ainda não estão matriculados na turma. */
    public List<AlunoTurma> alunosDisponiveis(int turmaId) {
        String sql = "SELECT NULL AS matricula_id, u.usuario_id, u.nome, u.email, a.ra FROM USUARIO u "
                + "JOIN ALUNO a ON a.aluno_id = u.usuario_id WHERE u.ativo = TRUE AND u.perfil = 'ALUNO' AND NOT EXISTS "
                + "(SELECT 1 FROM MATRICULA m WHERE m.aluno_id = a.aluno_id AND m.turma_id = ? AND m.status = 'ATIVA') "
                + "ORDER BY u.nome";
        return consultarAlunos(sql, turmaId, false);
    }

    private List<AlunoTurma> consultarAlunos(String sql, int turmaId, boolean comMatricula) {
        List<AlunoTurma> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turmaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AlunoTurma a = new AlunoTurma();
                    if (comMatricula) {
                        a.setMatriculaId(rs.getInt("matricula_id"));
                    }
                    a.setUsuarioId(rs.getInt("usuario_id"));
                    a.setNome(rs.getString("nome"));
                    a.setEmail(rs.getString("email"));
                    a.setRa(rs.getString("ra"));
                    lista.add(a);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar alunos: " + e.getMessage(), e);
        }
        return lista;
    }

    public void matricular(int turmaId, int alunoUsuarioId) {
        String sql = "INSERT INTO MATRICULA (aluno_id, turma_id, status) VALUES (?, ?, 'ATIVA') "
                + "ON DUPLICATE KEY UPDATE status = 'ATIVA'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alunoUsuarioId);
            ps.setInt(2, turmaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao matricular aluno: " + e.getMessage(), e);
        }
    }

    /** Remove a matrícula (apaga também as notas e frequências do aluno nessa turma). */
    public void desmatricular(int matriculaId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM MATRICULA WHERE matricula_id = ?")) {
            ps.setInt(1, matriculaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover matrícula: " + e.getMessage(), e);
        }
    }
}
