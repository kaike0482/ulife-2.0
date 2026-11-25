package br.com.ulife.ui;

import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.AlunoTurma;
import br.com.ulife.model.Turma;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Matricular e remover alunos de uma turma. */
public class DialogoMatriculas extends JDialog {

    private final TurmaDAO dao = new TurmaDAO();
    private final Turma turma;
    private final DefaultListModel<AlunoTurma> matriculados = new DefaultListModel<>();
    private final DefaultListModel<AlunoTurma> disponiveis = new DefaultListModel<>();
    private final JList<AlunoTurma> listaMat = new JList<>(matriculados);
    private final JList<AlunoTurma> listaDisp = new JList<>(disponiveis);
    private final JLabel tituloMat = Estilo.rotulo("", Font.BOLD, 14, Estilo.TX);

    private DialogoMatriculas(Window dono, Turma turma) {
        super(dono, "Alunos — " + turma.getNome(), ModalityType.APPLICATION_MODAL);
        this.turma = turma;
        montar();
        recarregar();
        setSize(760, 480);
        setLocationRelativeTo(dono);
    }

    public static void mostrar(Component pai, Turma turma) {
        new DialogoMatriculas(SwingUtilities.getWindowAncestor(pai), turma).setVisible(true);
    }

    private void montar() {
        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Estilo.BG);
        raiz.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        raiz.add(Estilo.rotulo(turma.getCodigo() + " — " + turma.getNome() + "  ·  Prof. " + turma.getProfessorNome(),
                Font.BOLD, 15, Estilo.P), BorderLayout.NORTH);

        JPanel colunas = new JPanel(new GridLayout(1, 2, 16, 0));
        colunas.setOpaque(false);
        colunas.add(coluna(tituloMat, listaMat, "Remover da turma", false));
        colunas.add(coluna(Estilo.rotulo("Alunos disponíveis", Font.BOLD, 14, Estilo.TX), listaDisp, "Matricular", true));
        raiz.add(colunas, BorderLayout.CENTER);

        JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sul.setOpaque(false);
        Estilo.Botao fechar = Estilo.neutro("Fechar");
        fechar.addActionListener(e -> dispose());
        sul.add(fechar);
        raiz.add(sul, BorderLayout.SOUTH);
        setContentPane(raiz);
    }

    private JComponent coluna(JLabel titulo, JList<AlunoTurma> lista, String botao, boolean matricular) {
        lista.setFont(Estilo.fonte(Font.PLAIN, 14));
        lista.setFixedCellHeight(30);
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 10), 14);
        c.add(titulo, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(lista);
        sp.setBorder(BorderFactory.createLineBorder(Estilo.BD));
        c.add(sp, BorderLayout.CENTER);
        Estilo.Botao b = matricular ? Estilo.primario(botao) : Estilo.perigo(botao);
        b.addActionListener(e -> acao(lista, matricular));
        JPanel s = Estilo.acoes(0);
        s.setOpaque(false);
        s.add(b);
        c.add(s, BorderLayout.SOUTH);
        return c;
    }

    private void acao(JList<AlunoTurma> lista, boolean matricular) {
        List<AlunoTurma> sel = lista.getSelectedValuesList();
        if (sel.isEmpty()) {
            Estilo.aviso(this, "Selecione ao menos um aluno.");
            return;
        }
        if (!matricular && JOptionPane.showConfirmDialog(this,
                "Remover " + sel.size() + " aluno(s) da turma? As notas e frequências deles nesta turma serão apagadas.",
                "Remover matrícula", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            for (AlunoTurma a : sel) {
                if (matricular) {
                    dao.matricular(turma.getTurmaId(), a.getUsuarioId());
                } else {
                    dao.desmatricular(a.getMatriculaId());
                }
            }
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
        }
        recarregar();
    }

    private void recarregar() {
        matriculados.clear();
        disponiveis.clear();
        List<AlunoTurma> m = dao.alunosDaTurma(turma.getTurmaId());
        m.forEach(matriculados::addElement);
        dao.alunosDisponiveis(turma.getTurmaId()).forEach(disponiveis::addElement);
        tituloMat.setText("Matriculados (" + m.size() + ")");
    }
}
