package br.com.ulife.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Painel que faz uma transição suave (esmaecimento) da tela antiga para a nova.
 * Antes de trocar o conteúdo chame {@link #capturar()}; depois da troca, {@link #animar(BufferedImage)}.
 */
public class PainelAnimado extends JPanel {

    private static final int DURACAO_MS = 170;

    private BufferedImage antes;
    private float alpha;
    private long inicio;
    private Timer timer;

    public PainelAnimado() {
        super(new BorderLayout());
    }

    /** Foto do conteúdo atual (null se a janela ainda não está visível). */
    public BufferedImage capturar() {
        if (!isShowing() || getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(getBackground());
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        printAll(g);
        g.dispose();
        return img;
    }

    /** Mostra a foto antiga por cima do conteúdo novo e a vai apagando. */
    public void animar(BufferedImage fotoAntiga) {
        if (fotoAntiga == null || !isShowing()) {
            return;
        }
        if (timer != null) {
            timer.stop();
        }
        antes = fotoAntiga;
        alpha = 1f;
        inicio = System.currentTimeMillis();
        timer = new Timer(15, e -> {
            float t = (System.currentTimeMillis() - inicio) / (float) DURACAO_MS;
            if (t >= 1f) {
                antes = null;
                alpha = 0f;
                timer.stop();
            } else {
                float r = 1f - t;
                alpha = r * r * r; // sai rápido no começo e suaviza no fim
            }
            repaint();
        });
        timer.start();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (antes != null && alpha > 0f) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(antes, 0, 0, null);
            g2.dispose();
        }
    }
}
