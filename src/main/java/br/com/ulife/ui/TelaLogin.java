package br.com.ulife.ui;

import br.com.ulife.dao.DatabaseConnection;
import br.com.ulife.dao.UsuarioDAO;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class TelaLogin extends JFrame {
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private JTextField txtEmail;
    private JPasswordField txtSenha;
    private JButton btnEntrar;
    private JLabel lblStatus;

    public TelaLogin() {
        initComponents();
        testDatabaseConnection();
    }

    private void initComponents() {
        setTitle("Ulife 2.0 - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 500);
        setLocationRelativeTo(null);
        setResizable(false);

        // Painel principal com layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        mainPanel.setBackground(Color.WHITE);

        // Logo / Título
        JLabel lblTitulo = new JLabel("Ulife 2.0");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTitulo.setForeground(new Color(0, 102, 204));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel("Plataforma Acadêmica Integrada");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitulo.setForeground(Color.GRAY);
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(lblTitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(lblSubtitulo);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 40)));

        // Campo Email
        JLabel lblEmail = new JLabel("E-mail");
        lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(lblEmail);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        txtEmail = new JTextField();
        txtEmail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        mainPanel.add(txtEmail);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Campo Senha
        JLabel lblSenha = new JLabel("Senha");
        lblSenha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSenha.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(lblSenha);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        txtSenha = new JPasswordField();
        txtSenha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtSenha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSenha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        mainPanel.add(txtSenha);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Link "Esqueci a senha"
        JLabel lblEsqueciSenha = new JLabel("<html><u>Esqueci a senha</u></html>");
        lblEsqueciSenha.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEsqueciSenha.setForeground(new Color(0, 102, 204));
        lblEsqueciSenha.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblEsqueciSenha.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblEsqueciSenha.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                JOptionPane.showMessageDialog(TelaLogin.this,
                        "Funcionalidade de recuperação de senha em desenvolvimento.",
                        "Esqueci a senha", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        mainPanel.add(lblEsqueciSenha);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Botão Entrar
        btnEntrar = new JButton("Entrar");
        btnEntrar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnEntrar.setForeground(Color.WHITE);
        btnEntrar.setBackground(new Color(0, 102, 204));
        btnEntrar.setFocusPainted(false);
        btnEntrar.setBorderPainted(false);
        btnEntrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnEntrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEntrar.addActionListener(this::btnEntrarActionPerformed);
        mainPanel.add(btnEntrar);

        // Enter key binding para o botão
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterPressed");
        getRootPane().getActionMap().put("enterPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnEntrar.doClick();
            }
        });

        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Status label
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(Color.RED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(lblStatus);

        add(mainPanel);
    }

    private void testDatabaseConnection() {
        new Thread(() -> {
            try {
                DatabaseConnection.getInstance().testConnection();
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Conectado ao banco de dados");
                    lblStatus.setForeground(new Color(0, 150, 0));
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Erro de conexão: " + e.getMessage());
                    lblStatus.setForeground(Color.RED);
                    btnEntrar.setEnabled(false);
                });
            }
        }).start();
    }

    private void btnEntrarActionPerformed(ActionEvent e) {
        String email = txtEmail.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (email.isEmpty() || senha.isEmpty()) {
            lblStatus.setText("Preencha e-mail e senha");
            lblStatus.setForeground(Color.RED);
            return;
        }

        btnEntrar.setEnabled(false);
        btnEntrar.setText("Entrando...");
        lblStatus.setText(" ");

        new Thread(() -> {
            try {
                if (usuarioDAO.verifyPassword(email, senha)) {
                    Usuario usuario = usuarioDAO.findByEmail(email).orElse(null);
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("Login realizado com sucesso!");
                        lblStatus.setForeground(new Color(0, 150, 0));
                        abrirPainelPrincipal(usuario);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("E-mail ou senha incorretos");
                        lblStatus.setForeground(Color.RED);
                        btnEntrar.setEnabled(true);
                        btnEntrar.setText("Entrar");
                        txtSenha.setText("");
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Erro: " + ex.getMessage());
                    lblStatus.setForeground(Color.RED);
                    btnEntrar.setEnabled(true);
                    btnEntrar.setText("Entrar");
                });
            }
        }).start();
    }

    private void abrirPainelPrincipal(Usuario usuario) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            new TelaPrincipal(usuario).setVisible(true);
        });
    }

    public static void main(String[] args) {
        // Look and Feel do sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new TelaLogin().setVisible(true);
        });
    }
}