package br.com.ulife;

import br.com.ulife.dao.DatabaseConnection;
import br.com.ulife.dao.UsuarioDAO;
import br.com.ulife.model.Usuario;

import java.sql.Connection;

public class TestLogin {
    public static void main(String[] args) {
        System.out.println("=== Teste de conexão e login ===");

        // 1. Testar conexão
        try {
            DatabaseConnection.getInstance().testConnection();
            System.out.println("✓ Conexão com banco OK");
        } catch (Exception e) {
            System.err.println("✗ Falha na conexão: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 2. Testar verificação de senha para cada usuário
        UsuarioDAO dao = new UsuarioDAO();

        String[] emails = {"admin@ulife.com", "aluno@ulife.com", "prof@ulife.com"};
        String senha = "123456";

        for (String email : emails) {
            try {
                boolean ok = dao.verifyPassword(email, senha);
                if (ok) {
                    Usuario u = dao.findByEmail(email).orElse(null);
                    System.out.printf("✓ Login OK: %s | Perfil: %s%n", email, u != null ? u.getPerfil() : "?");
                } else {
                    System.out.printf("✗ Login FALHOU: %s%n", email);
                }
            } catch (Exception e) {
                System.err.printf("✗ Erro ao testar %s: %s%n", email, e.getMessage());
            }
        }

        System.out.println("=== Fim do teste ===");
    }
}