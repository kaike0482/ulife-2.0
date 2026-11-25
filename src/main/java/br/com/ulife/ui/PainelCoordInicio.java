package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;
import br.com.ulife.service.ResumoTurma;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Painel inicial do coordenador: indicadores gerais e visão das turmas. */
public class PainelCoordInicio extends JPanel {

    public PainelCoordInicio(Usuario coord) {
        super(new BorderLayout(0, 18));
        setOpaque(false);

        AcademicoDAO.Indicadores ind;
        List<Turma> turmas;
        try {
            ind = new AcademicoDAO().indicadores();
            turmas = new TurmaDAO().listar("", null);
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro ao carregar os indicadores: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }

        Color alerta = new Color(0xe8862a);
        JPanel l1 = new JPanel(new GridLayout(1, 4, 16, 0));
        l1.setOpaque(false);
        l1.add(Estilo.indicador("Alunos", String.valueOf(ind.alunos), "contas ativas", Estilo.AC));
        l1.add(Estilo.indicador("Professores", String.valueOf(ind.professores), "contas ativas", Estilo.AC));
        l1.add(Estilo.indicador("Turmas ativas", String.valueOf(ind.turmas), "no semestre", Estilo.AC));
        l1.add(Estilo.indicador("Matrículas", String.valueOf(ind.matriculas), "ativas", Estilo.AC));

        JPanel l2 = new JPanel(new GridLayout(1, 4, 16, 0));
        l2.setOpaque(false);
        l2.add(Estilo.indicador("Mensalidades vencidas", DialogoPagamento.MOEDA.format(ind.valorVencido),
                ind.alunosComVencidas + (ind.alunosComVencidas == 1 ? " aluno inadimplente" : " alunos inadimplentes"),
                ind.alunosComVencidas > 0 ? Estilo.ERRO : Estilo.OK));
        l2.add(Estilo.indicador("Mensalidades em aberto", DialogoPagamento.MOEDA.format(ind.valorEmAberto),
                "a vencer", alerta));
        l2.add(Estilo.indicador("Frequência média", ind.frequenciaMedia == null ? "—"
                : String.format("%.0f%%", ind.frequenciaMedia), "aulas lançadas", Estilo.AC));
        l2.add(Estilo.indicador("Média geral", ind.mediaGeral == null ? "—"
                : ind.mediaGeral.toPlainString().replace('.', ','), "alunos com A1–A3", Estilo.AC));

        JPanel cards = new JPanel(new GridLayout(2, 1, 0, 16));
        cards.setOpaque(false);
        cards.add(l1);
        cards.add(l2);
        add(cards, BorderLayout.NORTH);

        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Código", "Turma", "Professor", "Alunos", "Frequência", "Média"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Turma t : turmas) {
            ResumoTurma r = ResumoTurma.calcular(t.getTurmaId());
            m.addRow(new Object[]{t.getCodigo(), t.getNome(), t.getProfessorNome(), r.alunos,
                    r.frequenciaTexto(), r.mediaTexto()});
        }
        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(Estilo.rotulo("Turmas", Font.BOLD, 16, Estilo.TX), BorderLayout.NORTH);
        JTable t = new JTable(m);
        Estilo.estilizarTabela(t);
        centro.add(Estilo.tabelaSemRolagem(t), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }
}
