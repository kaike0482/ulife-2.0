package br.com.ulife.ui;

import br.com.ulife.dao.PortalAlunoDAO;
import br.com.ulife.dao.PortalAlunoDAO.AtivComp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Validação das atividades complementares registradas pelos alunos (coordenação). */
public class PainelValidacoes extends JPanel {

    private final PortalAlunoDAO dao = new PortalAlunoDAO();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Aluno", "Atividade", "Categoria", "Carga horária"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private List<AtivComp> pendentes = new ArrayList<>();
    private final JPanel conteudo = new JPanel(new BorderLayout(0, 14));

    public PainelValidacoes() {
        super(new BorderLayout());
        setOpaque(false);
        conteudo.setOpaque(false);
        Estilo.estilizarTabela(tabela);
        add(conteudo, BorderLayout.CENTER);
        recarregar();
    }

    private void recarregar() {
        conteudo.removeAll();
        try {
            pendentes = dao.atividadesEmAnalise();
        } catch (RuntimeException ex) {
            conteudo.add(Estilo.rotulo("Erro: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO), BorderLayout.NORTH);
            return;
        }
        int horas = pendentes.stream().mapToInt(AtivComp::horas).sum();
        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
        cards.add(Estilo.indicador("Aguardando validação", String.valueOf(pendentes.size()), "atividades complementares", Estilo.AC));
        cards.add(Estilo.indicador("Carga horária em análise", horas + "h", "somando todas", Estilo.AC));
        conteudo.add(cards, BorderLayout.NORTH);

        if (pendentes.isEmpty()) {
            conteudo.add(Estilo.rotulo("Nenhuma atividade aguardando validação.", Font.PLAIN, 14, Estilo.OK), BorderLayout.CENTER);
        } else {
            modelo.setRowCount(0);
            for (AtivComp a : pendentes) {
                modelo.addRow(new Object[]{a.aluno(), a.descricao(), a.categoria(), a.horas() + "h"});
            }
            JPanel centro = new JPanel(new BorderLayout(0, 12));
            centro.setOpaque(false);
            centro.add(Estilo.tabelaSemRolagem(tabela), BorderLayout.CENTER);
            JPanel acoes = Estilo.acoes(10);
            Estilo.Botao ok = Estilo.primario("Validar");
            ok.addActionListener(e -> decidir(true));
            Estilo.Botao no = Estilo.perigo("Rejeitar");
            no.addActionListener(e -> decidir(false));
            acoes.add(ok);
            acoes.add(no);
            acoes.add(Estilo.rotulo("Selecione uma atividade na lista.", Font.PLAIN, 13, Estilo.MU));
            centro.add(acoes, BorderLayout.SOUTH);
            conteudo.add(centro, BorderLayout.CENTER);
        }
        conteudo.revalidate();
        conteudo.repaint();
    }

    private void decidir(boolean validar) {
        int row = tabela.getSelectedRow();
        if (row < 0 || row >= pendentes.size()) {
            Estilo.aviso(this, "Selecione uma atividade na lista.");
            return;
        }
        AtivComp a = pendentes.get(row);
        String parecer = JOptionPane.showInputDialog(this, (validar ? "Validar" : "Rejeitar") + " \"" + a.descricao() + "\" de "
                + a.aluno() + ".\nParecer (opcional):", validar ? "Validar atividade" : "Rejeitar atividade",
                JOptionPane.QUESTION_MESSAGE);
        if (parecer == null) {
            return; // cancelou
        }
        try {
            dao.decidirAtividade(a.id(), validar, parecer.trim().isEmpty() ? null : parecer.trim());
            recarregar();
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
        }
    }
}
