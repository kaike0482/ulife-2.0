package br.com.ulife.dao;

import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Locale;
import java.time.format.DateTimeFormatter;

/**
 * Ajusta automaticamente o banco existente (por exemplo, o MySQL do Docker já criado com o bd.sql antigo)
 * para os requisitos do Projeto A3: adiciona usuario/telefone em USUARIO, cria a tabela PROJETO, permite
 * cobranças por usuário e cria as contas de demonstração (admin, coordenador, professor e aluno).
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

            // Cobranças diretamente por usuário (MVP: sem exigir turma/matrícula)
            if (!colunaExiste(conn, "COBRANCA", "usuario_id")) {
                st.execute("ALTER TABLE COBRANCA MODIFY matricula_id INT NULL");
                st.execute("ALTER TABLE COBRANCA ADD COLUMN usuario_id INT NULL AFTER matricula_id");
                st.execute("ALTER TABLE COBRANCA ADD CONSTRAINT fk_cobranca_usuario "
                        + "FOREIGN KEY (usuario_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE");
            }

            // Notas A1-A3 sem exigir tarefa, e frequência com minutos da videochamada
            if (!colunaExiste(conn, "FREQUENCIA", "minutos_conectado")) {
                st.execute("ALTER TABLE FREQUENCIA ADD COLUMN minutos_conectado INT NULL");
            }
            if (!colunaExiste(conn, "FREQUENCIA", "lancada")) {
                st.execute("ALTER TABLE FREQUENCIA ADD COLUMN lancada BOOLEAN NOT NULL DEFAULT FALSE");
            }
            if (!indiceExiste(conn, "NOTA", "uk_nota_aval")) {
                st.execute("ALTER TABLE NOTA MODIFY tarefa_id INT NULL");
                st.execute("ALTER TABLE NOTA ADD UNIQUE KEY uk_nota_aval (matricula_id, tipo_avaliacao)");
            }

            garantirUsuario(conn, "admin", "Administrador", "admin@ulife.com", "(11) 99999-0000",
                    "admin123", "COORDENADOR");
            garantirUsuario(conn, "coordenador", "Carla Coordenadora", "coordenador@ulife.com", "(11) 98888-0001",
                    "coord123", "COORDENADOR");
            garantirUsuario(conn, "professor", "Paulo Professor", "professor@ulife.com", "(11) 98888-0002",
                    "prof123", "PROFESSOR");
            garantirUsuario(conn, "aluno", "Aline Aluna", "aluno@ulife.com", "(11) 98888-0003",
                    "aluno123", "ALUNO");
            garantirUsuario(conn, "bruno", "Bruno Souza", "bruno@ulife.com", "(11) 98888-0004",
                    "aluno123", "ALUNO");
            garantirUsuario(conn, "camila", "Camila Ribeiro", "camila@ulife.com", "(11) 98888-0005",
                    "aluno123", "ALUNO");
            garantirUsuario(conn, "diego", "Diego Martins", "diego@ulife.com", "(11) 98888-0006",
                    "aluno123", "ALUNO");
            garantirUsuario(conn, "elisa", "Elisa Nogueira", "elisa@ulife.com", "(11) 98888-0007",
                    "aluno123", "ALUNO");
            sincronizarPapeis(conn);
            semearCobrancasDoAluno(conn);
            semearAcademico(conn);
            ConteudoSchema.garantir(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao preparar o banco de dados: " + e.getMessage(), e);
        }
    }

    /** Garante as linhas ALUNO/PROFESSOR (RA e matrícula funcional) de cada usuário conforme o perfil. */
    public static void sincronizarPapeis() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            sincronizarPapeis(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao sincronizar perfis: " + e.getMessage(), e);
        }
    }

    private static void sincronizarPapeis(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO ALUNO (aluno_id, ra) SELECT usuario_id, CONCAT('RA', LPAD(usuario_id, 6, '0')) "
                    + "FROM USUARIO WHERE perfil = 'ALUNO' AND usuario_id NOT IN (SELECT aluno_id FROM ALUNO)");
            st.executeUpdate("INSERT INTO PROFESSOR (professor_id, matricula_funcional, departamento) "
                    + "SELECT usuario_id, CONCAT('PF', LPAD(usuario_id, 6, '0')), 'Computação' "
                    + "FROM USUARIO WHERE perfil = 'PROFESSOR' AND usuario_id NOT IN (SELECT professor_id FROM PROFESSOR)");
        }
    }

    private static int idDe(Connection conn, String usuario) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT usuario_id FROM USUARIO WHERE usuario = ?")) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Turmas, matrículas, frequência (com minutos da videochamada) e notas de demonstração.
     * A aula mais recente de cada turma fica sem lançamento, para o professor lançar.
     */
    private static void semearAcademico(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM TURMA")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }
        int prof = idDe(conn, "professor");
        String[] logins = {"aluno", "bruno", "camila", "diego", "elisa"};
        int[] alunos = new int[logins.length];
        for (int i = 0; i < logins.length; i++) {
            alunos[i] = idDe(conn, logins[i]);
            if (alunos[i] < 0) {
                return;
            }
        }
        if (prof < 0) {
            return;
        }
        String[][] turmas = {
                {"MS-2026-2", "Modelagem de Software", "0"},
                {"BD-2026-2", "Banco de Dados", "-2"},
                {"ES-2026-2", "Modelos, métodos e técnicas da Engenharia de Software", "-1"}};
        int[][] matriculados = {{0, 1, 2, 3, 4}, {0, 1, 2, 3}, {0, 2, 4}};
        LocalDate hoje = LocalDate.now();

        for (int t = 0; t < turmas.length; t++) {
            int turmaId;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO TURMA (codigo, nome, descricao, professor_id, "
                    + "semestre, ano, carga_horaria_total) VALUES (?, ?, ?, ?, '2026/2', 2026, 80)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, turmas[t][0]);
                ps.setString(2, turmas[t][1]);
                ps.setString(3, "Turma de demonstração do Ulife 2.0");
                ps.setInt(4, prof);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    turmaId = k.getInt(1);
                }
            }
            for (int idx : matriculados[t]) {
                int matId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO MATRICULA (aluno_id, turma_id, status) VALUES (?, ?, 'ATIVA')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, alunos[idx]);
                    ps.setInt(2, turmaId);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        matId = k.getInt(1);
                    }
                }
                for (int s = 0; s < 4; s++) {
                    LocalDate d = hoje.plusDays(Integer.parseInt(turmas[t][2]) - 7L * s);
                    int minutos = Math.min(170, 118 + ((idx * 37 + s * 53 + t * 29) % 55));
                    boolean lancada = s > 0;
                    boolean presenca = lancada && minutos >= 128;
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO FREQUENCIA (matricula_id, aula_data, "
                            + "presenca, minutos_conectado, lancada) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setInt(1, matId);
                        ps.setDate(2, Date.valueOf(d));
                        ps.setBoolean(3, presenca);
                        ps.setInt(4, minutos);
                        ps.setBoolean(5, lancada);
                        ps.executeUpdate();
                    }
                }
                for (int a = 0; a < 3; a++) {
                    boolean tem = a == 0 || (a == 1 && idx < 4) || (a == 2 && t == 1);
                    if (!tem) {
                        continue;
                    }
                    double v = 5.0 + ((idx * 13 + t * 7 + a * 11) % 50) / 10.0;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO NOTA (matricula_id, tipo_avaliacao, valor) VALUES (?, ?, ?)")) {
                        ps.setInt(1, matId);
                        ps.setString(2, "A" + (a + 1));
                        ps.setBigDecimal(3, new BigDecimal(String.format(Locale.US, "%.1f", v)));
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    private static final String SENHA_ANTIGA = "123456";

    private static boolean usaSenha(String hash, String senha) {
        try {
            return hash != null && BCrypt.checkpw(senha, hash);
        } catch (IllegalArgumentException hashInvalido) {
            return false;
        }
    }

    /** Cria a conta de demonstração apenas se o usuário e o e-mail ainda não existirem. */
    private static void garantirUsuario(Connection conn, String usuario, String nome, String email, String telefone,
                                        String senha, String perfil) throws SQLException {
        Integer idExistente = null;
        String hashExistente = null;
        boolean mesmoUsuario = false;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT usuario_id, usuario, senha_hash FROM USUARIO WHERE usuario = ? OR email = ?")) {
            ps.setString(1, usuario);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idExistente = rs.getInt(1);
                    mesmoUsuario = usuario.equals(rs.getString(2));
                    hashExistente = rs.getString(3);
                }
            }
        }
        if (idExistente != null) {
            // Conta de um banco antigo que ainda usa a senha padrão antiga (123456): passa a usar a senha de
            // demonstração documentada. Contas cuja senha já foi trocada nunca são alteradas.
            if (mesmoUsuario && !SENHA_ANTIGA.equals(senha) && usaSenha(hashExistente, SENHA_ANTIGA)) {
                try (PreparedStatement up = conn.prepareStatement("UPDATE USUARIO SET senha_hash = ? WHERE usuario_id = ?")) {
                    up.setString(1, BCrypt.hashpw(senha, BCrypt.gensalt()));
                    up.setInt(2, idExistente);
                    up.executeUpdate();
                }
            }
            return;
        }
        String sql = "INSERT INTO USUARIO (usuario, nome, email, telefone, senha_hash, perfil, ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, nome);
            ps.setString(3, email);
            ps.setString(4, telefone);
            ps.setString(5, BCrypt.hashpw(senha, BCrypt.gensalt()));
            ps.setString(6, perfil);
            ps.executeUpdate();
        }
    }

    /** Mensalidades de demonstração (uma paga, uma vencida e duas em aberto) para a conta "aluno". */
    private static void semearCobrancasDoAluno(Connection conn) throws SQLException {
        int alunoId;
        try (PreparedStatement ps = conn.prepareStatement("SELECT usuario_id FROM USUARIO WHERE usuario = 'aluno'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return;
            }
            alunoId = rs.getInt(1);
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM COBRANCA WHERE usuario_id = ?")) {
            ps.setInt(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        LocalDate hoje = LocalDate.now();
        int[] deslocamentos = {-60, -25, 5, 35};
        String[] status = {"PAGA", "ABERTA", "ABERTA", "ABERTA"};
        DateTimeFormatter mes = DateTimeFormatter.ofPattern("MM/yyyy");
        String sql = "INSERT INTO COBRANCA (usuario_id, descricao, valor, vencimento, status) VALUES (?, ?, ?, ?, ?)";
        for (int i = 0; i < deslocamentos.length; i++) {
            LocalDate venc = hoje.plusDays(deslocamentos[i]);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, alunoId);
                ps.setString(2, "Mensalidade " + venc.format(mes));
                ps.setBigDecimal(3, new BigDecimal("890.00"));
                ps.setDate(4, Date.valueOf(venc));
                ps.setString(5, status[i]);
                ps.executeUpdate();
                if ("PAGA".equals(status[i])) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            try (PreparedStatement pg = conn.prepareStatement("INSERT INTO PAGAMENTO (cobranca_id, "
                                    + "metodo, valor_pago, data_pagamento, transacao_id, status) "
                                    + "VALUES (?, 'PIX', 890.00, ?, 'TXDEMO0000000000001', 'CONFIRMADO')")) {
                                pg.setInt(1, keys.getInt(1));
                                pg.setTimestamp(2, Timestamp.valueOf(venc.minusDays(2).atTime(10, 30)));
                                pg.executeUpdate();
                            }
                        }
                    }
                }
            }
        }
    }

    static boolean colunaExiste(Connection conn, String tabela, String coluna) throws SQLException {
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
