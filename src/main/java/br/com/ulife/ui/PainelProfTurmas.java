package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.AlunoTurma;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;
import br.com.ulife.service.ResumoTurma;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Turmas do professor e lista de alunos com frequência, média e situação. */
public class PainelProfTurmas extends JPanel {

    private final TurmaDAO turmaDAO = new TurmaDAO();
    private final AcademicoDAO acad = new AcademicoDAO();
    private final JComboBox<Turma> cmbTurma = new JComboBox<>();
    private final JPanel conteudo = new JPanel(new BorderLayout());

    public PainelProfTurmas(Usuario prof) {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        conteudo.setOpaque(false);

        List<Turma> turmas;
        try {
            turmas = turmaDAO.listar("", prof.getUsuarioId());
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }
        if (turmas.isEmpty()) {
            add(Estilo.rotulo("Você ainda não tem turmas atribuídas. Peça à coordenação.", Font.PLAIN, 14, Estilo.MU));
            return;
        }
        for (Turma t : turmas) {
            cmbTurma.addItem(t);
        }
        cmbTurma.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbTurma.addActionListener(e -> carregar());

        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setOpaque(false);
        barra.add(Estilo.rotulo("Turma:", Font.BOLD, 14, Estilo.MU), BorderLayout.WEST);
        barra.add(cmbTurma, BorderLayout.CENTER);
        add(barra, BorderLayout.NORTH);
        add(conteudo, BorderLayout.CENTER);
        carregar();
    }

    private void carregar() {
        Turma t = (Turma) cmbTurma.getSelectedItem();
        conteudo.removeAll();
        if (t == null) {
            return;
        }
        ResumoTurma r = ResumoTurma.calcular(t.getTurmaId());
        List<AlunoTurma> alunos = turmaDAO.alunosDaTurma(t.getTurmaId());
        Map<Integer, Map<String, BigDecimal>> notas = acad.notasDaTurma(t.getTurmaId());
        Map<Integer, int[]> freq = acad.resumoFrequencia(t.getTurmaId());

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(Estilo.indicador("Alunos", String.valueOf(r.alunos), t.getSemestre() + " · " + t.getCargaHoraria() + "h", Estilo.AC));
        cards.add(Estilo.indicador("Frequência média", r.frequenciaTexto(), "aulas já lançadas", Estilo.AC));
        cards.add(Estilo.indicador("Média da turma", r.mediaTexto(), "alunos com A1–A3", Estilo.AC));
        cards.add(Estilo.indicador("Sem nota completa", String.valueOf(r.semNotaCompleta), "alunos", r.semNotaCompleta == 0 ? Estilo.OK : new Color(0xe8862a)));

        DefaultTableModel m = new DefaultTableModel(
                new String[]{"RA", "Aluno", "E-mail", "Presenças", "Frequência", "Média", "Situação"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        for (AlunoTurma a : alunos) {
            int[] f = freq.get(a.getMatriculaId());
            BigDecimal media = AcademicoDAO.media(notas.get(a.getMatriculaId()));
            String situacao;
            if (f != null && f[1] > 0 && f[0] * 100.0 / f[1] < 75) {
                situacao = "Frequência baixa";
            } else if (media != null) {
                situacao = media.compareTo(AcademicoDAO.MEDIA_APROVACAO) >= 0 ? "Aprovado" : "Reprovado";
            } else {
                situacao = "Em andamento";
            }
            m.addRow(new Object[]{a.getRa(), a.getNome(), a.getEmail(),
                    f == null ? "—" : f[0] + " de " + f[1],
                    f == null || f[1] == 0 ? "—" : String.format("%.0f%%", f[0] * 100.0 / f[1]),
                    media == null ? "—" : media.toPlainString().replace('.', ','), situacao});
        }
        JTable tabela = new JTable(m);
        Estilo.estilizarTabela(tabela, "Situação");

        JPanel col = new JPanel(new BorderLayout(0, 16));
        col.setOpaque(false);
        col.add(cards, BorderLayout.NORTH);
        col.add(Estilo.tabelaSemRolagem(tabela), BorderLayout.CENTER);
        conteudo.add(col, BorderLayout.CENTER);
        conteudo.revalidate();
        conteudo.repaint();
    }
}
