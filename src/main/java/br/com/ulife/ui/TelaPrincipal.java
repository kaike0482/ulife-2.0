package br.com.ulife.ui;

import br.com.ulife.model.Usuario;

import javax.swing.*;
import java.awt.*;

/**
 * Tela principal (dashboard) da plataforma Ulife 2.0.
 * <p>
 * Apresenta um menu lateral com as funcionalidades disponíveis conforme o
 * perfil do usuário logado e um painel central onde cada opção é renderizada.
 * Funcionalidades ainda não conectadas a DAOs exibem um aviso "Em desenvolvimento".
 */
public class TelaPrincipal extends JFrame {

    private static final Color ACCENT = new Color(0, 102, 204);
    private static final Color SIDEBAR = new Color(245, 246, 248);
    private static final Color TEXT_DARK = new Color(40, 40, 40);

    private final Usuario usuarioLogado;
    private JPanel painelCentral;

    public TelaPrincipal(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
        initComponents();
    }

    private void initComponents() {
        setTitle("Ulife 2.0 - Painel Principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel raiz = new JPanel(new BorderLayout(0, 0));
        raiz.setBackground(Color.WHITE);

        // Cabeçalho com boas-vindas e perfil
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(ACCENT);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel lblBoasVindas = new JLabel("Bem-vindo, " + usuarioLogado.getNome());
        lblBoasVindas.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBoasVindas.setForeground(Color.WHITE);

        JLabel lblPerfil = new JLabel("Perfil: " + usuarioLogado.getPerfil());
        lblPerfil.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPerfil.setForeground(new Color(200, 220, 240));

        JPanel info = new JPanel(new BorderLayout());
        info.setOpaque(false);
        info.add(lblBoasVindas, BorderLayout.NORTH);
        info.add(lblPerfil, BorderLayout.SOUTH);
        cabecalho.add(info, BorderLayout.WEST);

        JButton btnSair = new JButton("Sair");
        btnSair.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSair.setBackground(new Color(220, 53, 69));
        btnSair.setForeground(Color.WHITE);
        btnSair.setFocusPainted(false);
        btnSair.setBorderPainted(false);
        btnSair.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSair.addActionListener(e -> sair());
        cabecalho.add(btnSair, BorderLayout.EAST);

        // Sidebar com menu de funcionalidades por perfil
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

        JLabel lblMenu = new JLabel("  Menu");
        lblMenu.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblMenu.setForeground(TEXT_DARK);
        lblMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblMenu);
        sidebar.add(Box.createRigidArea(new Dimension(0, 12)));

        for (String opcao : opcoesDoPerfil()) {
            JButton btn = criarBotaoMenu(opcao);
            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        sidebar.add(Box.createVerticalGlue());

        // Painel central com conteúdo
        painelCentral = new JPanel(new BorderLayout());
        painelCentral.setBackground(Color.WHITE);
        painelCentral.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        mostrarInicio();

        raiz.add(cabecalho, BorderLayout.NORTH);
        raiz.add(sidebar, BorderLayout.WEST);
        raiz.add(painelCentral, BorderLayout.CENTER);

        add(raiz);
    }

    private JButton criarBotaoMenu(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_DARK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, Color.WHITE));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> abrirOpcao(texto));
        return btn;
    }

    private String[] opcoesDoPerfil() {
        Usuario.Perfil perfil = usuarioLogado.getPerfil();
        switch (perfil) {
            case ALUNO:
                return new String[] {
                        "Início",
                        "Minhas Turmas",
                        "Tarefas",
                        "Minhas Entregas",
                        "Frequência",
                        "Minhas Notas",
                        "Cobranças e Pagamentos",
                        "Abrir Chamado"
                };
            case PROFESSOR:
                return new String[] {
                        "Início",
                        "Minhas Turmas",
                        "Criar Tarefa",
                        "Entregas dos Alunos",
                        "Lançar Notas",
                        "Lançar Frequência"
                };
            case COORDENADOR:
            case SECRETARIA:
            case TESOURARIA:
            default:
                return new String[] {
                        "Início",
                        "Gerenciar Usuários",
                        "Gerenciar Turmas",
                        "Cobranças",
                        "Pagamentos",
                        "Chamados"
                };
        }
    }

    private void abrirOpcao(String opcao) {
        switch (opcao) {
            case "Início" -> mostrarInicio();
            case "Minhas Turmas" -> mostrarMensagem("Minhas Turmas", "Em desenvolvimento.");
            case "Tarefas" -> mostrarMensagem("Tarefas", "Em desenvolvimento.");
            case "Minhas Entregas" -> mostrarMensagem("Minhas Entregas", "Em desenvolvimento.");
            case "Frequência" -> mostrarMensagem("Frequência", "Em desenvolvimento.");
            case "Minhas Notas" -> mostrarMensagem("Minhas Notas", "Em desenvolvimento.");
            case "Cobranças e Pagamentos" -> mostrarMensagem("Cobranças e Pagamentos", "Em desenvolvimento.");
            case "Abrir Chamado" -> mostrarMensagem("Abrir Chamado", "Em desenvolvimento.");
            case "Criar Tarefa" -> mostrarMensagem("Criar Tarefa", "Em desenvolvimento.");
            case "Entregas dos Alunos" -> mostrarMensagem("Entregas dos Alunos", "Em desenvolvimento.");
            case "Lançar Notas" -> mostrarMensagem("Lançar Notas", "Em desenvolvimento.");
            case "Lançar Frequência" -> mostrarMensagem("Lançar Frequência", "Em desenvolvimento.");
            case "Gerenciar Usuários" -> mostrarMensagem("Gerenciar Usuários", "Em desenvolvimento.");
            case "Gerenciar Turmas" -> mostrarMensagem("Gerenciar Turmas", "Em desenvolvimento.");
            case "Cobranças" -> mostrarMensagem("Cobranças", "Em desenvolvimento.");
            case "Pagamentos" -> mostrarMensagem("Pagamentos", "Em desenvolvimento.");
            case "Chamados" -> mostrarMensagem("Chamados", "Em desenvolvimento.");
            default -> mostrarMensagem(opcao, "Opção não implementada.");
        }
    }

    private void mostrarInicio() {
        painelCentral.removeAll();
        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setOpaque(false);

        JLabel titulo = new JLabel("Painel Principal");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setForeground(TEXT_DARK);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Plataforma Acadêmica Integrada - Ulife 2.0");
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitulo.setForeground(Color.GRAY);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel perfil = new JLabel("Usuário logado: " + usuarioLogado.getNome()
                + "  |  Perfil: " + usuarioLogado.getPerfil());
        perfil.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        perfil.setForeground(TEXT_DARK);
        perfil.setAlignmentX(Component.LEFT_ALIGNMENT);

        conteudo.add(titulo);
        conteudo.add(Box.createRigidArea(new Dimension(0, 6)));
        conteudo.add(subtitulo);
        conteudo.add(Box.createRigidArea(new Dimension(0, 20)));
        conteudo.add(perfil);
        conteudo.add(Box.createVerticalGlue());

        painelCentral.add(conteudo, BorderLayout.NORTH);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void mostrarMensagem(String titulo, String mensagem) {
        painelCentral.removeAll();
        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setOpaque(false);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitulo.setForeground(TEXT_DARK);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblMsg = new JLabel(mensagem);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblMsg.setForeground(Color.GRAY);
        lblMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

        conteudo.add(lblTitulo);
        conteudo.add(Box.createRigidArea(new Dimension(0, 8)));
        conteudo.add(lblMsg);
        conteudo.add(Box.createVerticalGlue());

        painelCentral.add(conteudo, BorderLayout.NORTH);
        painelCentral.revalidate();
        painelCentral.repaint();
    }

    private void sair() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Deseja realmente sair da sua conta?",
                "Sair", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
        }
    }

    public static void main(String[] args) {
        // Permite executar a tela diretamente para testes de layout
        SwingUtilities.invokeLater(() -> {
            Usuario teste = new Usuario();
            teste.setNome("Usuário de Teste");
            teste.setPerfil(Usuario.Perfil.ALUNO);
            new TelaPrincipal(teste).setVisible(true);
        });
    }
}
