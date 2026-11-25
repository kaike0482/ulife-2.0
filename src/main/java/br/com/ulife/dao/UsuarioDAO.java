package br.com.ulife.dao;

import br.com.ulife.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class UsuarioDAO {
    private final DatabaseConnection dbConnection = DatabaseConnection.getInstance();

    public Optional<Usuario> findByEmail(String email) {
        String sql = "SELECT * FROM USUARIO WHERE email = ? AND ativo = TRUE";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por email", e);
        }
        return Optional.empty();
    }

    public Optional<Usuario> findById(Integer id) {
        String sql = "SELECT * FROM USUARIO WHERE usuario_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por ID", e);
        }
        return Optional.empty();
    }

    public boolean verifyPassword(String email, String senha) {
        Optional<Usuario> usuarioOpt = findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            return false;
        }
        Usuario usuario = usuarioOpt.get();
        return BCrypt.checkpw(senha, usuario.getSenhaHash());
    }

    public Usuario createUsuario(String nome, String email, String senha, Usuario.Perfil perfil) {
        String sql = "INSERT INTO USUARIO (nome, email, senha_hash, perfil, ativo) VALUES (?, ?, ?, ?, TRUE)";
        String hash = BCrypt.hashpw(senha, BCrypt.gensalt());
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, hash);
            stmt.setString(4, perfil.name());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    Integer id = keys.getInt(1);
                    return new Usuario(id, nome, email, hash, perfil, true, LocalDateTime.now(), LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar usuário", e);
        }
        return null;
    }

    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(rs.getInt("usuario_id"));
        usuario.setNome(rs.getString("nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setSenhaHash(rs.getString("senha_hash"));
        usuario.setPerfil(Usuario.Perfil.valueOf(rs.getString("perfil")));
        usuario.setAtivo(rs.getBoolean("ativo"));
        Timestamp criado = rs.getTimestamp("criado_em");
        Timestamp atualizado = rs.getTimestamp("atualizado_em");
        if (criado != null) usuario.setCriadoEm(criado.toLocalDateTime());
        if (atualizado != null) usuario.setAtualizadoEm(atualizado.toLocalDateTime());
        return usuario;
    }
}