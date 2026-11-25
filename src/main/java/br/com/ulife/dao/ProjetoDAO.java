package br.com.ulife.dao;

import br.com.ulife.model.Projeto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjetoDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /** Consulta com filtro opcional (código, descrição, ODS, responsável, telefone ou status). */
    public List<Projeto> listar(String filtro) {
        String sql = "SELECT * FROM PROJETO WHERE CONCAT_WS(' ', codigo, descricao, CONCAT('ODS ', ods), "
                + "responsavel, telefone, status) LIKE ? ORDER BY codigo";
        List<Projeto> lista = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + (filtro == null ? "" : filtro.trim()) + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar projetos: " + e.getMessage(), e);
        }
        return lista;
    }

    public Optional<Projeto> findByCodigo(String codigo) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM PROJETO WHERE codigo = ?")) {
            stmt.setString(1, codigo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar projeto: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    /** Próximo código sugerido: PRJ-001, PRJ-002, ... */
    public String proximoCodigo() {
        int max = 0;
        for (Projeto p : listar("")) {
            String c = p.getCodigo();
            int i = c.length();
            while (i > 0 && Character.isDigit(c.charAt(i - 1))) {
                i--;
            }
            if (i < c.length()) {
                try {
                    max = Math.max(max, Integer.parseInt(c.substring(i)));
                } catch (NumberFormatException ignored) {
                    // código numérico grande demais: ignora
                }
            }
        }
        return String.format("PRJ-%03d", max + 1);
    }

    public void inserir(Projeto p) {
        String sql = "INSERT INTO PROJETO (codigo, descricao, ods, responsavel, telefone, data_criacao, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getCodigo());
            stmt.setString(2, p.getDescricao());
            stmt.setInt(3, p.getOds());
            stmt.setString(4, p.getResponsavel());
            stmt.setString(5, p.getTelefone());
            stmt.setDate(6, Date.valueOf(p.getDataCriacao()));
            stmt.setString(7, p.getStatus().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar projeto: " + e.getMessage(), e);
        }
    }

    public void atualizar(Projeto p) {
        String sql = "UPDATE PROJETO SET descricao = ?, ods = ?, responsavel = ?, telefone = ?, "
                + "data_criacao = ?, status = ? WHERE codigo = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getDescricao());
            stmt.setInt(2, p.getOds());
            stmt.setString(3, p.getResponsavel());
            stmt.setString(4, p.getTelefone());
            stmt.setDate(5, Date.valueOf(p.getDataCriacao()));
            stmt.setString(6, p.getStatus().name());
            stmt.setString(7, p.getCodigo());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao alterar projeto: " + e.getMessage(), e);
        }
    }

    public void excluir(String codigo) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM PROJETO WHERE codigo = ?")) {
            stmt.setString(1, codigo);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir projeto: " + e.getMessage(), e);
        }
    }

    private Projeto map(ResultSet rs) throws SQLException {
        Projeto p = new Projeto();
        p.setProjetoId(rs.getInt("projeto_id"));
        p.setCodigo(rs.getString("codigo"));
        p.setDescricao(rs.getString("descricao"));
        p.setOds(rs.getInt("ods"));
        p.setResponsavel(rs.getString("responsavel"));
        p.setTelefone(rs.getString("telefone"));
        p.setDataCriacao(rs.getDate("data_criacao").toLocalDate());
        p.setStatus(Projeto.Status.valueOf(rs.getString("status")));
        return p;
    }
}
