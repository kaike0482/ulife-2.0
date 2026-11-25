package br.com.ulife;

import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {
    public static void main(String[] args) {
        String senha = "123456";
        String hash = BCrypt.hashpw(senha, BCrypt.gensalt());
        System.out.println("Hash for '" + senha + "': " + hash);
    }
}