package br.com.ulife.dao;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * Ajusta automaticamente o banco existente (por exemplo, o MySQL do Docker já criado com o bd.sql antigo)
 * para os requisitos do Projeto A3: adiciona usuario/telefone em USUARIO, cria a tabela PROJETO e,
 * se não houver nenhum usuário "admin", cria um (senha inicial: admin123).
 * É idempotente: pode rodar a cada inicialização.
 */
public final class SchemaInitializer {

    private SchemaInitializer() {}

    public static void garantir() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement st = conn.createStatement()) {

            if (!colunaExiste(conn, "USUARIO", "usuario")) {
                st.execute("ALTER TABLE USUARIO ADD COLUMN usuario VARCHAR(30) NULL AFTER usuario_id");
            }
            if (!colunaExiste(conn, "USUARIO", "telefone")) {
                st.execute("ALTER TABLE USUARIO ADD COLUMN telefone VARCHAR(20) NULL AFTER email");
            }

            // Usuários antigos: o login passa a ser a parte do e-mail antes do @ (admin@ulife.com -> admin)
            st.executeUpdate("UPDATE USUARIO SET usuario = LOWER(SUBSTRING_INDEX(email, '@', 1)) "
                    + "WHERE usuario IS NULL OR usuario = ''");
            st.executeUpdate("UPDATE USUARIO SET usuario = CONCAT(usuario, '_', usuario_id) WHERE usuario IN "
                    + "(SELECT u FROM (SELECT usuario AS u FROM USUARIO GROUP BY usuario HAVING COUNT(*) > 1) t)");
            st.executeUpdate("UPDATE USUARIO SET telefone = '' WHERE telefone IS NULL");

            if (!indiceExiste(conn, "USUARIO", "uk_usuario_login")) {
                st.execute("ALTER TABLE USUARIO MODIFY usuario VARCHAR(30) NOT NULL");
                st.execute("ALTER TABLE USUARIO ADD UNIQUE KEY uk_usuario_login (usuario)");
            }

            st.execute("CREATE TABLE IF NOT EXISTS PROJETO ("
                    + "projeto_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "codigo VARCHAR(20) NOT NULL UNIQUE, "
                    + "descricao VARCHAR(500) NOT NULL, "
                    + "ods TINYINT NOT NULL, "
                    + "responsavel VARCHAR(100) NOT NULL, "
                    + "telefone VARCHAR(20) NOT NULL, "
                    + "data_criacao DATE NOT NULL, "
                    + "status ENUM('PLANEJADO','EM_ANDAMENTO','CONCLUIDO','CANCELADO') NOT NULL DEFAULT 'PLANEJADO', "
                    + "CONSTRAINT ck_projeto_ods CHECK (ods BETWEEN 1 AND 17)"
                    + ") ENGINE=InnoDB");

            criarAdminSeNecessario(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao preparar o banco de dados: " + e.getMessage(), e);
        }
    }

    private static void criarAdminSeNecessario(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM USUARIO")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }
        String sql = "INSERT INTO USUARIO (usuario, nome, email, telefone, senha_hash, perfil, ativo) "
                + "VALUES ('admin', 'Administrador', 'admin@ulife.com', '(11) 99999-0000', ?, 'COORDENADOR', TRUE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw("admin123", BCrypt.gensalt()));
            ps.executeUpdate();
        }
    }

    private static boolean colunaExiste(Connection conn, String tabela, String coluna) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabela);
            ps.setString(2, coluna);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean indiceExiste(Connection conn, String tabela, String indice) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabela);
            ps.setString(2, indice);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
