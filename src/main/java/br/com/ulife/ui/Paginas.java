package br.com.ulife.ui;

import br.com.ulife.ui.Icone.Tipo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Páginas informativas do portal (portadas do protótipo HTML). */
public final class Paginas {

    private Paginas() {}

    private static final QuadroAulas.UC[] UCS = {QuadroAulas.A, QuadroAulas.B, QuadroAulas.C, QuadroAulas.D};

    // ------------------------------------------------------------------ blocos reutilizáveis

    private static Icon ic(Tipo t, int tam) {
        return new Icone(t, tam, Estilo.AC);
    }

    /** Cartão: ícone, título e descrição. */
    private static JComponent cartao(Tipo tipo, String titulo, String desc) {
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 6), 20);
        c.add(new JLabel(ic(tipo, 28)), BorderLayout.NORTH);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        JLabel a = Estilo.texto(titulo, 260, Font.BOLD, 16, Estilo.TX);
        JLabel b = Estilo.texto(desc, 260, Font.PLAIN, 13, Estilo.MU);
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.add(a);
        t.add(Box.createVerticalStrut(6));
        t.add(b);
        c.add(t, BorderLayout.CENTER);
        return c;
    }

    private static JComponent grade(List<JComponent> itens) {
        JPanel g = new JPanel(new GridLayout(0, 3, 16, 16));
        g.setOpaque(false);
        for (JComponent i : itens) {
            g.add(i);
        }
        // completa a última linha para as células não esticarem
        while (g.getComponentCount() % 3 != 0) {
            JPanel vazio = new JPanel();
            vazio.setOpaque(false);
            g.add(vazio);
        }
        return g;
    }

    private static JComponent cartoes(Object[][] itens) {
        List<JComponent> l = new ArrayList<>();
        for (Object[] i : itens) {
            l.add(cartao((Tipo) i[0], (String) i[1], (String) i[2]));
        }
        return grade(l);
    }

    private static JComponent feed(Object[][] itens) {
        Estilo.Cartao c = new Estilo.Cartao(new GridLayout(0, 1), 0);
        for (int n = 0; n < itens.length; n++) {
            Object[] i = itens[n];
            JPanel linha = new JPanel(new BorderLayout(14, 0));
            linha.setOpaque(false);
            linha.setBorder(BorderFactory.createCompoundBorder(
                    n == 0 ? BorderFactory.createEmptyBorder() : BorderFactory.createMatteBorder(1, 0, 0, 0, Estilo.BD),
                    BorderFactory.createEmptyBorder(16, 16, 16, 16)));
            linha.add(new JLabel(ic((Tipo) i[0], 22)), BorderLayout.WEST);
            JPanel t = new JPanel(new GridLayout(2, 1));
            t.setOpaque(false);
            t.add(Estilo.rotulo((String) i[1], Font.BOLD, 14, Estilo.TX));
            t.add(Estilo.rotulo((String) i[2], Font.PLAIN, 13, Estilo.MU));
            linha.add(t, BorderLayout.CENTER);
            c.add(linha);
        }
        return c;
    }

    private static JComponent docCard(Tipo tipo, String titulo, String desc, String botao) {
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 8), 20);
        c.add(new JLabel(ic(tipo, 28)), BorderLayout.NORTH);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        JLabel a = Estilo.texto(titulo, 560, Font.BOLD, 16, Estilo.TX);
        JLabel b = Estilo.texto(desc, 560, Font.PLAIN, 13, Estilo.MU);
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.add(a);
        t.add(Box.createVerticalStrut(6));
        t.add(b);
        c.add(t, BorderLayout.CENTER);
        JButton b1 = Estilo.primario(botao);
        b1.addActionListener(e -> Estilo.aviso(c, botao + " — funcionalidade em desenvolvimento."));
        JPanel sul = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        sul.setOpaque(false);
        sul.add(b1);
        c.add(sul, BorderLayout.SOUTH);
        return limitar(c, 640);
    }

    private static JComponent limitar(JComponent c, int largura) {
        JPanel p = Estilo.acoes(0);
        p.setOpaque(false);
        c.setPreferredSize(new Dimension(largura, c.getPreferredSize().height));
        p.add(c);
        return p;
    }

    private static JComponent progresso(Tipo tipo, String titulo, String desc, int pct, String nota) {
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 8), 20);
        JPanel top = Estilo.acoes(14);
        top.setOpaque(false);
        top.add(new JLabel(ic(tipo, 28)));
        top.add(Estilo.rotulo(titulo, Font.BOLD, 16, Estilo.TX));
        c.add(top, BorderLayout.NORTH);
        JPanel m = new JPanel();
        m.setOpaque(false);
        m.setLayout(new BoxLayout(m, BoxLayout.Y_AXIS));
        JLabel d = Estilo.texto(desc, 560, Font.PLAIN, 13, Estilo.MU);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        m.add(d);
        m.add(Box.createVerticalStrut(12));
        JComponent barra = Estilo.barra(pct, Estilo.AC);
        barra.setAlignmentX(Component.LEFT_ALIGNMENT);
        m.add(barra);
        m.add(Box.createVerticalStrut(8));
        JLabel n = Estilo.rotulo(pct + "% " + nota, Font.BOLD, 13, Estilo.TX);
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        m.add(n);
        c.add(m, BorderLayout.CENTER);
        return limitar(c, 640);
    }

    /** Tabela somente leitura; colunas listadas em tags aparecem como etiquetas coloridas. */
    private static JComponent tabela(String[] colunas, Object[][] linhas, String... colunasTag) {
        DefaultTableModel m = new DefaultTableModel(linhas, colunas) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        Estilo.estilizarTabela(t, colunasTag);
        return Estilo.tabelaSemRolagem(t);
    }

    private static String nota(double v) {
        return String.format("%.1f", v).replace('.', ',');
    }

    // ------------------------------------------------------------------ AULAS

    public static JComponent quadro() { return new QuadroAulas(); }

    public static JComponent gravadas() {
        String[][] r = {{"A", "29/09"}, {"B", "24/09"}, {"A", "22/09"}, {"C", "18/09"}, {"D", "15/09"}, {"B", "11/09"}};
        List<JComponent> l = new ArrayList<>();
        for (String[] x : r) {
            QuadroAulas.UC u = uc(x[0]);
            Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 6), 20);
            c.add(new JLabel(ic(Tipo.MON, 28)), BorderLayout.NORTH);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            JLabel a = Estilo.texto(u.nome, 260, Font.BOLD, 16, Estilo.TX);
            JLabel b = Estilo.texto(u.professor + "\nAula de " + x[1] + " · 19h00 - 21h50", 260, Font.PLAIN, 13, Estilo.MU);
            JLabel link = new JLabel("<html><u>Acessar gravação</u></html>");
            link.setFont(Estilo.fonte(Font.PLAIN, 13));
            link.setForeground(Estilo.AC);
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Estilo.aviso(c, "Gravação (em desenvolvimento).");
                }
            });
            for (JComponent j : new JComponent[]{a, b, link}) {
                j.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            t.add(a);
            t.add(Box.createVerticalStrut(6));
            t.add(b);
            t.add(Box.createVerticalStrut(10));
            t.add(link);
            c.add(t, BorderLayout.CENTER);
            l.add(c);
        }
        return grade(l);
    }

    private static QuadroAulas.UC uc(String k) {
        return UCS["ABCD".indexOf(k)];
    }

    public static JComponent ucs() {
        Object[][] r = {{"A", 72}, {"B", 55}, {"C", 40}, {"D", 85}};
        List<JComponent> l = new ArrayList<>();
        for (Object[] x : r) {
            QuadroAulas.UC u = uc((String) x[0]);
            int pct = (Integer) x[1];
            Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 6), 20);
            c.add(new JLabel(ic(Tipo.CAP, 28)), BorderLayout.NORTH);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            JLabel a = Estilo.texto(u.nome, 260, Font.BOLD, 16, Estilo.TX);
            JLabel b = Estilo.texto(u.professor, 260, Font.PLAIN, 13, Estilo.MU);
            JComponent barra = Estilo.barra(pct, Estilo.AC);
            JLabel n = Estilo.rotulo(pct + "% do conteúdo concluído", Font.PLAIN, 13, Estilo.MU);
            for (JComponent j : new JComponent[]{a, b, barra, n}) {
                j.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            t.add(a);
            t.add(Box.createVerticalStrut(6));
            t.add(b);
            t.add(Box.createVerticalStrut(12));
            t.add(barra);
            t.add(Box.createVerticalStrut(6));
            t.add(n);
            c.add(t, BorderLayout.CENTER);
            l.add(c);
        }
        return grade(l);
    }

    public static JComponent atividades() {
        Object[][] r = {
                {"Prova N1", QuadroAulas.A.nome, "05/10", "4,0", "Em breve"},
                {"Estudo de caso", QuadroAulas.B.nome, "09/10", "2,0", "Em breve"},
                {"Diagrama UML", QuadroAulas.A.nome, "14/10", "2,0", "Pendente"},
                {"Seminário", QuadroAulas.C.nome, "21/10", "3,0", "Pendente"},
                {"Quiz 02", QuadroAulas.D.nome, "25/09", "1,0", "Entregue"}};
        return tabela(new String[]{"Atividade", "Unidade de Curso", "Entrega", "Peso", "Status"}, r, "Status");
    }

    public static JComponent notasFaltas() {
        Object[][] r = new Object[4][];
        for (int i = 0; i < 4; i++) {
            r[i] = new Object[]{UCS[i].nome, nota(7 + i * 0.6), nota(6.5 + i * 0.7), String.valueOf(i * 2), "Regular"};
        }
        return tabela(new String[]{"UC", "N1", "N2", "Faltas", "Situação"}, r, "Situação");
    }

    public static JComponent planoEnsino() {
        List<JComponent> l = new ArrayList<>();
        for (QuadroAulas.UC u : UCS) {
            l.add(cartao(Tipo.DOC, u.nome, u.professor));
        }
        return grade(l);
    }

    public static JComponent agendamento() {
        return feed(new Object[][]{
                {Tipo.STAR, "Prova N1 · Cálculo I", "Agendamento aberto até 01/10 · Escolha o melhor horário"},
                {Tipo.STAR, "Prova N1 · Modelagem de Software", "Agendamento aberto até 03/10"},
                {Tipo.STAR, "Seminário · Arquitetura de Sistemas", "Agendamento aberto até 18/10"}});
    }

    public static JComponent bibliotecasOnline() {
        return docCard(Tipo.LIB, "Bibliotecas Online",
                "Acesso a acervos digitais, e-books e bases de artigos acadêmicos parceiras da instituição.",
                "Acessar bibliotecas online");
    }

    public static JComponent bibliotecaFisica() {
        return docCard(Tipo.BOOK, "Biblioteca Física",
                "Campus IUSJ São Bernardo · Térreo · Seg. a Sex. 8h–22h · Sáb. 8h–12h", "Consultar acervo físico");
    }

    // ------------------------------------------------------------------ CURSO

    public static JComponent certificados() {
        return cartoes(new Object[][]{
                {Tipo.DOC, "Certificado de Conclusão de Curso", "Disponível após a integralização de todas as UCs"},
                {Tipo.DOC, "Certificado de Participação", "Eventos e atividades extracurriculares concluídas"},
                {Tipo.DOC, "Certificado NEST/HUB", "Programas de extensão e inovação concluídos"}});
    }

    public static JComponent historico() {
        return docCard(Tipo.DOC, "Histórico Escolar",
                "Emita seu histórico escolar completo, com notas e frequência de todos os semestres cursados.",
                "Emitir histórico em PDF");
    }

    public static JComponent extensao() {
        return cartoes(new Object[][]{
                {Tipo.STAR, "Programa de Extensão Comunitária", "40h · Inscrições abertas até 10/10"},
                {Tipo.STAR, "Projeto de Iniciação Científica", "60h · Vagas limitadas"},
                {Tipo.STAR, "Curso de Libras", "20h · Modalidade online"}});
    }

    public static JComponent atividadesComplementares() {
        Object[][] r = {
                {"Semana de Tecnologia", "Evento acadêmico", "8h", "Validada"},
                {"Curso de Libras", "Curso extracurricular", "20h", "Validada"},
                {"Monitoria de Banco de Dados", "Monitoria", "30h", "Em análise"},
                {"Projeto Voluntário", "Extensão", "15h", "Pendente"}};
        return tabela(new String[]{"Atividade", "Categoria", "Carga Horária", "Status"}, r, "Status");
    }

    public static JComponent planoCurricular() {
        return cartoes(new Object[][]{
                {Tipo.CAP, "1º semestre", "Concluído · 6 UCs"},
                {Tipo.CAP, "2º semestre", "Concluído · 6 UCs"},
                {Tipo.CAP, "3º semestre", "Em andamento · 4 UCs"},
                {Tipo.CAP, "4º semestre", "A cursar · 6 UCs"},
                {Tipo.CAP, "5º semestre", "A cursar · 5 UCs"},
                {Tipo.CAP, "6º semestre", "A cursar · 5 UCs"}});
    }

    public static JComponent declaracaoRegularidade() {
        return docCard(Tipo.DOC, "Declaração de Regularidade Acadêmica",
                "Documento que comprova seu vínculo e situação regular de matrícula na instituição.",
                "Emitir declaração");
    }

    public static JComponent percentualHibridez() {
        return progresso(Tipo.CLA, "Percentual de Hibridez",
                "Percentual de carga horária cursada em formato digital em relação ao total permitido pela modalidade do seu curso.",
                32, "utilizado (limite de 40%)");
    }

    // ------------------------------------------------------------------ ACADEMIA / SERVIÇOS

    public static JComponent portalAcademia() {
        return docCard(Tipo.BAG, "Portal Academia de Futuros Profissionais",
                "Estágios, buscador de vagas, simulador de entrevistas com IA e programas NEST/HUB em um só lugar.",
                "Abrir portal (nova aba)");
    }

    public static JComponent rematricula() {
        return docCard(Tipo.DOC, "Rematrícula 2026/2",
                "Período de rematrícula aberto até 15/10. Confirme sua matrícula para continuar no próximo semestre.",
                "Iniciar rematrícula");
    }

    public static JComponent atendimento() {
        return feed(new Object[][]{
                {Tipo.CHAT, "Central de Relacionamento", "Chat institucional · Atendimento em horário comercial"},
                {Tipo.MAIL, "E-mail da Secretaria", "secretaria@portaldoaluno.edu.br"},
                {Tipo.BELL, "Abrir chamado", "Acompanhe solicitações abertas com a instituição"}});
    }

    public static JComponent meusContratos() {
        Object[][] r = {
                {"2026/2", "Contrato de Prestação de Serviços", "Assinado", "Baixar"},
                {"2026/1", "Contrato de Prestação de Serviços", "Assinado", "Baixar"},
                {"2025/2", "Contrato de Prestação de Serviços", "Assinado", "Baixar"}};
        DefaultTableModel m = new DefaultTableModel(r, new String[]{"Semestre", "Documento", "Status", ""}) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable t = new JTable(m);
        Estilo.estilizarTabela(t, "Status");
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (t.columnAtPoint(e.getPoint()) == 3 && t.rowAtPoint(e.getPoint()) >= 0) {
                    Estilo.aviso(t, "Download do contrato — funcionalidade em desenvolvimento.");
                }
            }
        });
        return Estilo.tabelaSemRolagem(t);
    }

    // ------------------------------------------------------------------ COMUNICAÇÃO

    public static JComponent conectados() {
        return feed(new Object[][]{
                {Tipo.LINK, "Microsoft Teams", "Salas de aula virtuais e reuniões institucionais"},
                {Tipo.LINK, "SharePoint Acadêmico", "Materiais e documentos compartilhados pela coordenação"},
                {Tipo.LINK, "Comunidade de Alunos", "Fórum de discussão entre estudantes do curso"}});
    }

    public static JComponent avisos() {
        return feed(new Object[][]{
                {Tipo.MAIL, "Rematrícula aberta", "Secretaria · Prazo até 15/10"},
                {Tipo.MAIL, "Calendário de provas N1", "Coordenação · Semana de 5/10"},
                {Tipo.MAIL, "Manutenção programada no portal", "TI · Sábado, 02h às 04h"}});
    }

    public static JComponent mensagens() {
        return feed(new Object[][]{
                {Tipo.MAIL, "Prof. Lawilson Pereira De Lima", "Material da aula de Modelagem disponível"},
                {Tipo.MAIL, "Secretaria Acadêmica", "Seu requerimento foi deferido"},
                {Tipo.MAIL, "Coordenação do Curso", "Convite para mentoria de carreira"}});
    }

    public static JComponent notificacoes() {
        return feed(new Object[][]{
                {Tipo.STAR, "Nova nota lançada", "Banco de Dados · Quiz 02"},
                {Tipo.STAR, "Aula gravada disponível", "Modelagem de Software"},
                {Tipo.STAR, "Atividade próxima do prazo", "Diagrama UML · 14/10"}});
    }
}
