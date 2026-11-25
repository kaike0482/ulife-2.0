package br.com.ulife.ui;

import br.com.ulife.model.Usuario;
import br.com.ulife.ui.Icone.Tipo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela principal: cabeçalho, barra lateral de ícones (rail), painel flutuante de navegação
 * e área de conteúdo com abas — mesma estrutura do protótipo HTML "Portal do Aluno".
 */
public class TelaPrincipal extends JFrame {

    private static final int LARG_RAIL = 64;
    private static final int LARG_FLY = 300;

    @FunctionalInterface
    private interface Pagina {
        JComponent criar();
    }

    private static final class Secao {
        final Tipo icone;
        final String titulo;
        final String desc;
        final String[] abas;
        final Pagina[] paginas;
        final int[] fly; // índices das abas exibidas no painel flutuante

        Secao(Tipo icone, String titulo, String desc, String[] abas, int[] fly, Pagina... paginas) {
            this.icone = icone;
            this.titulo = titulo;
            this.desc = desc;
            this.abas = abas;
            this.fly = fly;
            this.paginas = paginas;
        }
    }

    private final Usuario usuarioLogado;
    private final List<Secao> secoes = new ArrayList<>();

    private int secAtual = 0;
    private int abaAtual = 0;
    private int flySec = -1;
    private boolean flyAberto = false;

    private final List<BotaoIcone> botoesRail = new ArrayList<>();
    private JPanel paginaHost;
    private JPanel flyout;
    private JPanel backdrop;
    private JLayeredPane camadas;
    private JPanel base;

    public TelaPrincipal(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
        criarSecoes();
        initComponents();
        renderPagina();
        atualizarRail();
    }

    // ------------------------------------------------------------------ seções

    private void criarSecoes() {
        secoes.add(new Secao(Tipo.MON, "Aulas", "Tudo o que é necessário para sua rotina de estudos",
                new String[]{"Quadro de Aulas", "Aulas Gravadas", "Minhas UCs", "Atividades Avaliativas", "Notas/Faltas",
                        "Plano de Ensino", "Agendamento de Avaliações", "Bibliotecas Online", "Biblioteca Física"},
                new int[]{2, 0, 3, 4, 5, 6, 7, 8},
                Paginas::quadro, Paginas::gravadas, Paginas::ucs, Paginas::atividades, Paginas::notasFaltas,
                Paginas::planoEnsino, Paginas::agendamento, Paginas::bibliotecasOnline, Paginas::bibliotecaFisica));

        secoes.add(new Secao(Tipo.CAP, "Curso", "O caminho para concluir seu curso",
                new String[]{"Certificados", "Histórico Escolar", "Extensão", "Atividades Complementares", "Plano Curricular",
                        "Declaração de Regularidade Acadêmica", "Percentual de Hibridez"},
                new int[]{0, 1, 2, 3, 4, 5, 6},
                Paginas::certificados, Paginas::historico, Paginas::extensao, Paginas::atividadesComplementares,
                Paginas::planoCurricular, Paginas::declaracaoRegularidade, Paginas::percentualHibridez));

        secoes.add(new Secao(Tipo.BAG, "Academia de Futuros Profissionais",
                "(Estágios, Buscador de vagas, Simulador de entrevistas IA, e Programas NEST/HUB)",
                new String[]{"Portal Academia de Futuros Profissionais"}, new int[]{0},
                Paginas::portalAcademia));

        secoes.add(new Secao(Tipo.LIB, "Serviços", "Acesso aos processos e documentos acadêmicos",
                new String[]{"Rematrícula", "Atendimento", "Meus Contratos"}, new int[]{0, 1, 2},
                Paginas::rematricula, Paginas::atendimento, Paginas::meusContratos));

        secoes.add(new Secao(Tipo.MAIL, "Comunicação", "Encontre os avisos recebidos e envie mensagens",
                new String[]{"Conectados", "Avisos", "Mensagens", "Notificações"}, new int[]{0, 1, 2, 3},
                Paginas::conectados, Paginas::avisos, Paginas::mensagens, Paginas::notificacoes));

        secoes.add(new Secao(Tipo.FLAG, "Projetos ODS",
                "Cadastro, consulta, alteração e exclusão de projetos ligados aos ODS",
                new String[]{"Projetos"}, new int[]{0},
                PainelProjetos::new));

        secoes.add(new Secao(Tipo.USERS, "Usuários", "Gerenciamento dos usuários com acesso ao sistema",
                new String[]{"Usuários"}, new int[]{0},
                () -> new PainelUsuarios(usuarioLogado)));
    }

    // ------------------------------------------------------------------ montagem da janela

    private void initComponents() {
        setTitle("Portal do Aluno – Ulife 2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 800);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilo.BG);
        raiz.add(criarCabecalho(), BorderLayout.NORTH);

        // rail
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(Estilo.CARD);
        rail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Estilo.BD),
                BorderFactory.createEmptyBorder(18, 10, 0, 9)));
        rail.setPreferredSize(new Dimension(LARG_RAIL, 0));
        for (int i = 0; i < secoes.size(); i++) {
            final int idx = i;
            Secao s = secoes.get(i);
            BotaoIcone b = new BotaoIcone(s.icone, 24, true);
            b.setToolTipText(s.titulo);
            b.setMaximumSize(new Dimension(44, 44));
            b.setPreferredSize(new Dimension(44, 44));
            b.addActionListener(e -> cliqueRail(idx));
            botoesRail.add(b);
            rail.add(b);
            rail.add(Box.createVerticalStrut(8));
        }

        paginaHost = new JPanel(new BorderLayout());
        paginaHost.setBackground(Estilo.BG);
        paginaHost.setBorder(BorderFactory.createEmptyBorder(26, 36, 10, 36));

        base = new JPanel(new BorderLayout());
        base.add(rail, BorderLayout.WEST);
        base.add(paginaHost, BorderLayout.CENTER);

        // painel flutuante + fundo escurecido
        flyout = new JPanel();
        flyout.setLayout(new BoxLayout(flyout, BoxLayout.Y_AXIS));
        flyout.setBackground(Estilo.CARD);
        flyout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Estilo.BD),
                BorderFactory.createEmptyBorder(26, 24, 26, 24)));
        flyout.setVisible(false);

        backdrop = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(10, 20, 40, 38));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backdrop.setOpaque(false);
        backdrop.setVisible(false);
        backdrop.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                fecharFly();
            }
        });

        camadas = new JLayeredPane() {
            @Override
            public void doLayout() {
                int w = getWidth(), h = getHeight();
                base.setBounds(0, 0, w, h);
                backdrop.setBounds(LARG_RAIL, 0, Math.max(0, w - LARG_RAIL), h);
                flyout.setBounds(LARG_RAIL, 0, LARG_FLY, h);
            }
        };
        camadas.add(base, JLayeredPane.DEFAULT_LAYER);
        camadas.add(backdrop, JLayeredPane.PALETTE_LAYER);
        camadas.add(flyout, JLayeredPane.MODAL_LAYER);
        raiz.add(camadas, BorderLayout.CENTER);

        setContentPane(raiz);

        getRootPane().registerKeyboardAction(e -> fecharFly(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ------------------------------------------------------------------ cabeçalho

    private JComponent criarCabecalho() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(Color.WHITE);
        h.setPreferredSize(new Dimension(100, 64));
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Estilo.BD),
                BorderFactory.createEmptyBorder(0, 18, 0, 22)));

        // marca
        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 14));
        marca.setOpaque(false);
        JComponent logo = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Estilo.PD);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                new Icone(Tipo.CAP, 22, Color.WHITE).paintIcon(this, g2, 7, 7);
                g2.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(36, 36));
        JLabel nome = Estilo.rotulo("Portal do Aluno", Font.BOLD, 20, Estilo.P);
        marca.add(logo);
        marca.add(nome);
        marca.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        marca.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navegar(0, 0);
            }
        });
        h.add(marca, BorderLayout.WEST);

        // lado direito
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        dir.setOpaque(false);

        JButton campus = new JButton("IUSJ SÃO BERNARDO - São Bernardo do Campo - 430");
        campus.setIcon(new Icone(Tipo.PIN, 22, Estilo.AC));
        campus.setIconTextGap(10);
        campus.setHorizontalTextPosition(SwingConstants.RIGHT);
        campus.setFont(Estilo.fonte(Font.PLAIN, 13));
        campus.setForeground(Estilo.TX);
        campus.setContentAreaFilled(false);
        campus.setFocusPainted(false);
        campus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BD),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        campus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        campus.addActionListener(e -> Estilo.aviso(this, "Seletor de campus/organização (em desenvolvimento)."));
        dir.add(campus);

        BotaoIcone acc = new BotaoIcone(Tipo.ACC, 26, false);
        acc.setToolTipText("Acessibilidade");
        acc.addActionListener(e -> Estilo.aviso(this, "Recursos de acessibilidade (em desenvolvimento)."));
        dir.add(acc);

        BotaoIcone sino = new BotaoIcone(Tipo.BELL, 26, false);
        sino.setToolTipText("Notificações");
        sino.setBadge("3");
        JPopupMenu popNotif = criarPopupNotificacoes();
        sino.addActionListener(e -> popNotif.show(sino, sino.getWidth() - 320, sino.getHeight() + 4));
        dir.add(sino);

        BotaoIcone conta = new BotaoIcone(Tipo.USER, 26, false);
        conta.setToolTipText("Conta");
        JPopupMenu popConta = new JPopupMenu();
        JMenuItem itemOla = new JMenuItem("Olá, " + usuarioLogado.getNome());
        itemOla.setEnabled(false);
        itemOla.setFont(Estilo.fonte(Font.PLAIN, 13));
        JMenuItem tema = new JMenuItem("Alternar tema claro/escuro");
        tema.setFont(Estilo.fonte(Font.PLAIN, 14));
        tema.addActionListener(e -> Estilo.aviso(this, "Alternância de tema (em desenvolvimento)."));
        JMenuItem sair = new JMenuItem("Sair");
        sair.setFont(Estilo.fonte(Font.PLAIN, 14));
        sair.addActionListener(e -> sair());
        popConta.add(itemOla);
        popConta.addSeparator();
        popConta.add(tema);
        popConta.add(sair);
        conta.addActionListener(e -> popConta.show(conta, conta.getWidth() - 200, conta.getHeight() + 4));
        dir.add(conta);

        h.add(dir, BorderLayout.EAST);
        return h;
    }

    private JPopupMenu criarPopupNotificacoes() {
        JPopupMenu pop = new JPopupMenu();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(320, 215));
        JLabel nh = Estilo.rotulo("Notificações", Font.BOLD, 14, Estilo.TX);
        nh.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        nh.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(nh);
        String[][] itens = {
                {"Rematrícula aberta", "Secretaria · Prazo até 15/10"},
                {"Nova nota lançada", "Banco de Dados · Quiz 02"},
                {"Aula gravada disponível", "Modelagem de Software"}};
        for (String[] i : itens) {
            JPanel it = new JPanel(new GridLayout(2, 1));
            it.setBackground(Color.WHITE);
            it.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Estilo.ST),
                    BorderFactory.createEmptyBorder(10, 16, 10, 16)));
            it.add(Estilo.rotulo(i[0], Font.BOLD, 13, Estilo.TX));
            it.add(Estilo.rotulo(i[1], Font.PLAIN, 12, Estilo.MU));
            it.setAlignmentX(Component.LEFT_ALIGNMENT);
            it.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            p.add(it);
        }
        pop.add(p);
        return pop;
    }

    // ------------------------------------------------------------------ navegação

    private void cliqueRail(int idx) {
        if (flyAberto && flySec == idx) {
            fecharFly();
        } else {
            abrirFly(idx);
        }
    }

    private void abrirFly(int idx) {
        flySec = idx;
        flyAberto = true;
        Secao s = secoes.get(idx);

        flyout.removeAll();
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.setAlignmentX(Component.LEFT_ALIGNMENT);
        topo.add(Estilo.texto(s.titulo, 200, Font.BOLD, 18, Estilo.TX), BorderLayout.CENTER);
        BotaoIcone fechar = new BotaoIcone(Tipo.LEFT, 14, false);
        fechar.setPreferredSize(new Dimension(26, 26));
        fechar.addActionListener(e -> fecharFly());
        JPanel fc = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        fc.setOpaque(false);
        fc.add(fechar);
        topo.add(fc, BorderLayout.EAST);
        flyout.add(topo);
        flyout.add(Box.createVerticalStrut(6));

        JLabel d = Estilo.texto(s.desc, 240, Font.PLAIN, 13, Estilo.MU);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        flyout.add(d);
        flyout.add(Box.createVerticalStrut(16));
        JSeparator sep = new JSeparator();
        sep.setForeground(Estilo.BD);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        flyout.add(sep);
        flyout.add(Box.createVerticalStrut(16));

        for (int aba : s.fly) {
            final int abaIdx = aba;
            boolean atual = secAtual == idx && abaAtual == aba;
            JLabel l = new JLabel("<html><body style='width:230px'>" + Estilo.esc(s.abas[aba]) + "</body></html>");
            l.setFont(Estilo.fonte(atual ? Font.BOLD : Font.PLAIN, 14));
            l.setForeground(atual ? Estilo.PD : Estilo.AC);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navegar(idx, abaIdx);
                }
            });
            flyout.add(l);
            flyout.add(Box.createVerticalStrut(16));
        }
        flyout.add(Box.createVerticalGlue());

        flyout.setVisible(true);
        backdrop.setVisible(true);
        flyout.revalidate();
        flyout.repaint();
        atualizarRail();
    }

    private void fecharFly() {
        flyAberto = false;
        flyout.setVisible(false);
        backdrop.setVisible(false);
        atualizarRail();
    }

    private void navegar(int sec, int aba) {
        secAtual = sec;
        abaAtual = aba;
        fecharFly();
        renderPagina();
    }

    private void atualizarRail() {
        for (int i = 0; i < botoesRail.size(); i++) {
            botoesRail.get(i).setSelecionado((flyAberto && flySec == i) || (!flyAberto && secAtual == i));
        }
    }

    // ------------------------------------------------------------------ página

    private void renderPagina() {
        Secao s = secoes.get(secAtual);
        paginaHost.removeAll();

        JPanel cab = new JPanel();
        cab.setOpaque(false);
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        JLabel h1 = Estilo.rotulo(s.titulo, Font.BOLD, 26, Estilo.TX);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        cab.add(h1);
        cab.add(Box.createVerticalStrut(14));
        JComponent abas = criarAbas(s);
        abas.setAlignmentX(Component.LEFT_ALIGNMENT);
        cab.add(abas);
        cab.add(Box.createVerticalStrut(22));

        JComponent conteudo;
        try {
            conteudo = s.paginas[abaAtual].criar();
        } catch (RuntimeException ex) {
            conteudo = Estilo.rotulo("Erro ao carregar a página: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO);
        }
        JPanel wrap = new Estilo.PainelRolavel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(conteudo, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(18);

        paginaHost.add(cab, BorderLayout.NORTH);
        paginaHost.add(sp, BorderLayout.CENTER);
        paginaHost.revalidate();
        paginaHost.repaint();
    }

    private JComponent criarAbas(Secao s) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Estilo.BD));
        for (int i = 0; i < s.abas.length; i++) {
            final int idx = i;
            AbaBotao b = new AbaBotao(s.abas[i], i == abaAtual);
            b.addActionListener(e -> {
                abaAtual = idx;
                renderPagina();
            });
            bar.add(b);
        }
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, bar.getPreferredSize().height));
        return bar;
    }

    private void sair() {
        int r = JOptionPane.showConfirmDialog(this, "Deseja realmente sair da sua conta?",
                "Sair", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
        }
    }

    // ------------------------------------------------------------------ componentes

    /** Botão quadrado com ícone (rail e cabeçalho). */
    private static final class BotaoIcone extends JButton {
        private final Tipo tipo;
        private final int tamIcone;
        private final boolean comFundo;
        private boolean selecionado;
        private String badge;

        BotaoIcone(Tipo tipo, int tamIcone, boolean comFundo) {
            this.tipo = tipo;
            this.tamIcone = tamIcone;
            this.comFundo = comFundo;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(tamIcone + 14, tamIcone + 14));
        }

        void setSelecionado(boolean s) {
            this.selecionado = s;
            repaint();
        }

        void setBadge(String b) {
            this.badge = b;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover();
            if (comFundo && (selecionado || hover)) {
                g2.setColor(Estilo.PL);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                if (selecionado) {
                    g2.setColor(Estilo.AC);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));
                }
            }
            Color cor = (comFundo && (selecionado || hover)) ? Estilo.AC : (comFundo ? Estilo.MU : Estilo.TX);
            new Icone(tipo, tamIcone, cor).paintIcon(this, g2,
                    (getWidth() - tamIcone) / 2, (getHeight() - tamIcone) / 2);
            if (badge != null) {
                g2.setFont(Estilo.fonte(Font.PLAIN, 11));
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(badge) + 12;
                int x = getWidth() - w + 2, y = 0;
                g2.setColor(new Color(0xd83b3b));
                g2.fillRoundRect(x, y, w, 16, 16, 16);
                g2.setColor(Color.WHITE);
                g2.drawString(badge, x + 6, y + 12);
            }
            g2.dispose();
        }
    }

    /** Aba da página: texto com sublinhado na aba ativa. */
    private static final class AbaBotao extends JButton {
        private final boolean ativa;

        AbaBotao(String texto, boolean ativa) {
            super(texto);
            this.ativa = ativa;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Estilo.fonte(ativa ? Font.BOLD : Font.PLAIN, 14));
            setForeground(ativa ? Estilo.AC : Estilo.MU);
            setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (ativa) {
                g.setColor(Estilo.AC);
                g.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Usuario teste = new Usuario();
            teste.setUsuarioId(0);
            teste.setNome("Usuário de Teste");
            teste.setPerfil(Usuario.Perfil.ALUNO);
            new TelaPrincipal(teste).setVisible(true);
        });
    }
}
