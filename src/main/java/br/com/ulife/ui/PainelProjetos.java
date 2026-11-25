package br.com.ulife.ui;

import br.com.ulife.dao.ProjetoDAO;
import br.com.ulife.model.Projeto;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Gerenciamento de projetos (ODS): cadastro, alteração, consulta e exclusão. */
public class PainelProjetos extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern CODIGO = Pattern.compile("^[\\w.\\-]+$");

    private final ProjetoDAO dao = new ProjetoDAO();

    private final CardLayout cards = new CardLayout();
    private final JPanel raiz = new JPanel(cards);

    // lista
    private final JTextField txtBusca = Estilo.campo();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Código", "Descrição", "ODS", "Responsável", "Telefone", "Criação", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private List<Projeto> exibidos = new ArrayList<>();

    // formulário
    private final JLabel lblTituloForm = Estilo.rotulo("", Font.BOLD, 18, Estilo.TX);
    private final JTextField txtCodigo = Estilo.campo();
    private final JComboBox<String> cmbOds = new JComboBox<>();
    private final JTextArea txtDescricao = new JTextArea(3, 20);
    private final JTextField txtResponsavel = Estilo.campo();
    private final JTextField txtTelefone = Estilo.campo();
    private final JTextField txtData = Estilo.campo();
    private final JComboBox<Projeto.Status> cmbStatus = new JComboBox<>(Projeto.Status.values());
    private final JLabel lblErro = Estilo.rotulo(" ", Font.PLAIN, 13, Estilo.ERRO);
    private Projeto emEdicao; // null = novo

    public PainelProjetos() {
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
        txtBusca.setToolTipText("Consultar por código, descrição, ODS, responsável, telefone ou status");
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
        JButton novo = Estilo.primario("+ Novo projeto");
        novo.addActionListener(e -> abrirForm(null));
        barra.add(novo, BorderLayout.EAST);

        Estilo.estilizarTabela(tabela, "Status");
        tabela.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(200);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    alterarSelecionado();
                }
            }
        });
        JScrollPane sp = new JScrollPane(tabela);
        sp.setBorder(BorderFactory.createLineBorder(Estilo.BD));
        sp.getViewport().setBackground(Estilo.CARD);
        sp.setPreferredSize(new Dimension(800, 360));

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        acoes.setOpaque(false);
        JButton alterar = Estilo.neutro("Alterar");
        alterar.addActionListener(e -> alterarSelecionado());
        JButton excluir = Estilo.perigo("Excluir");
        excluir.addActionListener(e -> excluirSelecionado());
        acoes.add(alterar);
        acoes.add(excluir);
        acoes.add(Estilo.rotulo("Selecione uma linha (ou dê dois cliques para alterar).", Font.PLAIN, 13, Estilo.MU));

        p.add(barra, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(acoes, BorderLayout.SOUTH);
        return p;
    }

    private void recarregar() {
        try {
            exibidos = dao.listar(txtBusca.getText());
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
            return;
        }
        modelo.setRowCount(0);
        for (Projeto p : exibidos) {
            modelo.addRow(new Object[]{p.getCodigo(), p.getDescricao(), p.getOdsRotulo(), p.getResponsavel(),
                    p.getTelefone(), p.getDataCriacao().format(FMT), p.getStatus().getRotulo()});
        }
    }

    private Projeto selecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0 || row >= exibidos.size()) {
            Estilo.aviso(this, "Selecione um projeto na lista.");
            return null;
        }
        return exibidos.get(row);
    }

    private void alterarSelecionado() {
        Projeto p = selecionado();
        if (p != null) {
            abrirForm(p);
        }
    }

    private void excluirSelecionado() {
        Projeto p = selecionado();
        if (p == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Excluir o projeto \"" + p.getCodigo() + "\"? Esta ação não pode ser desfeita.",
                "Excluir projeto", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            try {
                dao.excluir(p.getCodigo());
                recarregar();
            } catch (RuntimeException ex) {
                Estilo.erro(this, ex.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ formulário

    private JComponent montarForm() {
        for (int i = 0; i < Projeto.ODS.length; i++) {
            cmbOds.addItem("ODS " + (i + 1) + " – " + Projeto.ODS[i]);
        }
        cmbOds.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbStatus.setFont(Estilo.fonte(Font.PLAIN, 14));
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
        PainelUsuarios.campo(c, g, 1, 0, "ODS", cmbOds);
        // descrição ocupa a linha toda
        JPanel celDesc = new JPanel(new BorderLayout(0, 6));
        celDesc.setOpaque(false);
        celDesc.add(Estilo.rotulo("Descrição", Font.BOLD, 13, Estilo.MU), BorderLayout.NORTH);
        celDesc.add(spDesc, BorderLayout.CENTER);
        g.gridx = 0;
        g.gridy = 1;
        g.gridwidth = 2;
        c.add(celDesc, g);
        PainelUsuarios.campo(c, g, 0, 2, "Nome do responsável", txtResponsavel);
        PainelUsuarios.campo(c, g, 1, 2, "Telefone  (ex.: (11) 99999-0000)", txtTelefone);
        PainelUsuarios.campo(c, g, 0, 3, "Data da criação  (dd/mm/aaaa)", txtData);
        PainelUsuarios.campo(c, g, 1, 3, "Status", cmbStatus);

        g.gridx = 0;
        g.gridy = 4;
        g.gridwidth = 2;
        c.add(lblErro, g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btns.setOpaque(false);
        JButton salvar = Estilo.primario("Salvar");
        salvar.addActionListener(e -> salvar());
        JButton cancelar = Estilo.neutro("Cancelar");
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

    private void abrirForm(Projeto p) {
        emEdicao = p;
        lblErro.setText(" ");
        if (p == null) {
            lblTituloForm.setText("Novo projeto");
            try {
                txtCodigo.setText(dao.proximoCodigo());
            } catch (RuntimeException ex) {
                txtCodigo.setText("PRJ-001");
            }
            txtCodigo.setEditable(true);
            cmbOds.setSelectedIndex(-1);
            txtDescricao.setText("");
            txtResponsavel.setText("");
            txtTelefone.setText("");
            txtData.setText(LocalDate.now().format(FMT));
            cmbStatus.setSelectedItem(Projeto.Status.PLANEJADO);
        } else {
            lblTituloForm.setText("Alterar projeto");
            txtCodigo.setText(p.getCodigo());
            txtCodigo.setEditable(false);
            cmbOds.setSelectedIndex(p.getOds() - 1);
            txtDescricao.setText(p.getDescricao());
            txtResponsavel.setText(p.getResponsavel());
            txtTelefone.setText(p.getTelefone());
            txtData.setText(p.getDataCriacao().format(FMT));
            cmbStatus.setSelectedItem(p.getStatus());
        }
        cards.show(raiz, "form");
        txtDescricao.requestFocusInWindow();
    }

    private void salvar() {
        String codigo = txtCodigo.getText().trim();
        String descricao = txtDescricao.getText().trim();
        String responsavel = txtResponsavel.getText().trim();
        String telefone = txtTelefone.getText().trim();
        int ods = cmbOds.getSelectedIndex() + 1;
        boolean novo = emEdicao == null;

        String erro = null;
        LocalDate data = null;
        if (codigo.isEmpty() || descricao.isEmpty() || responsavel.isEmpty() || telefone.isEmpty()) {
            erro = "Preencha todos os campos obrigatórios.";
        } else if (codigo.length() > 20 || !CODIGO.matcher(codigo).matches()) {
            erro = "Código: até 20 caracteres, apenas letras, números, ponto, hífen ou underscore.";
        } else if (ods < 1) {
            erro = "Selecione um ODS.";
        } else if (!PainelUsuarios.TELEFONE.matcher(telefone).matches()) {
            erro = "Telefone inválido. Use o formato (11) 99999-0000.";
        } else {
            try {
                data = LocalDate.parse(txtData.getText().trim(), FMT);
            } catch (DateTimeParseException ex) {
                erro = "Data inválida. Use o formato dd/mm/aaaa.";
            }
        }
        if (erro != null) {
            lblErro.setText(erro);
            return;
        }

        Projeto p = new Projeto();
        p.setCodigo(codigo);
        p.setDescricao(descricao);
        p.setOds(ods);
        p.setResponsavel(responsavel);
        p.setTelefone(telefone);
        p.setDataCriacao(data);
        p.setStatus((Projeto.Status) cmbStatus.getSelectedItem());

        try {
            if (novo) {
                if (dao.findByCodigo(codigo).isPresent()) {
                    lblErro.setText("Já existe um projeto com este código.");
                    return;
                }
                dao.inserir(p);
            } else {
                dao.atualizar(p);
            }
        } catch (RuntimeException ex) {
            lblErro.setText(ex.getMessage());
            return;
        }
        cards.show(raiz, "lista");
        recarregar();
    }
}
