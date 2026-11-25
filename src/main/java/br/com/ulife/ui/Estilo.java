package br.com.ulife.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Paleta, fontes e componentes visuais compartilhados (mesmo visual do protótipo HTML). */
public final class Estilo {

    private Estilo() {}

    public static final Color P = new Color(0x1c4f8c);
    public static final Color PD = new Color(0x0d2c54);
    public static final Color AC = new Color(0x2f6fd0);
    public static final Color PL = new Color(0xe3ecf9);
    public static final Color BG = new Color(0xe9eef5);
    public static final Color CARD = Color.WHITE;
    public static final Color TX = new Color(0x14233a);
    public static final Color MU = new Color(0x6b7a90);
    public static final Color BD = new Color(0xc9d4e3);
    public static final Color ST = new Color(0xeef2f8);
    public static final Color OR = new Color(0xf29b38);
    public static final Color TQ = new Color(0x2fb8ac);
    public static final Color OK = new Color(0x1f7a45);
    public static final Color ERRO = new Color(0xc0392b);

    public static Font fonte(int estilo, int tamanho) {
        return new Font("Segoe UI", estilo, tamanho);
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static JLabel rotulo(String texto, int estilo, int tamanho, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(fonte(estilo, tamanho));
        l.setForeground(cor);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4)); // evita corte da última letra
        return l;
    }

    /** Rótulo com quebra de linha automática em uma largura fixa (px). */
    public static JLabel texto(String texto, int largura, int estilo, int tamanho, Color cor) {
        JLabel l = new JLabel("<html><body style='width:" + largura + "px'>"
                + esc(texto).replace("\n", "<br>") + "</body></html>");
        l.setFont(fonte(estilo, tamanho));
        l.setForeground(cor);
        return l;
    }

    public static void aviso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Ulife 2.0", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void erro(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Ulife 2.0", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------ botões

    public static class Botao extends JButton {
        private final Color fundo;
        private final Color hover;

        Botao(String texto, Color fundo, Color hover, Color fg, int vert, int horiz, int fonteTam) {
            super(texto);
            this.fundo = fundo;
            this.hover = hover;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(fg);
            setFont(fonte(Font.PLAIN, fonteTam));
            setBorder(BorderFactory.createEmptyBorder(vert, horiz, vert, horiz));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getModel().isRollover() ? hover : fundo;
            if (!isEnabled()) {
                c = ST;
            }
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Botao primario(String texto) {
        return new Botao(texto, AC, P, Color.WHITE, 11, 20, 14);
    }

    public static Botao neutro(String texto) {
        return new Botao(texto, new Color(0xd7dde7), new Color(0xc6cfdd), new Color(0x3b4a60), 11, 20, 14);
    }

    public static Botao perigo(String texto) {
        return new Botao(texto, new Color(0xf6dcdc), new Color(0xefc6c6), new Color(0xa32b2b), 11, 20, 14);
    }

    // ------------------------------------------------------------------ cartão

    public static class Cartao extends JPanel {
        public Cartao(LayoutManager layout, int padding) {
            super(layout);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
            g2.setColor(BD);
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Linha de botões/itens alinhada à esquerda, sem o recuo lateral que o FlowLayout acrescenta. */
    public static JPanel acoes(int espaco) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, espaco, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, -espaco, 0, 0));
        return p;
    }

    /** Cartão de indicador: título, valor grande e uma linha de apoio. */
    public static JComponent indicador(String titulo, String valor, String nota, Color cor) {
        Cartao c = new Cartao(new BorderLayout(0, 4), 18);
        c.add(rotulo(titulo, Font.PLAIN, 13, MU), BorderLayout.NORTH);
        c.add(rotulo(valor, Font.BOLD, 26, cor), BorderLayout.CENTER);
        c.add(rotulo(nota == null ? " " : nota, Font.PLAIN, 12, MU), BorderLayout.SOUTH);
        return c;
    }

    // ------------------------------------------------------------------ barra de progresso

    public static JComponent barra(int percentual, Color cor) {
        JComponent b = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ST);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), 8, 8, 8));
                g2.setColor(cor);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() * percentual / 100.0, 8, 8, 8));
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(100, 8));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        return b;
    }

    // ------------------------------------------------------------------ tag de status

    public static class Tag extends JLabel {
        private final Color fundo;

        public Tag(String texto) {
            super(texto, SwingConstants.CENTER);
            Color[] cores = coresDoStatus(texto);
            this.fundo = cores[0];
            setForeground(cores[1]);
            setFont(fonte(Font.BOLD, 12));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fundo);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static Color[] coresDoStatus(String s) {
        Color azulBg = new Color(0xdfe9fa), azul = new Color(0x1c4f8c);
        Color amBg = new Color(0xfbefd5), am = new Color(0x9a6312);
        Color vdBg = new Color(0xdff3e7), vd = new Color(0x1f7a45);
        Color vmBg = new Color(0xf9dede), vm = new Color(0xa32b2b);
        switch (s) {
            case "Entregue": case "Regular": case "Validada": case "Assinado": case "Concluído":
            case "Paga": case "Confirmado": case "CONFIRMADO": case "Presente": case "Aprovado": case "Lançada":
                return new Color[]{vdBg, vd};
            case "Pendente": case "Em andamento": case "Pendente de lançamento": case "A lançar":
                return new Color[]{amBg, am};
            case "Cancelado": case "Cancelada": case "Vencida": case "Ausente": case "Reprovado":
            case "Frequência baixa":
                return new Color[]{vmBg, vm};
            default: // Em breve, Em análise, Planejado
                return new Color[]{azulBg, azul};
        }
    }

    // ------------------------------------------------------------------ tabelas

    /** Aplica o visual do protótipo (cabeçalho cinza-azulado, linhas separadas) a uma JTable. */
    public static void estilizarTabela(JTable t, String... colunasTag) {
        t.setRowHeight(42);
        t.setFont(fonte(Font.PLAIN, 14));
        t.setForeground(TX);
        t.setBackground(CARD);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BD);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(PL);
        t.setSelectionForeground(TX);
        t.setFillsViewportHeight(true);
        t.setFocusable(false);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 40));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setBackground(ST);
                l.setForeground(MU);
                l.setFont(fonte(Font.BOLD, 12));
                l.setBorder(BorderFactory.createEmptyBorder(0, 13, 0, 13));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                String nome = tb.getColumnName(c);
                for (String col : colunasTag) {
                    if (col.equals(nome) && v != null) {
                        JPanel p = new JPanel(new GridBagLayout());
                        p.setBackground(sel ? PL : CARD);
                        GridBagConstraints gc = new GridBagConstraints();
                        gc.anchor = GridBagConstraints.WEST;
                        gc.insets = new Insets(0, 12, 0, 0);
                        gc.weightx = 1;
                        p.add(new Tag(v.toString()), gc);
                        return p;
                    }
                }
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                l.setBorder(BorderFactory.createEmptyBorder(0, 13, 0, 13));
                return l;
            }
        });
    }

    /** Cabeçalho + tabela em um painel que cresce com o conteúdo (sem rolagem própria). */
    public static JPanel tabelaSemRolagem(JTable t) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(BD));
        p.add(t.getTableHeader(), BorderLayout.NORTH);
        p.add(t, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------ campos de formulário

    public static Border bordaCampo() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BD),
                BorderFactory.createEmptyBorder(9, 11, 9, 11));
    }

    public static JTextField campo() {
        JTextField f = new JTextField();
        f.setFont(fonte(Font.PLAIN, 14));
        f.setForeground(TX);
        f.setBorder(bordaCampo());
        return f;
    }

    public static JPasswordField campoSenha() {
        JPasswordField f = new JPasswordField();
        f.setFont(fonte(Font.PLAIN, 14));
        f.setForeground(TX);
        f.setBorder(bordaCampo());
        return f;
    }

    /** Painel rolável que acompanha a largura do viewport (conteúdo reflui ao redimensionar). */
    public static class PainelRolavel extends JPanel implements Scrollable {
        public PainelRolavel(LayoutManager l) { super(l); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
