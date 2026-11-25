package br.com.ulife.ui;

import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.dao.UsuarioDAO;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Gestão de turmas (coordenador): cadastro, alteração, consulta, exclusão e matrícula de alunos. */
public class PainelTurmas extends JPanel {

    private static final Pattern CODIGO = Pattern.compile("^[\\w.\\-]+$");

    private final TurmaDAO dao = new TurmaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private final CardLayout cards = new CardLayout();
    private final JPanel raiz = new JPanel(cards);

    private final JTextField txtBusca = Estilo.campo();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Código", "Turma", "Professor", "Semestre", "Carga", "Alunos"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private List<Turma> exibidas = new ArrayList<>();

    private final JLabel lblTituloForm = Estilo.rotulo("", Font.BOLD, 18, Estilo.TX);
    private final JTextField txtCodigo = Estilo.campo();
    private final JTextField txtNome = Estilo.campo();
    private final JComboBox<Usuario> cmbProfessor = new JComboBox<>();
    private final JTextField txtSemestre = Estilo.campo();
    private final JTextField txtAno = Estilo.campo();
    private final JTextField txtCarga = Estilo.campo();
    private final JTextArea txtDescricao = new JTextArea(3, 20);
    private final JLabel lblErro = Estilo.rotulo(" ", Font.PLAIN, 13, Estilo.ERRO);
    private Turma emEdicao;

    public PainelTurmas() {
        super(new BorderLayout());
        setOpaque(false);
        raiz.setOpaque(false);
        raiz.add(montarLista(), "lista");
        raiz.add(montarForm(), "form");
        add(raiz, BorderLayout.CENTER);
        recarregar();
    }

    // ------------------------------------------------------------------ lista

    private JComponent montarLista() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        txtBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recarregar(); }
            public void removeUpdate(DocumentEvent e) { recarregar(); }
            public void changedUpdate(DocumentEvent e) { recarregar(); }
        });
        JPanel busca = new JPanel(new BorderLayout(8, 0));
        busca.setOpaque(false);
        busca.add(Estilo.rotulo("Consultar:", Font.PLAIN, 14, Estilo.MU), BorderLayout.WEST);
        busca.add(txtBusca, BorderLayout.CENTER);
        barra.add(busca, BorderLayout.CENTER);
        Estilo.Botao novo = Estilo.primario("+ Nova turma");
        novo.addActionListener(e -> abrirForm(null));
        barra.add(novo, BorderLayout.EAST);

        Estilo.estilizarTabela(tabela);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(280);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    alterar();
                }
            }
        });
        JScrollPane sp = new JScrollPane(tabela);
        sp.setBorder(BorderFactory.createLineBorder(Estilo.BD));
        sp.getViewport().setBackground(Estilo.CARD);
        sp.setPreferredSize(new Dimension(800, 360));

        JPanel acoes = Estilo.acoes(10);
        acoes.setOpaque(false);
        Estilo.Botao alunos = Estilo.primario("Alunos da turma");
        alunos.addActionListener(e -> {
            Turma t = selecionada();
            if (t != null) {
                DialogoMatriculas.mostrar(this, t);
                recarregar();
            }
        });
        Estilo.Botao alterar = Estilo.neutro("Alterar");
        alterar.addActionListener(e -> alterar());
        Estilo.Botao excluir = Estilo.perigo("Excluir");
        excluir.addActionListener(e -> excluir());
        acoes.add(alunos);
        acoes.add(alterar);
        acoes.add(excluir);
        acoes.add(Estilo.rotulo("Selecione uma turma na lista.", Font.PLAIN, 13, Estilo.MU));

        p.add(barra, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(acoes, BorderLayout.SOUTH);
        return p;
    }

    private void recarregar() {
        try {
            exibidas = dao.listar(txtBusca.getText(), null);
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
            return;
        }
        modelo.setRowCount(0);
        for (Turma t : exibidas) {
            modelo.addRow(new Object[]{t.getCodigo(), t.getNome(), t.getProfessorNome(), t.getSemestre(),
                    t.getCargaHoraria() + "h", t.getQtdAlunos()});
        }
    }

    private Turma selecionada() {
        int row = tabela.getSelectedRow();
        if (row < 0 || row >= exibidas.size()) {
            Estilo.aviso(this, "Selecione uma turma na lista.");
            return null;
        }
        return exibidas.get(row);
    }

    private void alterar() {
        Turma t = selecionada();
        if (t != null) {
            abrirForm(t);
        }
    }

    private void excluir() {
        Turma t = selecionada();
        if (t == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "Excluir a turma \"" + t.getCodigo() + "\"? As matrículas, notas e "
                + "frequências dela também serão apagadas. Esta ação não pode ser desfeita.",
                "Excluir turma", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            try {
                dao.excluir(t.getTurmaId());
                recarregar();
            } catch (RuntimeException ex) {
                Estilo.erro(this, ex.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ formulário

    private JComponent montarForm() {
        cmbProfessor.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbProfessor.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v == null ? "" : ((Usuario) v).getNome(), i, s, f);
            }
        });
        txtDescricao.setFont(Estilo.fonte(Font.PLAIN, 14));
        txtDescricao.setLineWrap(true);
        txtDescricao.setWrapStyleWord(true);
        txtDescricao.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane spDesc = new JScrollPane(txtDescricao);
        spDesc.setBorder(BorderFactory.createLineBorder(Estilo.BD));

        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        p.add(lblTituloForm, BorderLayout.NORTH);

        Estilo.Cartao c = new Estilo.Cartao(new GridBagLayout(), 24);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 16, 22);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        PainelUsuarios.campo(c, g, 0, 0, "Código", txtCodigo);
        PainelUsuarios.campo(c, g, 1, 0, "Nome da turma", txtNome);
        PainelUsuarios.campo(c, g, 0, 1, "Professor responsável", cmbProfessor);
        PainelUsuarios.campo(c, g, 1, 1, "Semestre (ex.: 2026/2)", txtSemestre);
        PainelUsuarios.campo(c, g, 0, 2, "Ano", txtAno);
        PainelUsuarios.campo(c, g, 1, 2, "Carga horária total (horas)", txtCarga);
        JPanel celDesc = new JPanel(new BorderLayout(0, 6));
        celDesc.setOpaque(false);
        celDesc.add(Estilo.rotulo("Descrição (opcional)", Font.BOLD, 13, Estilo.MU), BorderLayout.NORTH);
        celDesc.add(spDesc, BorderLayout.CENTER);
        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        c.add(celDesc, g);

        g.gridy = 4;
        c.add(lblErro, g);

        JPanel btns = Estilo.acoes(12);
        btns.setOpaque(false);
        Estilo.Botao salvar = Estilo.primario("Salvar");
        salvar.addActionListener(e -> salvar());
        Estilo.Botao cancelar = Estilo.neutro("Cancelar");
        cancelar.addActionListener(e -> cards.show(raiz, "lista"));
        btns.add(salvar);
        btns.add(cancelar);
        g.gridy = 5;
        c.add(btns, g);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(c, BorderLayout.NORTH);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    private void abrirForm(Turma t) {
        emEdicao = t;
        lblErro.setText(" ");
        cmbProfessor.removeAllItems();
        try {
            for (Usuario u : usuarioDAO.listarPorPerfil(Usuario.Perfil.PROFESSOR)) {
                cmbProfessor.addItem(u);
            }
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
            return;
        }
        if (cmbProfessor.getItemCount() == 0) {
            Estilo.aviso(this, "Cadastre ao menos um usuário com perfil Professor antes de criar turmas.");
            return;
        }
        if (t == null) {
            lblTituloForm.setText("Nova turma");
            txtCodigo.setText("");
            txtCodigo.setEditable(true);
            txtNome.setText("");
            txtSemestre.setText("2026/2");
            txtAno.setText("2026");
            txtCarga.setText("80");
            txtDescricao.setText("");
            cmbProfessor.setSelectedIndex(0);
        } else {
            lblTituloForm.setText("Alterar turma");
            txtCodigo.setText(t.getCodigo());
            txtCodigo.setEditable(false);
            txtNome.setText(t.getNome());
            txtSemestre.setText(t.getSemestre());
            txtAno.setText(String.valueOf(t.getAno()));
            txtCarga.setText(String.valueOf(t.getCargaHoraria()));
            txtDescricao.setText(t.getDescricao() == null ? "" : t.getDescricao());
            for (int i = 0; i < cmbProfessor.getItemCount(); i++) {
                if (cmbProfessor.getItemAt(i).getUsuarioId().equals(t.getProfessorId())) {
                    cmbProfessor.setSelectedIndex(i);
                }
            }
        }
        cards.show(raiz, "form");
        txtNome.requestFocusInWindow();
    }

    private void salvar() {
        String codigo = txtCodigo.getText().trim();
        String nome = txtNome.getText().trim();
        String semestre = txtSemestre.getText().trim();
        Usuario prof = (Usuario) cmbProfessor.getSelectedItem();
        boolean novo = emEdicao == null;

        int ano = 0, carga = 0;
        String erro = null;
        if (codigo.isEmpty() || nome.isEmpty() || semestre.isEmpty() || prof == null) {
            erro = "Preencha todos os campos obrigatórios.";
        } else if (codigo.length() > 20 || !CODIGO.matcher(codigo).matches()) {
            erro = "Código: até 20 caracteres, apenas letras, números, ponto, hífen ou underscore.";
        } else if (semestre.length() > 10) {
            erro = "Semestre: até 10 caracteres.";
        } else {
            try {
                ano = Integer.parseInt(txtAno.getText().trim());
                carga = Integer.parseInt(txtCarga.getText().trim());
                if (ano < 2000 || ano > 2100) {
                    erro = "Ano inválido.";
                } else if (carga < 1 || carga > 400) {
                    erro = "Carga horária deve ficar entre 1 e 400 horas.";
                }
            } catch (NumberFormatException ex) {
                erro = "Ano e carga horária devem ser números.";
            }
        }
        if (erro != null) {
            lblErro.setText(erro);
            return;
        }

        Turma t = novo ? new Turma() : emEdicao;
        t.setCodigo(codigo);
        t.setNome(nome);
        t.setDescricao(txtDescricao.getText().trim());
        t.setProfessorId(prof.getUsuarioId());
        t.setSemestre(semestre);
        t.setAno(ano);
        t.setCargaHoraria(carga);
        try {
            if (novo) {
                if (dao.findByCodigo(codigo).isPresent()) {
                    lblErro.setText("Já existe uma turma com este código.");
                    return;
                }
                dao.inserir(t);
            } else {
                dao.atualizar(t);
            }
        } catch (RuntimeException ex) {
            lblErro.setText(ex.getMessage());
            return;
        }
        cards.show(raiz, "lista");
        recarregar();
    }
}
