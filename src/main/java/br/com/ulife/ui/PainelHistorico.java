package br.com.ulife.ui;

import br.com.ulife.dao.CobrancaDAO;
import br.com.ulife.model.Pagamento;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Histórico de pagamentos já confirmados do aluno. */
public class PainelHistorico extends JPanel {

    public PainelHistorico(Usuario usuario) {
        super(new BorderLayout());
        setOpaque(false);
        List<Pagamento> lista;
        try {
            lista = new CobrancaDAO().historico(usuario.getUsuarioId());
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro ao carregar o histórico: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }
        if (lista.isEmpty()) {
            add(Estilo.rotulo("Você ainda não realizou pagamentos.", Font.PLAIN, 14, Estilo.MU));
            return;
        }
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Data", "Descrição", "Método", "Valor", "Transação", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Pagamento p : lista) {
            m.addRow(new Object[]{
                    p.getDataPagamento() == null ? "" : p.getDataPagamento().format(f),
                    p.getDescricaoCobranca(),
                    p.getMetodo().getRotulo(),
                    DialogoPagamento.MOEDA.format(p.getValorPago()),
                    p.getTransacaoId(),
                    "CONFIRMADO".equals(p.getStatus()) ? "Confirmado" : p.getStatus()});
        }
        JTable t = new JTable(m);
        Estilo.estilizarTabela(t, "Status");
        add(Estilo.tabelaSemRolagem(t), BorderLayout.CENTER);
    }
}
