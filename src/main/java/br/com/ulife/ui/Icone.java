package br.com.ulife.ui;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/** Ícones vetoriais desenhados com Java2D (mesmos desenhos do protótipo HTML, grade 24x24). */
public class Icone implements Icon {

    public enum Tipo { MON, CLA, BOOK, CAP, BAG, LIB, MAIL, DOC, STAR, LINK, CHAT, BELL, USERS, FLAG, PIN, USER, ACC, DOWN, LEFT, CARD, HOME, CAL }

    private final Tipo tipo;
    private final int tamanho;
    private final Color cor;

    public Icone(Tipo tipo, int tamanho, Color cor) {
        this.tipo = tipo;
        this.tamanho = tamanho;
        this.cor = cor;
    }

    @Override public int getIconWidth() { return tamanho; }
    @Override public int getIconHeight() { return tamanho; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        double s = tamanho / 24.0;
        g2.scale(s, s);
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(cor);
        desenhar(g2);
        g2.dispose();
    }

    private void desenhar(Graphics2D g) {
        switch (tipo) {
            case MON:
                rect(g, 3, 4, 18, 12);
                linha(g, 2, 20, 22, 20);
                poly(g, false, 8, 20, 9, 16, 15, 16, 16, 20);
                break;
            case CLA:
                rect(g, 3, 4, 18, 12);
                g.draw(new Ellipse2D.Double(6.5, 7.5, 3, 3));
                linha(g, 13, 8, 18, 8);
                linha(g, 13, 11, 17, 11);
                linha(g, 8, 20, 16, 20);
                break;
            case BOOK: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(12, 6);
                p.curveTo(10, 4.5, 7, 4, 3, 4);
                p.lineTo(3, 18);
                p.curveTo(7, 18, 10, 18.5, 12, 20);
                p.curveTo(14, 18.5, 17, 18, 21, 18);
                p.lineTo(21, 4);
                p.curveTo(17, 4, 14, 4.5, 12, 6);
                g.draw(p);
                linha(g, 12, 6, 12, 20);
                break;
            }
            case CAP: {
                poly(g, true, 2, 9, 12, 4, 22, 9, 12, 14);
                Path2D.Double p = new Path2D.Double();
                p.moveTo(6, 11);
                p.lineTo(6, 16);
                p.curveTo(6, 17.5, 9, 19, 12, 19);
                p.curveTo(15, 19, 18, 17.5, 18, 16);
                p.lineTo(18, 11);
                g.draw(p);
                break;
            }
            case BAG:
                rect(g, 3, 8, 18, 12);
                poly(g, false, 9, 8, 9, 5, 15, 5, 15, 8);
                linha(g, 3, 13, 21, 13);
                rect(g, 11, 13, 2, 2);
                break;
            case LIB:
                rect(g, 4, 4, 4, 16);
                rect(g, 10, 4, 4, 16);
                poly(g, true, 16, 6, 20, 7, 17, 20, 13, 19);
                break;
            case MAIL:
                rect(g, 3, 5, 18, 14);
                poly(g, false, 3, 6, 12, 13, 21, 6);
                break;
            case DOC:
                poly(g, true, 6, 3, 14, 3, 18, 7, 18, 21, 6, 21);
                poly(g, false, 14, 3, 14, 7, 18, 7);
                linha(g, 9, 12, 15, 12);
                linha(g, 9, 16, 15, 16);
                break;
            case STAR: {
                double[] pts = new double[20];
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 9 : 4;
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    pts[i * 2] = 12 + r * Math.cos(a);
                    pts[i * 2 + 1] = 12.5 + r * Math.sin(a);
                }
                poly(g, true, pts);
                break;
            }
            case LINK: {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.rotate(-Math.PI / 4, 12, 12);
                g2.draw(new RoundRectangle2D.Double(2, 8.5, 11, 7, 7, 7));
                g2.draw(new RoundRectangle2D.Double(11, 8.5, 11, 7, 7, 7));
                g2.dispose();
                break;
            }
            case CHAT: {
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 13, 4, 4));
                poly(g, false, 8, 17, 7, 21, 12, 17);
                break;
            }
            case BELL: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(6, 16);
                p.lineTo(6, 11);
                p.append(new Arc2D.Double(6, 5, 12, 12, 180, -180, Arc2D.OPEN), true);
                p.lineTo(18, 16);
                p.lineTo(20, 18);
                p.lineTo(4, 18);
                p.closePath();
                g.draw(p);
                linha(g, 10, 21, 14, 21);
                break;
            }
            case USERS:
                g.draw(new Ellipse2D.Double(5.5, 4.5, 7, 7));
                g.draw(new Arc2D.Double(2.5, 14, 13, 12, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(13, 4.5, 6, 6, 90, -180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(15.5, 14, 6, 10, 0, 90, Arc2D.OPEN));
                break;
            case FLAG:
                linha(g, 5, 21, 5, 4);
                poly(g, true, 5, 4, 17, 4, 15, 8, 17, 12, 5, 12);
                break;
            case PIN: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(5, 10);
                p.append(new Arc2D.Double(5, 3, 14, 14, 180, -180, Arc2D.OPEN), true);
                p.lineTo(12, 21);
                p.closePath();
                g.draw(p);
                g.draw(new Ellipse2D.Double(9.5, 7.5, 5, 5));
                break;
            }
            case USER:
                g.draw(new Ellipse2D.Double(2, 2, 20, 20));
                g.draw(new Ellipse2D.Double(9, 7, 6, 6));
                g.draw(new Arc2D.Double(6, 14.5, 12, 10, 0, 180, Arc2D.OPEN));
                break;
            case ACC:
                g.draw(new Ellipse2D.Double(2, 2, 20, 20));
                g.fill(new Ellipse2D.Double(11, 6.5, 2, 2));
                poly(g, false, 7, 10.5, 12, 11.5, 17, 10.5);
                linha(g, 12, 11.5, 12, 15);
                poly(g, false, 10, 19, 12, 15, 14, 19);
                break;
            case CARD:
                g.draw(new RoundRectangle2D.Double(2, 5, 20, 14, 3, 3));
                linha(g, 2, 10, 22, 10);
                linha(g, 6, 15, 11, 15);
                break;
            case HOME:
                poly(g, false, 3, 11, 12, 3, 21, 11);
                poly(g, false, 5, 9.5, 5, 21, 19, 21, 19, 9.5);
                rect(g, 10, 14, 4, 7);
                break;
            case CAL:
                rect(g, 3, 5, 18, 16);
                linha(g, 3, 10, 21, 10);
                linha(g, 8, 3, 8, 7);
                linha(g, 16, 3, 16, 7);
                poly(g, false, 8, 15, 11, 18, 16, 13);
                break;
            case DOWN:
                poly(g, false, 6, 9, 12, 15, 18, 9);
                break;
            case LEFT:
                poly(g, false, 15, 6, 9, 12, 15, 18);
                break;
            default:
                break;
        }
    }

    private static void rect(Graphics2D g, double x, double y, double w, double h) {
        g.draw(new Rectangle2D.Double(x, y, w, h));
    }

    private static void linha(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poly(Graphics2D g, boolean fechar, double... p) {
        Path2D.Double d = new Path2D.Double();
        d.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) {
            d.lineTo(p[i], p[i + 1]);
        }
        if (fechar) {
            d.closePath();
        }
        g.draw(d);
    }
}
