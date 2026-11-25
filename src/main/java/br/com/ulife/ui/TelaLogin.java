package br.com.ulife.ui;

import br.com.ulife.dao.DatabaseConnection;
import br.com.ulife.dao.SchemaInitializer;
import br.com.ulife.dao.UsuarioDAO;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;

/** Tela de login (controle de acesso). Visual igual ao protótipo HTML "Portal do Aluno". */
public class TelaLogin extends JFrame {

    private static final Color BRANCO_SUAVE = new Color(0xeaf1fb);

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final Preferences prefs = Preferences.userNodeForPackage(TelaLogin.class);

    private CampoTexto txtUsuario;
    private CampoSenha txtSenha;
    private JCheckBox chkLembrar;
    private Estilo.Botao btnEntrar;
    private JLabel lblErro;
    private JLabel lblStatus;

    public TelaLogin() {
        initComponents();
        prepararBanco();
    }

    // ------------------------------------------------------------------ campos com placeholder

    private static void desenharHint(Graphics g, JTextComponent c, String hint) {
        if (c.getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setFont(c.getFont());
            Insets in = c.getInsets();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, in.left, in.top + fm.getAscent() + (c.getHeight() - in.top - in.bottom - fm.getHeight()) / 2);
            g2.dispose();
        }
    }

    private static class CampoTexto extends JTextField {
        private final String hint;
        CampoTexto(String hint) { this.hint = hint; }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            desenharHint(g, this, hint);
        }
    }

    private static class CampoSenha extends JPasswordField {
        private final String hint;
        CampoSenha(String hint) { this.hint = hint; }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0) {
                desenharHint(g, this, hint);
            }
        }
    }

    private static void estilizarCampo(JTextComponent c) {
        c.setOpaque(false);
        c.setForeground(Color.WHITE);
        c.setCaretColor(Color.WHITE);
        c.setFont(Estilo.fonte(Font.PLAIN, 17));
        c.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
    }

    private JComponent linhaCampo(Icone.Tipo tipo, JTextComponent campo) {
        estilizarCampo(campo);
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(255, 255, 255, 165)),
                BorderFactory.createEmptyBorder(0, 0, 8, 0)));
        p.add(new JLabel(new Icone(tipo, 20, BRANCO_SUAVE)), BorderLayout.WEST);
        p.add(campo, BorderLayout.CENTER);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return p;
    }

    // ------------------------------------------------------------------ interface

    private void initComponents() {
        setTitle("Portal do Aluno – Ulife 2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 740);
        setMinimumSize(new Dimension(700, 640));
        setLocationRelativeTo(null);

        JPanel fundo = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                float raio = (float) Math.max(w, h);
                g2.setPaint(new RadialGradientPaint(new Point(Math.round(w * 0.3f), Math.round(h * 0.2f)), raio,
                        new float[]{0f, 0.55f, 1f},
                        new Color[]{new Color(0x4a80c9), new Color(0x1c4f8c), new Color(0x0d2c54)}));
                g2.fillRect(0, 0, w, h);
                g2.setPaint(new RadialGradientPaint(new Point(Math.round(w * 0.6f), Math.round(h * 0.3f)), raio * 0.45f,
                        new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 55), new Color(255, 255, 255, 0)}));
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(460, 600));

        // círculo com ícone de usuário
        JComponent circulo = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(13, 44, 84, 140));
                g2.fillOval(1, 1, getWidth() - 3, getHeight() - 3);
                g2.setColor(new Color(255, 255, 255, 128));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
                new Icone(Icone.Tipo.USER, 50, Color.WHITE).paintIcon(this, g2, (getWidth() - 50) / 2, (getHeight() - 50) / 2);
                g2.dispose();
            }
        };
        circulo.setPreferredSize(new Dimension(100, 100));
        circulo.setMaximumSize(new Dimension(100, 100));
        circulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(circulo);
        col.add(Box.createVerticalStrut(25));

        JLabel titulo = new JLabel("PORTAL DO ALUNO");
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, 0.2f);
        titulo.setFont(Estilo.fonte(Font.PLAIN, 26).deriveFont(attrs));
        titulo.setForeground(BRANCO_SUAVE);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(titulo);
        col.add(Box.createVerticalStrut(45));

        txtUsuario = new CampoTexto("Usuário");
        txtSenha = new CampoSenha("Senha");
        col.add(linhaCampo(Icone.Tipo.MAIL, txtUsuario));
        col.add(Box.createVerticalStrut(28));
        col.add(linhaCampo(Icone.Tipo.BOOK, txtSenha));
        col.add(Box.createVerticalStrut(10));

        lblErro = Estilo.rotulo(" ", Font.PLAIN, 13, new Color(0xffd7d7));
        lblErro.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel linhaErro = new JPanel(new BorderLayout());
        linhaErro.setOpaque(false);
        linhaErro.add(lblErro, BorderLayout.WEST);
        linhaErro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        col.add(linhaErro);
        col.add(Box.createVerticalStrut(8));

        // lembrar de mim + esqueci a senha
        JPanel opcoes = new JPanel(new BorderLayout());
        opcoes.setOpaque(false);
        chkLembrar = new JCheckBox("Lembrar de mim");
        chkLembrar.setOpaque(false);
        chkLembrar.setForeground(BRANCO_SUAVE);
        chkLembrar.setFont(Estilo.fonte(Font.PLAIN, 14));
        chkLembrar.setFocusPainted(false);
        String lembrado = prefs.get("usuario", "");
        chkLembrar.setSelected(true);
        txtUsuario.setText(lembrado);
        JLabel esqueci = new JLabel("<html><i>Esqueci a senha</i></html>");
        esqueci.setFont(Estilo.fonte(Font.PLAIN, 14));
        esqueci.setForeground(BRANCO_SUAVE);
        esqueci.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        esqueci.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Estilo.aviso(TelaLogin.this, "Recuperação de senha: procure a secretaria ou um administrador do sistema.");
            }
        });
        opcoes.add(chkLembrar, BorderLayout.WEST);
        opcoes.add(esqueci, BorderLayout.EAST);
        opcoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        col.add(opcoes);
        col.add(Box.createVerticalStrut(30));

        btnEntrar = new Estilo.Botao("ENTRAR", new Color(10, 34, 66, 190), new Color(10, 34, 66, 240),
                Color.WHITE, 15, 0, 16);
        Map<TextAttribute, Object> a2 = new HashMap<>();
        a2.put(TextAttribute.TRACKING, 0.18f);
        a2.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_SEMIBOLD);
        btnEntrar.setFont(Estilo.fonte(Font.BOLD, 16).deriveFont(a2));
        btnEntrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnEntrar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEntrar.setEnabled(false);
        btnEntrar.addActionListener(e -> entrar());
        col.add(btnEntrar);
        col.add(Box.createVerticalStrut(22));

        lblStatus = Estilo.rotulo("Conectando ao banco de dados...", Font.PLAIN, 12, new Color(255, 255, 255, 170));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(lblStatus);

        fundo.add(col);
        setContentPane(fundo);
        getRootPane().setDefaultButton(btnEntrar);
    }

    // ------------------------------------------------------------------ banco

    /** Garante o esquema (colunas/tabelas do A3) e confirma a conexão antes de liberar o login. */
    private void prepararBanco() {
        new Thread(() -> {
            try {
                DatabaseConnection.getInstance().testConnection();
                SchemaInitializer.garantir();
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Conectado ao banco de dados");
                    btnEntrar.setEnabled(true);
                    (txtUsuario.getText().isEmpty() ? txtUsuario : txtSenha).requestFocusInWindow();
                });
            } catch (Exception e) {
                Throwable causa = e.getCause() != null ? e.getCause() : e;
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("<html><div style='text-align:center;width:420px'>Erro de conexão: "
                            + Estilo.esc(causa.getMessage()) + "<br>Verifique se o MySQL (Docker) está em execução "
                            + "e o arquivo database.properties.</div></html>");
                    lblStatus.setForeground(new Color(0xffd7d7));
                });
            }
        }, "preparar-banco").start();
    }

    private void entrar() {
        String login = txtUsuario.getText().trim();
        String senha = new String(txtSenha.getPassword());
        if (login.isEmpty() || senha.isEmpty()) {
            lblErro.setText("Preencha usuário e senha para continuar.");
            return;
        }
        lblErro.setText(" ");
        btnEntrar.setEnabled(false);
        btnEntrar.setText("ENTRANDO...");

        new Thread(() -> {
            try {
                Optional<Usuario> u = usuarioDAO.autenticar(login, senha);
                SwingUtilities.invokeLater(() -> {
                    if (u.isPresent()) {
                        if (chkLembrar.isSelected()) {
                            prefs.put("usuario", login);
                        } else {
                            prefs.remove("usuario");
                        }
                        dispose();
                        new TelaPrincipal(u.get()).setVisible(true);
                    } else {
                        lblErro.setText("Usuário ou senha inválidos.");
                        txtSenha.setText("");
                        restaurarBotao();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblErro.setText("Erro: " + ex.getMessage());
                    restaurarBotao();
                });
            }
        }, "login").start();
    }

    private void restaurarBotao() {
        btnEntrar.setEnabled(true);
        btnEntrar.setText("ENTRAR");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // usa o padrão
        }
        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}
