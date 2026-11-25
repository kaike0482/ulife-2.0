package br.com.ulife.ui;

import br.com.ulife.dao.PortalAlunoDAO;
import br.com.ulife.dao.PortalAlunoDAO.Aula;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Quadro semanal do aluno, montado com as turmas em que ele está matriculado (dia, horário, modalidade e local). */
public class QuadroAulasAluno extends JPanel {

    private static final String[] MESES = {"JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO", "JULHO",
            "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"};
    private static final String[] DIAS = {"SEGUNDA", "TERÇA", "QUARTA", "QUINTA", "SEXTA", "SÁBADO"};

    private final List<Aula> aulas;
    private int semana = 0;

    public QuadroAulasAluno(Usuario aluno) {
        super(new BorderLayout(0, 20));
        setOpaque(false);
        aulas = new PortalAlunoDAO().quadro(aluno.getUsuarioId());
        montar();
    }

    private void montar() {
        removeAll();
        LocalDate hoje = LocalDate.now();
        LocalDate segunda = hoje.with(DayOfWeek.MONDAY).plusWeeks(semana);
        LocalDate sabado = segunda.plusDays(5);

        JPanel wk = Estilo.acoes(14);
        String mes = segunda.getMonthValue() == sabado.getMonthValue() ? MESES[segunda.getMonthValue() - 1]
                : MESES[segunda.getMonthValue() - 1] + "/" + MESES[sabado.getMonthValue() - 1];
        JLabel lblMes = new JLabel("<html>" + mes + " <span style='color:#6b7a90;font-weight:normal'>" + sabado.getYear() + "</span></html>");
        lblMes.setFont(Estilo.fonte(Font.BOLD, 22));
        lblMes.setForeground(Estilo.TX);
        wk.add(lblMes);
        wk.add(seta("‹", -1));
        JLabel lab = new JLabel("<html><u>semana " + segunda.getDayOfMonth() + "-" + sabado.getDayOfMonth() + "</u></html>");
        lab.setFont(Estilo.fonte(Font.PLAIN, 15));
        lab.setForeground(Estilo.AC);
        wk.add(lab);
        wk.add(seta("›", 1));
        Estilo.Botao bHoje = Estilo.neutro("exibir hoje");
        bHoje.addActionListener(e -> {
            semana = 0;
            montar();
        });
        wk.add(bHoje);

        Estilo.Cartao quadro = new Estilo.Cartao(new GridLayout(1, 6, 18, 0), 22);
        for (int i = 0; i < 6; i++) {
            LocalDate d = segunda.plusDays(i);
            JPanel col = new JPanel(new BorderLayout(0, 14));
            col.setOpaque(false);
            col.add(cabecalhoDia(DIAS[i], d.getDayOfMonth(), d.equals(hoje)), BorderLayout.NORTH);
            JPanel pilha = new JPanel();
            pilha.setOpaque(false);
            pilha.setLayout(new BoxLayout(pilha, BoxLayout.Y_AXIS));
            boolean algum = false;
            for (Aula a : aulas) {
                if (a.dia() == i + 1) {
                    pilha.add(cartao(a, d.equals(hoje)));
                    pilha.add(Box.createVerticalStrut(10));
                    algum = true;
                }
            }
            if (!algum) {
                pilha.add(vazio());
            }
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.add(pilha, BorderLayout.NORTH);
            col.add(wrap, BorderLayout.CENTER);
            quadro.add(col);
        }
        add(wk, BorderLayout.NORTH);
        if (aulas.isEmpty()) {
            add(Estilo.rotulo("Você ainda não está matriculado em turmas com horário definido.", Font.PLAIN, 14, Estilo.MU),
                    BorderLayout.CENTER);
        } else {
            add(quadro, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    private JButton seta(String t, int delta) {
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

    private JComponent vazio() {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createLineBorder(Estilo.BD, 2));
        JLabel ic = new JLabel(new Icone(Icone.Tipo.BOOK, 32, Estilo.MU));
        ic.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t = Estilo.rotulo("(sem aula)", Font.BOLD, 14, Estilo.MU);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(Box.createVerticalStrut(18));
        c.add(ic);
        c.add(Box.createVerticalStrut(10));
        c.add(t);
        c.add(Box.createVerticalStrut(18));
        return c;
    }

    private JComponent cartao(Aula a, boolean agora) {
        boolean digital = "DIGITAL".equals(a.modalidade());
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createLineBorder(agora ? Estilo.AC : Estilo.BD, 2));

        JLabel ic = new JLabel(new Icone(digital ? Icone.Tipo.MON : Icone.Tipo.CLA, 32, Estilo.MU));
        ic.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(Box.createVerticalStrut(14));
        c.add(ic);
        c.add(Box.createVerticalStrut(8));
        Color cor = digital ? Estilo.OR : Estilo.TQ;
        JComponent barra = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(cor);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
            }
        };
        barra.setPreferredSize(new Dimension(80, 7));
        barra.setMaximumSize(new Dimension(80, 7));
        barra.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(barra);
        c.add(Box.createVerticalStrut(8));
        c.add(centro(Estilo.rotulo(digital ? "Digital" : "Presencial", Font.BOLD, 14, Estilo.MU)));
        c.add(Box.createVerticalStrut(8));

        JPanel tm = new JPanel();
        tm.setBackground(Estilo.ST);
        tm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tm.add(Estilo.rotulo(a.inicio().replace(':', 'h') + " - " + a.fim().replace(':', 'h'), Font.BOLD, 15, Estilo.TX));
        c.add(tm);

        JPanel in = new JPanel();
        in.setLayout(new BoxLayout(in, BoxLayout.Y_AXIS));
        in.setOpaque(false);
        in.setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 10));
        in.add(centro(Estilo.texto(a.turma(), 110, Font.BOLD, 14, Estilo.TX)));
        in.add(Box.createVerticalStrut(6));
        in.add(centro(Estilo.texto(a.local() == null ? "—" : a.local(), 110, Font.PLAIN, 13, Estilo.MU)));
        in.add(Box.createVerticalStrut(6));
        in.add(centro(Estilo.texto(a.professor(), 110, Font.PLAIN, 12, Estilo.TX)));
        c.add(in);
        return c;
    }

    private static JComponent centro(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (c instanceof JLabel) {
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
        }
        return c;
    }
}
