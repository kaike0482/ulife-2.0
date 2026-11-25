package br.com.ulife.ui;

import br.com.ulife.dao.CobrancaDAO;
import br.com.ulife.model.Pagamento;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Pagamentos recebidos de todos os alunos (visão do coordenador). */
public class PainelRecebimentos extends JPanel {

    public PainelRecebimentos() {
        this(true);
    }

    public PainelRecebimentos(boolean comResumo) {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        List<Pagamento> lista;
        try {
            lista = new CobrancaDAO().recebimentos();
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro ao carregar os pagamentos: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        int pix = 0, boleto = 0;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Data", "Aluno", "Descrição", "Método", "Valor", "Transação", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Pagamento p : lista) {
            total = total.add(p.getValorPago());
            if (p.getMetodo() == Pagamento.Metodo.PIX) pix++; else boleto++;
            m.addRow(new Object[]{p.getDataPagamento() == null ? "" : p.getDataPagamento().format(f),
                    p.getAlunoNome(), p.getDescricaoCobranca(), p.getMetodo().getRotulo(),
                    DialogoPagamento.MOEDA.format(p.getValorPago()), p.getTransacaoId(),
                    "CONFIRMADO".equals(p.getStatus()) ? "Confirmado" : p.getStatus()});
        }
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(Estilo.indicador("Total recebido", DialogoPagamento.MOEDA.format(total), "pagamentos confirmados", Estilo.OK));
        cards.add(Estilo.indicador("Pagamentos", String.valueOf(lista.size()), "registrados", Estilo.AC));
        cards.add(Estilo.indicador("PIX / Boleto", pix + " / " + boleto, "por método", Estilo.AC));
        if (comResumo) {
            add(cards, BorderLayout.NORTH);
        }

        if (lista.isEmpty()) {
            add(Estilo.rotulo("Nenhum pagamento recebido ainda.", Font.PLAIN, 14, Estilo.MU), BorderLayout.CENTER);
            return;
        }
        JTable t = new JTable(m);
        Estilo.estilizarTabela(t, "Status");
        add(Estilo.tabelaSemRolagem(t), BorderLayout.CENTER);
    }
}
