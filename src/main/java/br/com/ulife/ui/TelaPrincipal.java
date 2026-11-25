package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.CobrancaDAO;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.Turma;
import br.com.ulife.service.ResumoTurma;
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
    private PainelAnimado paginaHost;
    private JPanel railPanel;
    private float flyProg = 0f;
    private javax.swing.Timer animFly;
    private JPanel flyout;
    private JPanel backdrop;
    private JLayeredPane camadas;
    private JPanel base;
    private final CobrancaDAO cobrancaDAO = new CobrancaDAO();
    private int secPagamentos = -1;
    private int secNotas = 0;
    private int secFrequencia = 0;
    private int pendentes = 0;
    private Estilo.Botao pillPagamentos;
    private int secGestaoPag = -1;
    private final AcademicoDAO academicoDAO = new AcademicoDAO();

    public TelaPrincipal(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
        criarSecoes();
        atualizarPendencias();
        initComponents();
        renderPagina();
        atualizarRail();
    }

    // ------------------------------------------------------------------ seções

    /** Monta o menu conforme o perfil: aluno, professor ou coordenador. */
    private void criarSecoes() {
        Secao aulas = new Secao(Tipo.MON, "Aulas", "Tudo o que é necessário para sua rotina de estudos",
                new String[]{"Quadro de Aulas", "Aulas Gravadas", "Minhas UCs", "Atividades Avaliativas", "Notas/Faltas",
                        "Plano de Ensino", "Agendamento de Avaliações", "Bibliotecas Online", "Biblioteca Física"},
                new int[]{2, 0, 3, 4, 5, 6, 7, 8},
                () -> PaginasAluno.quadro(usuarioLogado), () -> PaginasAluno.gravadas(usuarioLogado),
                () -> PaginasAluno.ucs(usuarioLogado), () -> PaginasAluno.atividades(usuarioLogado),
                () -> new PainelNotasFaltas(usuarioLogado), () -> PaginasAluno.plano(usuarioLogado),
                () -> PaginasAluno.agendamento(usuarioLogado), () -> PaginasAluno.biblioteca(usuarioLogado, true),
                () -> PaginasAluno.biblioteca(usuarioLogado, false));

        Secao pagamentos = new Secao(Tipo.CARD, "Pagamentos",
                "Mensalidades, pagamento por PIX ou boleto e histórico de pagamentos",
                new String[]{"Pagamentos"}, new int[]{0},
                () -> new PainelPagamentos(usuarioLogado, this::atualizarPendencias));

        Secao curso = new Secao(Tipo.CAP, "Curso", "O caminho para concluir seu curso",
                new String[]{"Certificados", "Histórico Escolar", "Extensão", "Atividades Complementares", "Plano Curricular",
                        "Declaração de Regularidade Acadêmica", "Percentual de Hibridez"},
                new int[]{0, 1, 2, 3, 4, 5, 6},
                () -> PaginasAluno.certificados(usuarioLogado), () -> PaginasAluno.historico(usuarioLogado),
                () -> PaginasAluno.extensao(usuarioLogado), () -> PaginasAluno.atividadesComplementares(usuarioLogado),
                () -> PaginasAluno.planoCurricular(usuarioLogado), () -> PaginasAluno.declaracao(usuarioLogado),
                () -> PaginasAluno.hibridez(usuarioLogado));

        Secao academia = new Secao(Tipo.BAG, "Academia de Futuros Profissionais",
                "(Estágios, Buscador de vagas, Simulador de entrevistas IA, e Programas NEST/HUB)",
                new String[]{"Portal Academia de Futuros Profissionais"}, new int[]{0},
                Paginas::portalAcademia);

        Secao servicos = new Secao(Tipo.LIB, "Serviços", "Acesso aos processos e documentos acadêmicos",
                new String[]{"Rematrícula", "Atendimento", "Meus Contratos"}, new int[]{0, 1, 2},
                Paginas::rematricula, Paginas::atendimento, Paginas::meusContratos);

        Secao comunicacao = new Secao(Tipo.MAIL, "Comunicação", "Encontre os avisos recebidos e envie mensagens",
                new String[]{"Conectados", "Avisos", "Mensagens", "Notificações"}, new int[]{0, 1, 2, 3},
                Paginas::conectados, Paginas::avisos, Paginas::mensagens, Paginas::notificacoes);

        Secao projetos = new Secao(Tipo.FLAG, "Projetos ODS",
                "Cadastro, consulta, alteração e exclusão de projetos ligados aos ODS",
                new String[]{"Projetos"}, new int[]{0},
                PainelProjetos::new);

        Secao usuarios = new Secao(Tipo.USERS, "Usuários",
                "Gerenciamento dos usuários e dos perfis de acesso ao sistema",
                new String[]{"Usuários"}, new int[]{0},
                () -> new PainelUsuarios(usuarioLogado));

        // ---- professor
        Secao profInicio = new Secao(Tipo.HOME, "Início", "Resumo das suas turmas e pendências",
                new String[]{"Visão geral"}, new int[]{0},
                () -> new PainelProfInicio(usuarioLogado, () -> navegar(secFrequencia, 0), () -> navegar(secNotas, 0)));
        Secao profAulas = new Secao(Tipo.MON, "Aulas", "Seu quadro semanal de aulas",
                new String[]{"Quadro de Aulas"}, new int[]{0},
                () -> new QuadroAulasProfessor(usuarioLogado, () -> navegar(secFrequencia, 0)));
        Secao profTurmas = new Secao(Tipo.USERS, "Minhas turmas", "Suas turmas, alunos, frequência e médias",
                new String[]{"Turmas e alunos"}, new int[]{0},
                () -> new PainelProfTurmas(usuarioLogado));
        Secao profNotas = new Secao(Tipo.STAR, "Lançar notas", "Notas A1, A2 e A3 por turma, com média automática",
                new String[]{"Notas A1–A3"}, new int[]{0},
                () -> new PainelNotas(usuarioLogado));
        Secao profFreq = new Secao(Tipo.CAL, "Lançar frequência",
                "Presença das aulas, com sugestão automática pela videochamada",
                new String[]{"Frequência das aulas"}, new int[]{0},
                () -> new PainelFrequencia(usuarioLogado));

        // ---- coordenador
        Secao coordInicio = new Secao(Tipo.HOME, "Início", "Indicadores gerais do curso",
                new String[]{"Indicadores"}, new int[]{0},
                () -> new PainelCoordInicio(usuarioLogado));
        Secao coordPagamentos = new Secao(Tipo.CARD, "Pagamentos",
                "Cobranças, inadimplência e pagamentos recebidos dos alunos",
                new String[]{"Pagamentos"}, new int[]{0},
                PainelGestaoPagamentos::new);
        Secao coordValidacoes = new Secao(Tipo.DOC, "Validações",
                "Atividades complementares dos alunos aguardando validação",
                new String[]{"Validações"}, new int[]{0},
                PainelValidacoes::new);
        Secao coordTurmas = new Secao(Tipo.CAP, "Turmas", "Cadastro de turmas, professores e matrículas",
                new String[]{"Gestão de turmas"}, new int[]{0},
                PainelTurmas::new);

        Usuario.Perfil perfil = usuarioLogado.getPerfil();
        if (perfil == Usuario.Perfil.ALUNO) {
            secoes.add(aulas);
            secoes.add(pagamentos);
            secoes.add(curso);
            secoes.add(academia);
            secoes.add(servicos);
            secoes.add(comunicacao);
        } else if (perfil == Usuario.Perfil.COORDENADOR) { // menu enxuto: só gestão
            secoes.add(coordInicio);
            secoes.add(coordTurmas);
            secoes.add(coordPagamentos);
            secoes.add(coordValidacoes);
            secoes.add(usuarios);
            secoes.add(projetos);
            secGestaoPag = secoes.indexOf(coordPagamentos);
        } else { // professor (e perfis administrativos)
            secoes.add(profInicio);
            secoes.add(profAulas);
            secoes.add(profTurmas);
            secoes.add(profNotas);
            secoes.add(profFreq);
            secNotas = secoes.indexOf(profNotas);
            secFrequencia = secoes.indexOf(profFreq);
        }
        secPagamentos = secoes.indexOf(pagamentos);
    }

    // ------------------------------------------------------------------ montagem da janela

    private void initComponents() {
        setTitle(nomePortal() + " – Ulife 2.0");
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

        paginaHost = new PainelAnimado();
        paginaHost.setBackground(Estilo.BG);
        paginaHost.setBorder(BorderFactory.createEmptyBorder(26, 36, 10, 36));

        railPanel = rail;
        JPanel espaco = new JPanel();
        espaco.setOpaque(false);
        espaco.setPreferredSize(new Dimension(LARG_RAIL, 0));
        base = new JPanel(new BorderLayout());
        base.add(espaco, BorderLayout.WEST);
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
                g.setColor(new Color(10, 20, 40, Math.round(38 * flyProg)));
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
                float e = 1f - (1f - flyProg) * (1f - flyProg) * (1f - flyProg);
                int x = LARG_RAIL - Math.round((1f - e) * LARG_FLY);
                railPanel.setBounds(0, 0, LARG_RAIL, h);
                backdrop.setBounds(LARG_RAIL, 0, Math.max(0, w - LARG_RAIL), h);
                flyout.setBounds(x, 0, LARG_FLY, h);
            }
        };
        camadas.add(base, JLayeredPane.DEFAULT_LAYER);
        camadas.add(backdrop, JLayeredPane.PALETTE_LAYER);
        camadas.add(flyout, JLayeredPane.MODAL_LAYER);
        camadas.add(railPanel, JLayeredPane.DRAG_LAYER);
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
        JLabel nome = Estilo.rotulo(nomePortal(), Font.BOLD, 20, Estilo.P);
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

        int alvo = secaoDoAtalho();
        if (alvo >= 0) {
            boolean prof = usuarioLogado.getPerfil() == Usuario.Perfil.PROFESSOR;
            pillPagamentos = new Estilo.Botao("Pagamentos", new Color(0xe8862a), new Color(0xcf741f),
                    Color.WHITE, 9, 18, 14);
            pillPagamentos.setFont(Estilo.fonte(Font.BOLD, 14));
            pillPagamentos.setIcon(new Icone(prof ? Tipo.CAL : Tipo.CARD, 20, Color.WHITE));
            pillPagamentos.setIconTextGap(8);
            pillPagamentos.setToolTipText(dicaDoAtalho());
            pillPagamentos.addActionListener(e -> navegar(alvo, 0));
            dir.add(pillPagamentos);
            atualizarPendencias();
        }

        JButton campus = new JButton("IUSJ SÃO BERNARDO - 430");
        campus.setIcon(new Icone(Tipo.PIN, 22, Estilo.AC));
        campus.setIconTextGap(10);
        campus.setToolTipText("IUSJ SÃO BERNARDO - São Bernardo do Campo - 430");
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
        List<String[]> notifs = notificacoes();
        sino.setBadge(notifs.isEmpty() ? null : String.valueOf(notifs.size()));
        JPopupMenu popNotif = criarPopupNotificacoes(notifs);
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
        JMenuItem itemPerfil = new JMenuItem("Perfil: " + PainelUsuarios.rotuloPerfil(usuarioLogado.getPerfil()));
        itemPerfil.setEnabled(false);
        itemPerfil.setFont(Estilo.fonte(Font.PLAIN, 13));
        popConta.add(itemOla);
        popConta.add(itemPerfil);
        popConta.addSeparator();
        popConta.add(tema);
        popConta.add(sair);
        conta.addActionListener(e -> popConta.show(conta, conta.getWidth() - 200, conta.getHeight() + 4));
        dir.add(conta);

        h.add(dir, BorderLayout.EAST);
        return h;
    }

    private JPopupMenu criarPopupNotificacoes(List<String[]> itens) {
        JPopupMenu pop = new JPopupMenu();
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(320, 52 + 56 * Math.max(1, itens.size())));
        JLabel nh = Estilo.rotulo("Notificações", Font.BOLD, 14, Estilo.TX);
        nh.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        nh.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(nh);
        if (itens.isEmpty()) {
            itens = new ArrayList<>();
            itens.add(new String[]{"Tudo em dia", "Nenhuma notificação no momento"});
        }
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
        topo.add(Estilo.texto(s.titulo, 150, Font.BOLD, 18, Estilo.TX), BorderLayout.CENTER);
        BotaoIcone fechar = new BotaoIcone(Tipo.LEFT, 14, false);
        fechar.setPreferredSize(new Dimension(26, 26));
        fechar.addActionListener(e -> fecharFly());
        JPanel fc = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        fc.setOpaque(false);
        fc.add(fechar);
        topo.add(fc, BorderLayout.EAST);
        topo.setMaximumSize(new Dimension(Integer.MAX_VALUE, topo.getPreferredSize().height));
        flyout.add(topo);
        flyout.add(Box.createVerticalStrut(6));

        JLabel d = Estilo.texto(s.desc, 175, Font.PLAIN, 13, Estilo.MU);
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
            JLabel l = new JLabel("<html><body style='width:170px'>" + Estilo.esc(s.abas[aba]) + "</body></html>");
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

        flyout.revalidate();
        animarFly(true);
        atualizarRail();
    }

    private void fecharFly() {
        flyAberto = false;
        atualizarRail();
        if (flyProg > 0f || flyout.isVisible()) {
            animarFly(false);
        }
    }

    /** Desliza o painel lateral para dentro/fora (e escurece o fundo) em cerca de 150 ms. */
    private void animarFly(boolean abrir) {
        if (animFly != null) {
            animFly.stop();
        }
        if (abrir) {
            flyout.setVisible(true);
            backdrop.setVisible(true);
        }
        final float alvo = abrir ? 1f : 0f;
        animFly = new javax.swing.Timer(15, null);
        animFly.addActionListener(e -> {
            float passo = 0.16f;
            flyProg = flyProg < alvo ? Math.min(alvo, flyProg + passo) : Math.max(alvo, flyProg - passo);
            camadas.doLayout();
            camadas.repaint();
            if (flyProg == alvo) {
                animFly.stop();
                if (!abrir) {
                    flyout.setVisible(false);
                    backdrop.setVisible(false);
                }
            }
        });
        animFly.start();
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
        java.awt.image.BufferedImage fotoAntiga = paginaHost.capturar();
        paginaHost.removeAll();

        JPanel cab = new JPanel();
        cab.setOpaque(false);
        cab.setLayout(new BoxLayout(cab, BoxLayout.Y_AXIS));
        JComponent banner = criarBannerPendencias();
        if (banner != null) {
            banner.setAlignmentX(Component.LEFT_ALIGNMENT);
            cab.add(banner);
            cab.add(Box.createVerticalStrut(14));
        }
        JLabel h1 = Estilo.rotulo(s.titulo, Font.BOLD, 26, Estilo.TX);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        cab.add(h1);
        if (s.abas.length > 1) {
            cab.add(Box.createVerticalStrut(14));
            JComponent abas = criarAbas(s);
            abas.setAlignmentX(Component.LEFT_ALIGNMENT);
            cab.add(abas);
            cab.add(Box.createVerticalStrut(24));
        } else {
            cab.add(Box.createVerticalStrut(20)); // seção de uma só tela: sem barra de abas
        }

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
        paginaHost.validate();
        paginaHost.repaint();
        paginaHost.animar(fotoAntiga);
    }

    private JComponent criarAbas(Secao s) {
        JPanel bar = Estilo.acoes(0);
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

    private String nomePortal() {
        switch (usuarioLogado.getPerfil()) {
            case ALUNO: return "Portal do Aluno";
            case PROFESSOR: return "Portal do Professor";
            case COORDENADOR: return "Portal da Coordenação";
            default: return "Portal Acadêmico";
        }
    }

    /** Seção para a qual o botão laranja do cabeçalho leva, conforme o perfil (-1 = sem atalho). */
    private int secaoDoAtalho() {
        switch (usuarioLogado.getPerfil()) {
            case ALUNO: return secPagamentos;
            case PROFESSOR: return secFrequencia;
            case COORDENADOR: return secGestaoPag;
            default: return -1;
        }
    }

    private String dicaDoAtalho() {
        switch (usuarioLogado.getPerfil()) {
            case PROFESSOR: return "Aulas com frequência aguardando lançamento";
            case COORDENADOR: return "Cobranças, inadimplência e pagamentos recebidos";
            default: return "Mensalidades, PIX, boleto e histórico";
        }
    }

    private String textoDoAtalho() {
        switch (usuarioLogado.getPerfil()) {
            case PROFESSOR: return pendentes > 0 ? "Frequência (" + pendentes + ")" : "Frequência";
            case COORDENADOR: return pendentes > 0 ? "Inadimplentes (" + pendentes + ")" : "Pagamentos";
            default: return pendentes > 0 ? "Pagamentos (" + pendentes + ")" : "Pagamentos";
        }
    }

    /** Atualiza o número do botão do cabeçalho: mensalidades pendentes, frequências a lançar ou inadimplentes. */
    private void atualizarPendencias() {
        try {
            switch (usuarioLogado.getPerfil()) {
                case ALUNO:
                    pendentes = cobrancaDAO.contarPendentes(usuarioLogado.getUsuarioId());
                    break;
                case PROFESSOR:
                    pendentes = academicoDAO.pendenciasDeFrequencia(usuarioLogado.getUsuarioId()).size();
                    break;
                case COORDENADOR:
                    pendentes = cobrancaDAO.alunosInadimplentes();
                    break;
                default:
                    pendentes = 0;
            }
        } catch (RuntimeException ex) {
            pendentes = 0;
        }
        if (pillPagamentos != null) {
            pillPagamentos.setText(textoDoAtalho());
        }
    }

    /** Notificações do sino, de acordo com o papel do usuário. */
    private List<String[]> notificacoes() {
        List<String[]> l = new ArrayList<>();
        try {
            switch (usuarioLogado.getPerfil()) {
                case ALUNO:
                    l.add(new String[]{"Rematrícula aberta", "Secretaria · Prazo até 15/10"});
                    l.add(new String[]{"Nova nota lançada", "Banco de Dados · Quiz 02"});
                    l.add(new String[]{"Aula gravada disponível", "Modelagem de Software"});
                    break;
                case PROFESSOR: {
                    int freq = academicoDAO.pendenciasDeFrequencia(usuarioLogado.getUsuarioId()).size();
                    if (freq > 0) {
                        l.add(new String[]{"Frequência a lançar", freq + (freq == 1 ? " aula aguardando" : " aulas aguardando")
                                + " seu lançamento"});
                    }
                    int semNota = 0;
                    for (Turma t : new TurmaDAO().listar("", usuarioLogado.getUsuarioId())) {
                        semNota += ResumoTurma.calcular(t.getTurmaId()).semNotaCompleta;
                    }
                    if (semNota > 0) {
                        l.add(new String[]{"Notas incompletas", semNota + " aluno(s) sem todas as notas (A1–A3)"});
                    }
                    break;
                }
                case COORDENADOR: {
                    int inad = cobrancaDAO.alunosInadimplentes();
                    if (inad > 0) {
                        l.add(new String[]{"Inadimplência", inad + " aluno(s) com mensalidade vencida"});
                    }
                    int vazias = 0;
                    for (Turma t : new TurmaDAO().listar("", null)) {
                        if (t.getQtdAlunos() == 0) {
                            vazias++;
                        }
                    }
                    if (vazias > 0) {
                        l.add(new String[]{"Turmas sem alunos", vazias + " turma(s) sem matrículas"});
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (RuntimeException ex) {
            // sem banco: o sino fica vazio
        }
        return l;
    }

    /** Faixa de aviso com atalho para pagar, exibida ao aluno com mensalidades pendentes. */
    private JComponent criarBannerPendencias() {
        if (pendentes <= 0 || secPagamentos < 0 || secAtual == secPagamentos) {
            return null;
        }
        JPanel faixa = new JPanel(new BorderLayout(16, 0));
        faixa.setBackground(new Color(0xfbefd5));
        faixa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xf29b38)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        faixa.add(new JLabel(new Icone(Tipo.CARD, 22, new Color(0x9a6312))), BorderLayout.WEST);
        faixa.add(Estilo.rotulo("Você tem " + pendentes + (pendentes == 1 ? " mensalidade pendente" : " mensalidades pendentes")
                + ". Pague agora por PIX ou boleto.", Font.BOLD, 14, new Color(0x9a6312)), BorderLayout.CENTER);
        Estilo.Botao ir = new Estilo.Botao("Pagar agora", new Color(0xe8862a), new Color(0xcf741f),
                Color.WHITE, 7, 16, 13);
        ir.addActionListener(e -> navegar(secPagamentos, 0));
        faixa.add(ir, BorderLayout.EAST);
        faixa.setMaximumSize(new Dimension(Integer.MAX_VALUE, faixa.getPreferredSize().height));
        return faixa;
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
            setFont(Estilo.fonte(ativa ? Font.BOLD : Font.PLAIN, 13));
            setForeground(ativa ? Estilo.AC : Estilo.MU);
            setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));
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
