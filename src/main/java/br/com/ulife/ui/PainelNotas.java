package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.AlunoTurma;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lançamento de notas A1, A2 e A3 por turma, com média e situação calculadas na hora. */
public class PainelNotas extends JPanel {

    private static final int COL_A1 = 2, COL_A3 = 4, COL_MEDIA = 5, COL_SIT = 6;

    private final TurmaDAO turmaDAO = new TurmaDAO();
    private final AcademicoDAO acad = new AcademicoDAO();
    private final JComboBox<Turma> cmbTurma = new JComboBox<>();
    private final JLabel lblMsg = Estilo.rotulo(" ", Font.PLAIN, 13, Estilo.MU);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"RA", "Aluno", "A1", "A2", "A3", "Média", "Situação"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return col >= COL_A1 && col <= COL_A3; }
    };
    private final JTable tabela = new JTable(modelo);
    private List<AlunoTurma> alunos;
    private boolean carregando;

    public PainelNotas(Usuario prof) {
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
        cmbTurma.addActionListener(e -> carregar());

        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setOpaque(false);
        barra.add(Estilo.rotulo("Turma:", Font.BOLD, 14, Estilo.MU), BorderLayout.WEST);
        barra.add(cmbTurma, BorderLayout.CENTER);
        JPanel topo = new JPanel(new BorderLayout(0, 8));
        topo.setOpaque(false);
        topo.add(barra, BorderLayout.NORTH);
        topo.add(Estilo.texto("Clique em uma nota (A1, A2 ou A3) para digitar de 0 a 10. A média é a média simples das "
                + "três avaliações; aprovado com média 6,0 ou mais.", 900, Font.PLAIN, 13, Estilo.MU), BorderLayout.CENTER);
        add(topo, BorderLayout.NORTH);

        Estilo.estilizarTabela(tabela, "Situação");
        tabela.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tabela.setFocusable(true);
        tabela.setRowHeight(44);
        modelo.addTableModelListener(e -> {
            if (!carregando && e.getColumn() >= COL_A1 && e.getColumn() <= COL_A3 && e.getFirstRow() >= 0) {
                recalcular(e.getFirstRow());
            }
        });
        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(Estilo.tabelaSemRolagem(tabela), BorderLayout.CENTER);

        JPanel sul = Estilo.acoes(14);
        sul.setOpaque(false);
        Estilo.Botao salvar = Estilo.primario("Salvar notas");
        salvar.addActionListener(e -> salvar());
        sul.add(salvar);
        sul.add(lblMsg);
        centro.add(sul, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        for (int i = 0; i < turmas.size(); i++) { // abre na primeira turma que já tem alunos
            try {
                if (!turmaDAO.alunosDaTurma(turmas.get(i).getTurmaId()).isEmpty()) {
                    cmbTurma.setSelectedIndex(i);
                    return;
                }
            } catch (RuntimeException ignorada) {
                break;
            }
        }
        carregar();
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Converte o texto digitado; "" = sem nota; lança IllegalArgumentException se inválido. */
    private static BigDecimal parse(Object o) {
        String s = o == null ? "" : o.toString().trim().replace(',', '.');
        if (s.isEmpty()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(BigDecimal.TEN) > 0 || v.scale() > 2) {
                throw new IllegalArgumentException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }

    private void carregar() {
        Turma t = (Turma) cmbTurma.getSelectedItem();
        if (t == null) {
            return;
        }
        carregando = true;
        modelo.setRowCount(0);
        alunos = turmaDAO.alunosDaTurma(t.getTurmaId());
        Map<Integer, Map<String, BigDecimal>> notas = acad.notasDaTurma(t.getTurmaId());
        for (AlunoTurma a : alunos) {
            Map<String, BigDecimal> n = notas.getOrDefault(a.getMatriculaId(), new HashMap<>());
            modelo.addRow(new Object[]{a.getRa(), a.getNome(), fmt(n.get("A1")), fmt(n.get("A2")), fmt(n.get("A3")), "—", ""});
        }
        carregando = false;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            recalcular(i);
        }
        lblMsg.setText(" ");
    }

    private void recalcular(int row) {
        carregando = true;
        try {
            Map<String, BigDecimal> n = new HashMap<>();
            boolean invalida = false;
            for (int c = COL_A1; c <= COL_A3; c++) {
                try {
                    BigDecimal v = parse(modelo.getValueAt(row, c));
                    if (v != null) {
                        n.put("A" + (c - COL_A1 + 1), v);
                    }
                } catch (IllegalArgumentException ex) {
                    invalida = true;
                }
            }
            BigDecimal media = invalida ? null : AcademicoDAO.media(n);
            modelo.setValueAt(media == null ? "—" : fmt(media.setScale(1, RoundingMode.HALF_UP)), row, COL_MEDIA);
            String sit = invalida ? "Pendente" : media == null ? "Em andamento"
                    : media.compareTo(AcademicoDAO.MEDIA_APROVACAO) >= 0 ? "Aprovado" : "Reprovado";
            modelo.setValueAt(invalida ? "Nota inválida" : sit, row, COL_SIT);
        } finally {
            carregando = false;
        }
    }

    private void salvar() {
        if (tabela.isEditing()) {
            tabela.getCellEditor().stopCellEditing();
        }
        BigDecimal[][] valores = new BigDecimal[modelo.getRowCount()][3];
        for (int r = 0; r < modelo.getRowCount(); r++) {
            for (int c = COL_A1; c <= COL_A3; c++) {
                try {
                    valores[r][c - COL_A1] = parse(modelo.getValueAt(r, c));
                } catch (IllegalArgumentException ex) {
                    lblMsg.setForeground(Estilo.ERRO);
                    lblMsg.setText("Nota inválida para " + modelo.getValueAt(r, 1) + " (" + AcademicoDAO.AVALIACOES[c - COL_A1]
                            + "). Use valores de 0 a 10.");
                    return;
                }
            }
        }
        try {
            for (int r = 0; r < alunos.size(); r++) {
                for (int i = 0; i < 3; i++) {
                    acad.salvarNota(alunos.get(r).getMatriculaId(), AcademicoDAO.AVALIACOES[i], valores[r][i]);
                }
            }
        } catch (RuntimeException ex) {
            lblMsg.setForeground(Estilo.ERRO);
            lblMsg.setText(ex.getMessage());
            return;
        }
        carregar();
        lblMsg.setForeground(Estilo.OK);
        lblMsg.setText("Notas salvas com sucesso.");
    }
}
