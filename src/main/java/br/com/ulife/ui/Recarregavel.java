package br.com.ulife.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/** Painel cujo conteúdo é reconstruído a cada ação (ex.: depois de entregar uma atividade). */
public class Recarregavel extends JPanel {

    private final Function<Runnable, JComponent> construtor;

    public Recarregavel(Function<Runnable, JComponent> construtor) {
        super(new BorderLayout());
        setOpaque(false);
        this.construtor = construtor;
        recarregar();
    }

    public void recarregar() {
        removeAll();
        try {
            add(construtor.apply(this::recarregar), BorderLayout.CENTER);
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro ao carregar: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO), BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
}
