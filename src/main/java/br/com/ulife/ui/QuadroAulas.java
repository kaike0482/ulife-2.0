package br.com.ulife.ui;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/** Quadro de Aulas semanal (mesma lógica e layout do protótipo HTML). */
public class QuadroAulas extends JPanel {

    static final class UC {
        final String nome, professor, local;
        UC(String nome, String professor, String local) {
            this.nome = nome;
            this.professor = professor;
            this.local = local;
        }
    }

    static final UC A = new UC("Modelagem de Software", "Prof. Lawilson Pereira De Lima",
            "São Bernardo Do Campo - Inovação Digital - 16º Andar - 1603");
    static final UC B = new UC("Modelos, métodos e técnicas da Engenharia de Software", "Prof. Edcleisson Martinez Zanardi",
            "São Bernardo Do Campo - Laboratório De Informática - Único - 18º Andar - 1801");
    static final UC C = new UC("Modelos, métodos e técnicas da Engenharia de Software", "Patricia Vitorino Gomes",
            "São Bernardo Do Campo - Inovação Digital - Único - 7º Andar - 703");
    static final UC D = new UC("Banco de Dados", "Profa. Marina Duarte",
            "São Bernardo Do Campo - Inovação Digital - 4º Andar - 402");

    private static final String[] MESES = {"JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO", "JULHO",
            "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"};
    private static final String[] DIAS = {"SEGUNDA", "TERÇA", "QUARTA", "QUINTA", "SEXTA", "SÁBADO"};

    private int semana = 0;

    public QuadroAulas() {
        super(new BorderLayout(0, 20));
        setOpaque(false);
        montar();
    }

    private void montar() {
        removeAll();
        LocalDate hoje = LocalDate.now();
        LocalDate segunda = hoje.with(DayOfWeek.MONDAY).plusWeeks(semana);
        LocalDate sabado = segunda.plusDays(5);

        // Cabeçalho da semana
        JPanel wk = Estilo.acoes(14);
        wk.setOpaque(false);
        String mes = segunda.getMonthValue() == sabado.getMonthValue()
                ? MESES[segunda.getMonthValue() - 1]
                : MESES[segunda.getMonthValue() - 1] + "/" + MESES[sabado.getMonthValue() - 1];
        JLabel lblMes = new JLabel("<html>" + mes + " <span style='color:#6b7a90;font-weight:normal'>"
                + sabado.getYear() + "</span></html>");
        lblMes.setFont(Estilo.fonte(Font.BOLD, 22));
        lblMes.setForeground(Estilo.TX);
        wk.add(lblMes);
        wk.add(setaBotao("‹", -1));
        JLabel lab = new JLabel("<html><u>semana " + segunda.getDayOfMonth() + "-" + sabado.getDayOfMonth() + "</u></html>");
        lab.setFont(Estilo.fonte(Font.PLAIN, 15));
        lab.setForeground(Estilo.AC);
        wk.add(lab);
        wk.add(setaBotao("›", 1));
        JButton bHoje = Estilo.neutro("exibir hoje");
        bHoje.addActionListener(e -> {
            semana = 0;
            montar();
        });
        wk.add(bHoje);

        // Quadro
        Estilo.Cartao quadro = new Estilo.Cartao(new GridLayout(1, 6, 18, 0), 22);
        for (int i = 0; i < 6; i++) {
            LocalDate d = segunda.plusDays(i);
            boolean agora = d.equals(hoje);
            JPanel col = new JPanel(new BorderLayout(0, 14));
            col.setOpaque(false);
            col.add(cabecalhoDia(DIAS[i], d.getDayOfMonth(), agora), BorderLayout.NORTH);
            col.add(cartaoAula(i, agora), BorderLayout.CENTER);
            quadro.add(col);
        }

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(wk, BorderLayout.WEST);
        add(topo, BorderLayout.NORTH);
        add(quadro, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JButton setaBotao(String t, int delta) {
        JButton b = new JButton(t);
        b.setFont(Estilo.fonte(Font.PLAIN, 24));
        b.setForeground(Estilo.AC);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            semana += delta;
            montar();
        });
        return b;
    }

    private JComponent cabecalhoDia(String nome, int dia, boolean hoje) {
        JPanel p = new JPanel(new GridLayout(2, 1)) {
            @Override
            protected void paintComponent(Graphics g) {
                if (hoje) {
                    g.setColor(Estilo.P);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        Color cor = hoje ? Color.WHITE : Estilo.MU;
        JLabel a = Estilo.rotulo(nome, Font.BOLD, 15, cor);
        a.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel b = Estilo.rotulo(String.valueOf(dia), Font.PLAIN, 15, cor);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(a);
        p.add(b);
        return p;
    }

    /** Mesma regra do protótipo: seg (semanas ímpares) = BD; ter = gravação; qua/qui/sex = presencial. */
    private JComponent cartaoAula(int diaSemana, boolean agora) {
        UC uc = null;
        boolean gravada = false;
        switch (diaSemana) {
            case 0: if (semana % 2 != 0) uc = D; break;
            case 1: uc = A; gravada = true; break;
            case 2: uc = A; break;
            case 3: uc = B; break;
            case 4: uc = C; break;
            default: break;
        }

        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createLineBorder(agora ? Estilo.AC : Estilo.BD, 2));

        JLabel ic = new JLabel(new Icone(uc == null ? Icone.Tipo.BOOK : (gravada ? Icone.Tipo.MON : Icone.Tipo.CLA),
                32, Estilo.MU));
        ic.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(Box.createVerticalStrut(18));
        c.add(ic);
        c.add(Box.createVerticalStrut(10));
        c.add(barra(uc == null ? Estilo.BD : (gravada ? Estilo.OR : Estilo.TQ)));
        c.add(Box.createVerticalStrut(10));

        if (uc == null) {
            c.add(centro(Estilo.rotulo("(sem aula)", Font.BOLD, 14, Estilo.MU)));
            c.add(Box.createVerticalStrut(14));
            return c;
        }

        if (gravada) {
            JLabel l = new JLabel("<html><u>Acessar gravação</u></html>");
            l.setFont(Estilo.fonte(Font.BOLD, 14));
            l.setForeground(Estilo.AC);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    Estilo.aviso(c, "Gravação da aula de " + A.nome + " (em desenvolvimento).");
                }
            });
            c.add(centro(l));
        } else {
            c.add(centro(Estilo.rotulo("Presencial", Font.BOLD, 14, Estilo.MU)));
        }
        c.add(Box.createVerticalStrut(10));

        JPanel tm = new JPanel();
        tm.setBackground(Estilo.ST);
        tm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tm.add(Estilo.rotulo("19h00 - 21h50", Font.BOLD, 15, Estilo.TX));
        c.add(tm);

        JPanel in = new JPanel();
        in.setLayout(new BoxLayout(in, BoxLayout.Y_AXIS));
        in.setOpaque(false);
        in.setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 10));
        in.add(centro(Estilo.texto(uc.nome, 110, Font.PLAIN, 14, Estilo.TX)));
        in.add(Box.createVerticalStrut(6));
        in.add(separador());
        in.add(Box.createVerticalStrut(6));
        in.add(centro(Estilo.texto(gravada ? "Digital" : uc.local, 110, Font.PLAIN, 14, Estilo.TX)));
        in.add(Box.createVerticalStrut(6));
        in.add(separador());
        in.add(Box.createVerticalStrut(8));
        in.add(centro(Estilo.texto(uc.professor, 110, Font.PLAIN, 12, Estilo.TX)));
        c.add(in);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(c, BorderLayout.NORTH);
        return wrap;
    }

    private static JComponent barra(Color cor) {
        JComponent b = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(80, 7));
        b.setMaximumSize(new Dimension(80, 7));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private static JComponent centro(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (c instanceof JLabel) {
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
        }
        return c;
    }

    private static JComponent separador() {
        JSeparator s = new JSeparator();
        s.setForeground(Estilo.BD);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        return s;
    }
}
