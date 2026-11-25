package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.LinhaFrequencia;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lançamento de frequência. Os minutos de cada aluno vêm do registro da videochamada; o sistema sugere
 * a presença (75% da aula) e o professor confere e lança.
 */
public class PainelFrequencia extends JPanel {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int COL_MIN = 2, COL_SUG = 3, COL_PRESENTE = 4, COL_JUST = 5;

    private final TurmaDAO turmaDAO = new TurmaDAO();
    private final AcademicoDAO acad = new AcademicoDAO();
    private final JComboBox<Turma> cmbTurma = new JComboBox<>();
    private final JComboBox<String> cmbAula = new JComboBox<>();
    private final JPanel statusBox = Estilo.acoes(0);
    private final JLabel lblMsg = Estilo.rotulo(" ", Font.PLAIN, 13, Estilo.MU);
    private final Estilo.Botao btnLancar = Estilo.primario("Lançar frequência");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"RA", "Aluno", "Videochamada", "Sugestão", "Presente", "Justificativa"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == COL_PRESENTE || c == COL_JUST; }
        @Override public Class<?> getColumnClass(int c) { return c == COL_PRESENTE ? Boolean.class : String.class; }
    };
    private final JTable tabela = new JTable(modelo);

    private List<AcademicoDAO.Sessao> sessoes = new ArrayList<>();
    private List<LinhaFrequencia> linhas = new ArrayList<>();
    private boolean carregando;

    public PainelFrequencia(Usuario prof) {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        List<Turma> turmas;
        try {
            turmas = turmaDAO.listar("", prof.getUsuarioId());
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }
        if (turmas.isEmpty()) {
            add(Estilo.rotulo("Você ainda não tem turmas atribuídas.", Font.PLAIN, 14, Estilo.MU));
            return;
        }
        for (Turma t : turmas) {
            cmbTurma.addItem(t);
        }
        cmbTurma.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbAula.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbTurma.addActionListener(e -> carregarSessoes(null));
        cmbAula.addActionListener(e -> {
            if (!carregando) {
                carregarLinhas();
            }
        });

        JPanel selecao = new JPanel(new GridBagLayout());
        selecao.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 0, 10);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        g.gridx = 0;
        selecao.add(Estilo.rotulo("Turma:", Font.BOLD, 14, Estilo.MU), g);
        g.gridx = 1;
        g.weightx = 1;
        selecao.add(cmbTurma, g);
        g.gridx = 2;
        g.weightx = 0;
        selecao.add(Estilo.rotulo("Aula:", Font.BOLD, 14, Estilo.MU), g);
        g.gridx = 3;
        g.weightx = 0.6;
        selecao.add(cmbAula, g);
        g.gridx = 4;
        g.weightx = 0;
        g.insets = new Insets(0, 4, 0, 0);
        statusBox.setOpaque(false);
        selecao.add(statusBox, g);

        JPanel faixa = new JPanel(new BorderLayout());
        faixa.setBackground(new Color(0xe3ecf9));
        faixa.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        faixa.add(Estilo.texto("Os minutos vêm do registro da videochamada. O sistema sugere \"Presente\" quando o aluno "
                + "ficou conectado por " + LinhaFrequencia.MINIMO_PRESENCA_MIN + " min ou mais (75% de "
                + LinhaFrequencia.DURACAO_AULA_MIN + " min). Confira, ajuste se precisar e clique em lançar.",
                900, Font.PLAIN, 13, Estilo.P), BorderLayout.CENTER);

        JPanel topo = new JPanel(new BorderLayout(0, 10));
        topo.setOpaque(false);
        topo.add(selecao, BorderLayout.NORTH);
        topo.add(faixa, BorderLayout.CENTER);
        add(topo, BorderLayout.NORTH);

        Estilo.estilizarTabela(tabela, "Sugestão");
        tabela.setRowHeight(44);
        tabela.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tabela.setFocusable(true);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(Estilo.tabelaSemRolagem(tabela), BorderLayout.CENTER);
        JPanel sul = Estilo.acoes(12);
        sul.setOpaque(false);
        Estilo.Botao aplicar = Estilo.neutro("Aplicar sugestão da videochamada");
        aplicar.addActionListener(e -> aplicarSugestao());
        btnLancar.addActionListener(e -> lancar());
        sul.add(btnLancar);
        sul.add(aplicar);
        sul.add(lblMsg);
        centro.add(sul, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        for (int i = 0; i < turmas.size(); i++) { // abre na primeira turma que já tem aulas
            try {
                if (!acad.sessoes(turmas.get(i).getTurmaId()).isEmpty()) {
                    cmbTurma.setSelectedIndex(i);
                    return;
                }
            } catch (RuntimeException ignorada) {
                break;
            }
        }
        carregarSessoes(null);
    }

    private Turma turma() {
        return (Turma) cmbTurma.getSelectedItem();
    }

    private void carregarSessoes(LocalDate manter) {
        Turma t = turma();
        if (t == null) {
            return;
        }
        carregando = true;
        sessoes = acad.sessoes(t.getTurmaId());
        cmbAula.removeAllItems();
        int sel = 0;
        for (int i = 0; i < sessoes.size(); i++) {
            AcademicoDAO.Sessao s = sessoes.get(i);
            cmbAula.addItem(s.data.format(DATA) + (s.lancada ? "  ·  lançada" : "  ·  aguardando lançamento"));
            if (manter != null && s.data.equals(manter)) {
                sel = i;
            }
        }
        if (!sessoes.isEmpty()) {
            cmbAula.setSelectedIndex(sel);
        }
        carregando = false;
        carregarLinhas();
    }

    private void carregarLinhas() {
        modelo.setRowCount(0);
        statusBox.removeAll();
        lblMsg.setText(" ");
        int i = cmbAula.getSelectedIndex();
        Turma t = turma();
        if (t == null || i < 0 || i >= sessoes.size()) {
            linhas = new ArrayList<>();
            btnLancar.setEnabled(false);
            statusBox.revalidate();
            statusBox.repaint();
            return;
        }
        AcademicoDAO.Sessao s = sessoes.get(i);
        linhas = acad.frequenciaDaSessao(t.getTurmaId(), s.data);
        for (LinhaFrequencia l : linhas) {
            boolean presente = l.isLancada() ? l.isPresenca() : l.isPresencaSugerida();
            String min = l.getMinutos() == null ? "—"
                    : l.getMinutos() + " min (" + Math.round(l.getMinutos() * 100.0 / LinhaFrequencia.DURACAO_AULA_MIN) + "%)";
            modelo.addRow(new Object[]{l.getRa(), l.getNome(), min,
                    l.isPresencaSugerida() ? "Presente" : "Ausente", presente,
                    l.getJustificativa() == null ? "" : l.getJustificativa()});
        }
        statusBox.add(new Estilo.Tag(s.lancada ? "Lançada" : "Pendente de lançamento"));
        btnLancar.setText(s.lancada ? "Atualizar lançamento" : "Lançar frequência");
        btnLancar.setEnabled(true);
        statusBox.revalidate();
        statusBox.repaint();
    }

    private void aplicarSugestao() {
        if (tabela.isEditing()) {
            tabela.getCellEditor().stopCellEditing();
        }
        for (int r = 0; r < linhas.size(); r++) {
            modelo.setValueAt(linhas.get(r).isPresencaSugerida(), r, COL_PRESENTE);
        }
        lblMsg.setForeground(Estilo.MU);
        lblMsg.setText("Sugestão aplicada. Clique em lançar para confirmar.");
    }

    private void lancar() {
        if (tabela.isEditing()) {
            tabela.getCellEditor().stopCellEditing();
        }
        int i = cmbAula.getSelectedIndex();
        if (i < 0 || linhas.isEmpty()) {
            return;
        }
        LocalDate data = sessoes.get(i).data;
        int presentes = 0;
        for (int r = 0; r < linhas.size(); r++) {
            boolean p = Boolean.TRUE.equals(modelo.getValueAt(r, COL_PRESENTE));
            linhas.get(r).setPresenca(p);
            Object j = modelo.getValueAt(r, COL_JUST);
            linhas.get(r).setJustificativa(j == null || j.toString().trim().isEmpty() ? null : j.toString().trim());
            if (p) {
                presentes++;
            }
        }
        try {
            acad.lancarFrequencia(linhas, data);
        } catch (RuntimeException ex) {
            lblMsg.setForeground(Estilo.ERRO);
            lblMsg.setText(ex.getMessage());
            return;
        }
        int total = linhas.size();
        carregarSessoes(data);
        lblMsg.setForeground(Estilo.OK);
        lblMsg.setText("Frequência lançada: " + presentes + " presentes e " + (total - presentes) + " ausentes.");
    }
}
