package br.com.ulife.dao;

import br.com.ulife.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAO {
    private final DatabaseConnection dbConnection = DatabaseConnection.getInstance();

    /** Busca por e-mail (usuário ativo). */
    public Optional<Usuario> findByEmail(String email) {
        return buscarUm("SELECT * FROM USUARIO WHERE email = ? AND ativo = TRUE", email);
    }

    /** Busca pelo nome de usuário (login). */
    public Optional<Usuario> findByUsuario(String usuario) {
        return buscarUm("SELECT * FROM USUARIO WHERE usuario = ?", usuario);
    }

    /** Busca por nome de usuário OU e-mail, apenas usuários ativos. */
    public Optional<Usuario> findByLogin(String login) {
        return buscarUm("SELECT * FROM USUARIO WHERE (usuario = ? OR email = ?) AND ativo = TRUE", login, login);
    }

    public Optional<Usuario> findById(Integer id) {
        return buscarUm("SELECT * FROM USUARIO WHERE usuario_id = ?", id);
    }

    /** Controle de acesso: retorna o usuário se o login (usuário ou e-mail) e a senha conferem. */
    public Optional<Usuario> autenticar(String login, String senha) {
        Optional<Usuario> u = findByLogin(login);
        if (u.isPresent() && BCrypt.checkpw(senha, u.get().getSenhaHash())) {
            return u;
        }
        return Optional.empty();
    }

    public boolean verifyPassword(String email, String senha) {
        Optional<Usuario> usuarioOpt = findByEmail(email);
        return usuarioOpt.isPresent() && BCrypt.checkpw(senha, usuarioOpt.get().getSenhaHash());
    }

    /** Consulta com filtro opcional (usuário, nome, e-mail ou telefone). */
    public List<Usuario> listar(String filtro) {
        String sql = "SELECT * FROM USUARIO WHERE ativo = TRUE AND "
                + "CONCAT_WS(' ', usuario, nome, email, telefone) LIKE ? ORDER BY nome";
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + (filtro == null ? "" : filtro.trim()) + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar usuários: " + e.getMessage(), e);
        }
        return lista;
    }

    public boolean usuarioExiste(String usuario) {
        return findByUsuario(usuario).isPresent();
    }

    public boolean emailExiste(String email, Integer ignorarId) {
        String sql = "SELECT 1 FROM USUARIO WHERE email = ? AND usuario_id <> ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, ignorarId == null ? -1 : ignorarId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar e-mail: " + e.getMessage(), e);
        }
    }

    /** Cadastro. A senha é gravada somente como hash BCrypt. */
    public Usuario inserir(String usuario, String nome, String email, String telefone, String senha,
                           Usuario.Perfil perfil) {
        String sql = "INSERT INTO USUARIO (usuario, nome, email, telefone, senha_hash, perfil, ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario);
            stmt.setString(2, nome);
            stmt.setString(3, email);
            stmt.setString(4, telefone);
            stmt.setString(5, BCrypt.hashpw(senha, BCrypt.gensalt()));
            stmt.setString(6, perfil.name());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    SchemaInitializer.sincronizarPapeis();
                    return findById(keys.getInt(1)).orElse(null);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar usuário: " + e.getMessage(), e);
        }
        return null;
    }

    /** Alteração. Se novaSenha for vazia, a senha atual é mantida. */
    public void atualizar(Integer id, String nome, String email, String telefone, String novaSenha,
                          Usuario.Perfil perfil) {
        boolean trocaSenha = novaSenha != null && !novaSenha.isEmpty();
        String sql = "UPDATE USUARIO SET nome = ?, email = ?, telefone = ?, perfil = ?"
                + (trocaSenha ? ", senha_hash = ?" : "") + " WHERE usuario_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 1;
            stmt.setString(i++, nome);
            stmt.setString(i++, email);
            stmt.setString(i++, telefone);
            stmt.setString(i++, perfil.name());
            if (trocaSenha) {
                stmt.setString(i++, BCrypt.hashpw(novaSenha, BCrypt.gensalt()));
            }
            stmt.setInt(i, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao alterar usuário: " + e.getMessage(), e);
        }
        SchemaInitializer.sincronizarPapeis();
    }

    /** Usuários ativos de um perfil (ex.: professores para escolher na turma). */
    public List<Usuario> listarPorPerfil(Usuario.Perfil perfil) {
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM USUARIO WHERE perfil = ? AND ativo = TRUE ORDER BY nome")) {
            stmt.setString(1, perfil.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar usuários: " + e.getMessage(), e);
        }
        return lista;
    }

    /** Exclusão. */
    public void excluir(Integer id) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM USUARIO WHERE usuario_id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir usuário: " + e.getMessage()
                    + " (o usuário pode estar vinculado a outros registros).", e);
        }
    }

    private Optional<Usuario> buscarUm(String sql, Object... params) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUsuario(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Usuario mapResultSetToUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(rs.getInt("usuario_id"));
        usuario.setUsuario(rs.getString("usuario"));
        usuario.setNome(rs.getString("nome"));
        usuario.setEmail(rs.getString("email"));
        usuario.setTelefone(rs.getString("telefone"));
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
