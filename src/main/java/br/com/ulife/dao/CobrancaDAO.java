package br.com.ulife.dao;

import br.com.ulife.model.Cobranca;
import br.com.ulife.model.Pagamento;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Cobranças e pagamentos do aluno (mensalidades pagas por PIX ou boleto). */
public class CobrancaDAO {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /** Marca como VENCIDA as cobranças em aberto cujo vencimento já passou. */
    public void atualizarVencidas(int usuarioId) {
        String sql = "UPDATE COBRANCA SET status = 'VENCIDA' WHERE usuario_id = ? AND status = 'ABERTA' "
                + "AND vencimento < CURDATE()";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar cobranças: " + e.getMessage(), e);
        }
    }

    public List<Cobranca> listar(int usuarioId) {
        atualizarVencidas(usuarioId);
        String sql = "SELECT * FROM COBRANCA WHERE usuario_id = ? ORDER BY vencimento";
        List<Cobranca> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cobranca c = new Cobranca();
                    c.setCobrancaId(rs.getInt("cobranca_id"));
                    c.setUsuarioId(rs.getInt("usuario_id"));
                    c.setDescricao(rs.getString("descricao"));
                    c.setValor(rs.getBigDecimal("valor"));
                    c.setVencimento(rs.getDate("vencimento").toLocalDate());
                    c.setStatus(Cobranca.Status.valueOf(rs.getString("status")));
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar cobranças: " + e.getMessage(), e);
        }
        return lista;
    }

    /** Quantidade de cobranças pendentes (em aberto ou vencidas). */
    public int contarPendentes(int usuarioId) {
        atualizarVencidas(usuarioId);
        String sql = "SELECT COUNT(*) FROM COBRANCA WHERE usuario_id = ? AND status IN ('ABERTA', 'VENCIDA')";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar cobranças: " + e.getMessage(), e);
        }
    }

    /**
     * Registra o pagamento (ambiente de demonstração/sandbox): grava o PAGAMENTO como CONFIRMADO e
     * quita a cobrança, na mesma transação. Falha se a cobrança já estiver paga ou não for do usuário.
     *
     * @return número da transação gerado
     */
    public String pagar(int cobrancaId, int usuarioId, Pagamento.Metodo metodo, String qrPix, String codigoBarras) {
        String transacao = "TX" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement up = conn.prepareStatement("UPDATE COBRANCA SET status = 'PAGA' "
                        + "WHERE cobranca_id = ? AND usuario_id = ? AND status IN ('ABERTA', 'VENCIDA')")) {
                    up.setInt(1, cobrancaId);
                    up.setInt(2, usuarioId);
                    if (up.executeUpdate() != 1) {
                        throw new IllegalStateException("Esta cobrança já foi paga ou não está disponível.");
                    }
                }
                try (PreparedStatement ins = conn.prepareStatement("INSERT INTO PAGAMENTO (cobranca_id, metodo, "
                        + "valor_pago, qr_code_pix, boleto_codigo_barras, transacao_id, status) "
                        + "SELECT cobranca_id, ?, valor, ?, ?, ?, 'CONFIRMADO' FROM COBRANCA WHERE cobranca_id = ?")) {
                    ins.setString(1, metodo.name());
                    ins.setString(2, qrPix);
                    ins.setString(3, codigoBarras);
                    ins.setString(4, transacao);
                    ins.setInt(5, cobrancaId);
                    ins.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar pagamento: " + e.getMessage(), e);
        }
        return transacao;
    }

    // ------------------------------------------------------------------ gestão (coordenador)

    /** Todas as cobranças de alunos, com filtro de texto e status opcional (null = todos). */
    public List<Cobranca> listarTodas(String filtro, Cobranca.Status status) {
        String sql = "SELECT c.*, u.nome AS aluno_nome FROM COBRANCA c JOIN USUARIO u ON u.usuario_id = c.usuario_id "
                + "WHERE " + (status != null ? "c.status = ? AND " : "")
                + "CONCAT_WS(' ', u.nome, u.usuario, c.descricao) LIKE ? ORDER BY c.vencimento DESC, u.nome";
        List<Cobranca> lista = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE COBRANCA SET status = 'VENCIDA' WHERE status = 'ABERTA' AND vencimento < CURDATE()");
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                if (status != null) {
                    ps.setString(i++, status.name());
                }
                ps.setString(i, "%" + (filtro == null ? "" : filtro.trim()) + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Cobranca c = new Cobranca();
                        c.setCobrancaId(rs.getInt("cobranca_id"));
                        c.setUsuarioId(rs.getInt("usuario_id"));
                        c.setAlunoNome(rs.getString("aluno_nome"));
                        c.setDescricao(rs.getString("descricao"));
                        c.setValor(rs.getBigDecimal("valor"));
                        c.setVencimento(rs.getDate("vencimento").toLocalDate());
                        c.setStatus(Cobranca.Status.valueOf(rs.getString("status")));
                        lista.add(c);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar cobranças: " + e.getMessage(), e);
        }
        return lista;
    }

    public void inserir(int usuarioId, String descricao, BigDecimal valor, java.time.LocalDate vencimento) {
        String sql = "INSERT INTO COBRANCA (usuario_id, descricao, valor, vencimento, status) VALUES (?, ?, ?, ?, 'ABERTA')";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, descricao);
            ps.setBigDecimal(3, valor);
            ps.setDate(4, Date.valueOf(vencimento));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar cobrança: " + e.getMessage(), e);
        }
    }

    /** Cancela uma cobrança em aberto ou vencida (cobranças pagas não podem ser canceladas). */
    public void cancelar(int cobrancaId) {
        String sql = "UPDATE COBRANCA SET status = 'CANCELADA' WHERE cobranca_id = ? AND status IN ('ABERTA', 'VENCIDA')";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cobrancaId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Só é possível cancelar cobranças em aberto ou vencidas.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cancelar cobrança: " + e.getMessage(), e);
        }
    }

    /** Quantidade de alunos com ao menos uma cobrança vencida. */
    public int alunosInadimplentes() {
        try (Connection conn = db.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE COBRANCA SET status = 'VENCIDA' WHERE status = 'ABERTA' AND vencimento < CURDATE()");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT usuario_id) FROM COBRANCA "
                    + "WHERE status = 'VENCIDA' AND usuario_id IS NOT NULL")) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar inadimplência: " + e.getMessage(), e);
        }
    }

    /** Pagamentos recebidos de todos os alunos. */
    public List<Pagamento> recebimentos() {
        String sql = "SELECT p.*, c.descricao, u.nome AS aluno_nome FROM PAGAMENTO p "
                + "JOIN COBRANCA c ON c.cobranca_id = p.cobranca_id JOIN USUARIO u ON u.usuario_id = c.usuario_id "
                + "ORDER BY p.data_pagamento DESC";
        List<Pagamento> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pagamento p = new Pagamento();
                p.setPagamentoId(rs.getInt("pagamento_id"));
                p.setCobrancaId(rs.getInt("cobranca_id"));
                p.setAlunoNome(rs.getString("aluno_nome"));
                p.setDescricaoCobranca(rs.getString("descricao"));
                p.setMetodo(Pagamento.Metodo.valueOf(rs.getString("metodo")));
                p.setValorPago(rs.getBigDecimal("valor_pago"));
                Timestamp t = rs.getTimestamp("data_pagamento");
                if (t != null) p.setDataPagamento(t.toLocalDateTime());
                p.setTransacaoId(rs.getString("transacao_id"));
                p.setStatus(rs.getString("status"));
                lista.add(p);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar pagamentos: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<Pagamento> historico(int usuarioId) {
        String sql = "SELECT p.*, c.descricao FROM PAGAMENTO p JOIN COBRANCA c ON c.cobranca_id = p.cobranca_id "
                + "WHERE c.usuario_id = ? ORDER BY p.data_pagamento DESC";
        List<Pagamento> lista = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pagamento p = new Pagamento();
                    p.setPagamentoId(rs.getInt("pagamento_id"));
                    p.setCobrancaId(rs.getInt("cobranca_id"));
                    p.setDescricaoCobranca(rs.getString("descricao"));
                    p.setMetodo(Pagamento.Metodo.valueOf(rs.getString("metodo")));
                    BigDecimal v = rs.getBigDecimal("valor_pago");
                    p.setValorPago(v);
                    Timestamp t = rs.getTimestamp("data_pagamento");
                    if (t != null) p.setDataPagamento(t.toLocalDateTime());
                    p.setTransacaoId(rs.getString("transacao_id"));
                    p.setStatus(rs.getString("status"));
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar pagamentos: " + e.getMessage(), e);
        }
        return lista;
    }
}
