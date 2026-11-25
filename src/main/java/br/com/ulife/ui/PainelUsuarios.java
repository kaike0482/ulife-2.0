package br.com.ulife.ui;

import br.com.ulife.dao.UsuarioDAO;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Pattern;

/** Gerenciamento de usuários: cadastro, alteração, consulta e exclusão. */
public class PainelUsuarios extends JPanel {

    static final Pattern TELEFONE = Pattern.compile("^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern LOGIN = Pattern.compile("^[\\w.\\-@]+$");

    private final UsuarioDAO dao = new UsuarioDAO();
    private final Usuario logado;

    private final CardLayout cards = new CardLayout();
    private final JPanel raiz = new JPanel(cards);

    // lista
    private final JTextField txtBusca = Estilo.campo();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Usuário", "Nome", "E-mail", "Telefone", "Perfil"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private List<Usuario> exibidos = new java.util.ArrayList<>();

    // formulário
    private final JLabel lblTituloForm = Estilo.rotulo("", Font.BOLD, 18, Estilo.TX);
    private final JTextField txtUsuario = Estilo.campo();
    private final JTextField txtNome = Estilo.campo();
    private final JTextField txtEmail = Estilo.campo();
    private final JTextField txtTelefone = Estilo.campo();
    private final JPasswordField txtSenha = Estilo.campoSenha();
    private final JComboBox<Usuario.Perfil> cmbPerfil = new JComboBox<>(Usuario.Perfil.values());
    private final JLabel lblErro = Estilo.rotulo(" ", Font.PLAIN, 13, Estilo.ERRO);
    private Usuario emEdicao; // null = novo

    public PainelUsuarios(Usuario logado) {
        super(new BorderLayout());
        this.logado = logado;
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
        txtBusca.putClientProperty("JTextField.placeholderText", "Consultar");
        txtBusca.setToolTipText("Consultar por usuário, nome, e-mail ou telefone");
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
        JButton novo = Estilo.primario("+ Novo usuário");
        novo.addActionListener(e -> abrirForm(null));
        barra.add(novo, BorderLayout.EAST);

        Estilo.estilizarTabela(tabela);
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

        JPanel acoes = Estilo.acoes(10);
        acoes.setOpaque(false);
        JButton alterar = Estilo.neutro("Alterar");
        alterar.addActionListener(e -> alterarSelecionado());
        JButton excluir = Estilo.perigo("Excluir");
        excluir.addActionListener(e -> excluirSelecionado());
        acoes.add(alterar);
        acoes.add(excluir);
        acoes.add(Estilo.rotulo("Selecione uma linha para alterar ou excluir.", Font.PLAIN, 13, Estilo.MU));

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
        for (Usuario u : exibidos) {
            modelo.addRow(new Object[]{u.getUsuario(), u.getNome(), u.getEmail(), u.getTelefone(),
                    rotuloPerfil(u.getPerfil())});
        }
    }

    private Usuario selecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0 || row >= exibidos.size()) {
            Estilo.aviso(this, "Selecione um usuário na lista.");
            return null;
        }
        return exibidos.get(row);
    }

    private void alterarSelecionado() {
        Usuario u = selecionado();
        if (u != null) {
            abrirForm(u);
        }
    }

    private void excluirSelecionado() {
        Usuario u = selecionado();
        if (u == null) {
            return;
        }
        if (u.getUsuarioId().equals(logado.getUsuarioId())) {
            Estilo.aviso(this, "Você não pode excluir o usuário que está logado.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Excluir o usuário \"" + u.getUsuario() + "\"? Esta ação não pode ser desfeita.",
                "Excluir usuário", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            try {
                dao.excluir(u.getUsuarioId());
                recarregar();
            } catch (RuntimeException ex) {
                Estilo.erro(this, ex.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ formulário

    private JComponent montarForm() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        p.add(lblTituloForm, BorderLayout.NORTH);

        Estilo.Cartao c = new Estilo.Cartao(new GridBagLayout(), 24);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 16, 22);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        campo(c, g, 0, 0, "Usuário", txtUsuario);
        campo(c, g, 1, 0, "Nome", txtNome);
        campo(c, g, 0, 1, "E-mail", txtEmail);
        campo(c, g, 1, 1, "Telefone  (ex.: (11) 99999-0000)", txtTelefone);
        campo(c, g, 0, 2, "Senha", txtSenha);
        cmbPerfil.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbPerfil.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v == null ? "" : rotuloPerfil((Usuario.Perfil) v), i, s, f);
            }
        });
        campo(c, g, 1, 2, "Perfil de acesso", cmbPerfil);

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        c.add(lblErro, g);

        JPanel btns = Estilo.acoes(12);
        btns.setOpaque(false);
        JButton salvar = Estilo.primario("Salvar");
        salvar.addActionListener(e -> salvar());
        JButton cancelar = Estilo.neutro("Cancelar");
        cancelar.addActionListener(e -> cards.show(raiz, "lista"));
        btns.add(salvar);
        btns.add(cancelar);
        g.gridy = 4;
        c.add(btns, g);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(c, BorderLayout.NORTH);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    static String rotuloPerfil(Usuario.Perfil p) {
        if (p == null) {
            return "";
        }
        String n = p.name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    static void campo(JPanel painel, GridBagConstraints g, int col, int lin, String rotulo, JComponent campo) {
        JPanel cel = new JPanel(new BorderLayout(0, 6));
        cel.setOpaque(false);
        cel.add(Estilo.rotulo(rotulo, Font.BOLD, 13, Estilo.MU), BorderLayout.NORTH);
        cel.add(campo, BorderLayout.CENTER);
        g.gridx = col;
        g.gridy = lin;
        g.gridwidth = 1;
        painel.add(cel, g);
    }

    private void abrirForm(Usuario u) {
        emEdicao = u;
        lblErro.setText(" ");
        txtSenha.setText("");
        if (u == null) {
            lblTituloForm.setText("Novo usuário");
            txtUsuario.setText("");
            txtUsuario.setEditable(true);
            txtNome.setText("");
            txtEmail.setText("");
            txtTelefone.setText("");
            cmbPerfil.setSelectedItem(Usuario.Perfil.ALUNO);
        } else {
            lblTituloForm.setText("Alterar usuário");
            txtUsuario.setText(u.getUsuario());
            txtUsuario.setEditable(false);
            txtNome.setText(u.getNome());
            txtEmail.setText(u.getEmail());
            txtTelefone.setText(u.getTelefone());
            cmbPerfil.setSelectedItem(u.getPerfil());
        }
        cards.show(raiz, "form");
        txtNome.requestFocusInWindow();
    }

    private void salvar() {
        String usuario = txtUsuario.getText().trim();
        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String telefone = txtTelefone.getText().trim();
        String senha = new String(txtSenha.getPassword());
        boolean novo = emEdicao == null;
        Usuario.Perfil perfil = (Usuario.Perfil) cmbPerfil.getSelectedItem();

        String erro = null;
        if (usuario.isEmpty() || nome.isEmpty() || email.isEmpty() || telefone.isEmpty()) {
            erro = "Preencha todos os campos obrigatórios.";
        } else if (usuario.length() > 30 || !LOGIN.matcher(usuario).matches()) {
            erro = "Usuário: até 30 caracteres, apenas letras, números, ponto, hífen ou underscore.";
        } else if (!EMAIL.matcher(email).matches()) {
            erro = "E-mail inválido.";
        } else if (!TELEFONE.matcher(telefone).matches()) {
            erro = "Telefone inválido. Use o formato (11) 99999-0000.";
        } else if (novo && senha.isEmpty()) {
            erro = "Informe a senha.";
        } else if (!senha.isEmpty() && senha.length() < 6) {
            erro = "A senha deve ter ao menos 6 caracteres.";
        } else if (!novo && emEdicao.getUsuarioId().equals(logado.getUsuarioId())
                && perfil != emEdicao.getPerfil()) {
            erro = "Você não pode alterar o seu próprio perfil de acesso.";
        }
        if (erro != null) {
            lblErro.setText(erro);
            return;
        }

        try {
            if (novo && dao.usuarioExiste(usuario)) {
                lblErro.setText("Já existe um usuário com este nome de usuário.");
                return;
            }
            if (dao.emailExiste(email, novo ? null : emEdicao.getUsuarioId())) {
                lblErro.setText("Este e-mail já está cadastrado.");
                return;
            }
            if (novo) {
                dao.inserir(usuario, nome, email, telefone, senha, perfil);
            } else {
                dao.atualizar(emEdicao.getUsuarioId(), nome, email, telefone, senha, perfil);
            }
        } catch (RuntimeException ex) {
            lblErro.setText(ex.getMessage());
            return;
        }
        cards.show(raiz, "lista");
        recarregar();
    }
}
