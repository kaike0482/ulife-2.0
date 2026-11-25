package br.com.ulife;

import br.com.ulife.ui.TelaLogin;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Configurar Look and Feel do sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Usa o padrão se falhar
        }

        // Iniciar a aplicação na Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new TelaLogin().setVisible(true);
        });
    }
}