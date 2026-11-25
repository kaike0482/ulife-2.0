package br.com.ulife.dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tabelas e dados de conteúdo das seções Aulas e Curso do portal do aluno: horário das turmas, gravações,
 * plano de ensino, atividades avaliativas, agendamento de avaliações, acervo das bibliotecas, extensão,
 * atividades complementares, plano curricular e documentos emitidos. Idempotente.
 */
final class ConteudoSchema {

    private ConteudoSchema() {}

    private static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS UNIDADE_CURRICULAR (uc_id INT AUTO_INCREMENT PRIMARY KEY, semestre INT NOT NULL, "
            + "nome VARCHAR(150) NOT NULL, carga_horaria INT NOT NULL DEFAULT 60) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS GRAVACAO (gravacao_id INT AUTO_INCREMENT PRIMARY KEY, turma_id INT NOT NULL, "
            + "aula_data DATE NOT NULL, titulo VARCHAR(150) NOT NULL, url VARCHAR(500) NOT NULL, "
            + "CONSTRAINT fk_grav_turma FOREIGN KEY (turma_id) REFERENCES TURMA(turma_id) ON DELETE CASCADE) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS PLANO_ENSINO (turma_id INT PRIMARY KEY, ementa TEXT, objetivos TEXT, conteudo TEXT, "
            + "criterios TEXT, bibliografia TEXT, "
            + "CONSTRAINT fk_plano_turma FOREIGN KEY (turma_id) REFERENCES TURMA(turma_id) ON DELETE CASCADE) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS AVALIACAO (avaliacao_id INT AUTO_INCREMENT PRIMARY KEY, turma_id INT NOT NULL, "
            + "tipo VARCHAR(20) NOT NULL, descricao VARCHAR(150) NOT NULL, agendar_ate DATE NOT NULL, "
            + "CONSTRAINT fk_aval_turma FOREIGN KEY (turma_id) REFERENCES TURMA(turma_id) ON DELETE CASCADE) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS HORARIO_AVALIACAO (horario_id INT AUTO_INCREMENT PRIMARY KEY, avaliacao_id INT NOT NULL, "
            + "data_hora DATETIME NOT NULL, vagas INT NOT NULL DEFAULT 10, "
            + "CONSTRAINT fk_hor_aval FOREIGN KEY (avaliacao_id) REFERENCES AVALIACAO(avaliacao_id) ON DELETE CASCADE) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS AGENDAMENTO_AVALIACAO (horario_id INT NOT NULL, aluno_id INT NOT NULL, "
            + "criado_em DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (horario_id, aluno_id), "
            + "CONSTRAINT fk_agend_hor FOREIGN KEY (horario_id) REFERENCES HORARIO_AVALIACAO(horario_id) ON DELETE CASCADE, "
            + "CONSTRAINT fk_agend_usr FOREIGN KEY (aluno_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS ACERVO (acervo_id INT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(200) NOT NULL, "
            + "autor VARCHAR(150) NOT NULL, tipo ENUM('ONLINE','FISICO') NOT NULL, url VARCHAR(500), "
            + "localizacao VARCHAR(150), exemplares INT NOT NULL DEFAULT 0) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS EXTENSAO (extensao_id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(150) NOT NULL, "
            + "descricao VARCHAR(300), carga_horaria INT NOT NULL, inscricoes_ate DATE NOT NULL, vagas INT NOT NULL) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS INSCRICAO_EXTENSAO (extensao_id INT NOT NULL, aluno_id INT NOT NULL, "
            + "status ENUM('INSCRITO','CONCLUIDO','CANCELADO') NOT NULL DEFAULT 'INSCRITO', "
            + "criado_em DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (extensao_id, aluno_id), "
            + "CONSTRAINT fk_insc_ext FOREIGN KEY (extensao_id) REFERENCES EXTENSAO(extensao_id) ON DELETE CASCADE, "
            + "CONSTRAINT fk_insc_usr FOREIGN KEY (aluno_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS ATIVIDADE_COMPLEMENTAR (atividade_id INT AUTO_INCREMENT PRIMARY KEY, aluno_id INT NOT NULL, "
            + "descricao VARCHAR(150) NOT NULL, categoria VARCHAR(60) NOT NULL, carga_horaria INT NOT NULL, "
            + "status ENUM('EM_ANALISE','VALIDADA','REJEITADA') NOT NULL DEFAULT 'EM_ANALISE', "
            + "comprovante VARCHAR(500), parecer VARCHAR(300), registrado_em DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "CONSTRAINT fk_atcomp_usr FOREIGN KEY (aluno_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS DOCUMENTO_EMITIDO (documento_id INT AUTO_INCREMENT PRIMARY KEY, aluno_id INT NOT NULL, "
            + "tipo VARCHAR(40) NOT NULL, codigo VARCHAR(40) NOT NULL UNIQUE, emitido_em DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + "CONSTRAINT fk_doc_usr FOREIGN KEY (aluno_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE) ENGINE=InnoDB"
    };

    static void garantir(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            for (String ddl : DDL) {
                st.execute(ddl);
            }
            if (!SchemaInitializer.colunaExiste(conn, "TURMA", "dia_semana")) {
                st.execute("ALTER TABLE TURMA ADD COLUMN dia_semana TINYINT NULL, "
                        + "ADD COLUMN hora_inicio TIME NOT NULL DEFAULT '19:00:00', "
                        + "ADD COLUMN hora_fim TIME NOT NULL DEFAULT '21:50:00', "
                        + "ADD COLUMN modalidade ENUM('PRESENCIAL','DIGITAL') NOT NULL DEFAULT 'PRESENCIAL', "
                        + "ADD COLUMN local_aula VARCHAR(150) NULL, "
                        + "ADD COLUMN uc_id INT NULL");
            }
        }
        semear(conn);
    }

    // ------------------------------------------------------------------ auxiliares

    private static void exec(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) {
                ps.setObject(i + 1, p[i]);
            }
            ps.executeUpdate();
        }
    }

    private static int escalar(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) {
                ps.setObject(i + 1, p[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static int idTurma(Connection c, String codigo) throws SQLException {
        return escalar(c, "SELECT turma_id FROM TURMA WHERE codigo = ?", codigo);
    }

    // ------------------------------------------------------------------ dados de demonstração

    private static void semear(Connection c) throws SQLException {
        LocalDate hoje = LocalDate.now();

        // Plano curricular
        if (escalar(c, "SELECT COUNT(*) FROM UNIDADE_CURRICULAR") == 0) {
            Object[][] ucs = {
                {1, "Fundamentos de Programação", 80}, {1, "Matemática Discreta", 60},
                {1, "Introdução à Computação", 40}, {1, "Comunicação e Expressão", 40},
                {2, "Estruturas de Dados", 80}, {2, "Algoritmos e Complexidade", 60},
                {2, "Arquitetura de Computadores", 60}, {2, "Cálculo I", 60},
                {3, "Modelagem de Software", 80}, {3, "Banco de Dados", 80},
                {3, "Modelos, métodos e técnicas da Engenharia de Software", 80}, {3, "Redes de Computadores", 60},
                {4, "Programação Orientada a Objetos", 80}, {4, "Sistemas Operacionais", 60},
                {4, "Engenharia de Requisitos", 60}, {4, "Interação Humano-Computador", 60},
                {5, "Arquitetura de Software", 80}, {5, "Segurança da Informação", 60},
                {5, "Computação em Nuvem", 60}, {5, "Gestão de Projetos", 60},
                {6, "Inteligência Artificial", 80}, {6, "Projeto Integrador", 80},
                {6, "Empreendedorismo", 40}, {6, "Ética e Legislação", 40}};
            for (Object[] u : ucs) {
                exec(c, "INSERT INTO UNIDADE_CURRICULAR (semestre, nome, carga_horaria) VALUES (?, ?, ?)", u[0], u[1], u[2]);
            }
        }

        // Horário, modalidade, local e UC das turmas de demonstração
        String[][] turmas = {
            {"MS-2026-2", "Modelagem de Software", "3", "PRESENCIAL", "Inovação Digital - 16º Andar - 1603"},
            {"BD-2026-2", "Banco de Dados", "1", "DIGITAL", "Aula online (videochamada)"},
            {"ES-2026-2", "Modelos, métodos e técnicas da Engenharia de Software", "2", "PRESENCIAL",
                "Laboratório de Informática - 18º Andar - 1801"}};
        for (String[] t : turmas) {
            exec(c, "UPDATE TURMA SET dia_semana = ?, modalidade = ?, local_aula = ?, "
                    + "uc_id = (SELECT uc_id FROM UNIDADE_CURRICULAR WHERE nome = ? LIMIT 1) "
                    + "WHERE codigo = ? AND dia_semana IS NULL", Integer.parseInt(t[2]), t[3], t[4], t[1], t[0]);
        }
        exec(c, "UPDATE ALUNO SET semestre_atual = 3 WHERE semestre_atual < 3");

        // Plano de ensino
        if (escalar(c, "SELECT COUNT(*) FROM PLANO_ENSINO") == 0) {
            for (String[] t : turmas) {
                int id = idTurma(c, t[0]);
                if (id < 0) continue;
                exec(c, "INSERT INTO PLANO_ENSINO (turma_id, ementa, objetivos, conteudo, criterios, bibliografia) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", id,
                        "Estudo dos fundamentos e das práticas de " + t[1] + " aplicados ao desenvolvimento de sistemas.",
                        "Compreender os conceitos centrais da disciplina e aplicá-los em projetos reais, "
                                + "com ênfase em documentação, qualidade e trabalho em equipe.",
                        "Unidade 1: conceitos e contexto. Unidade 2: técnicas e notações. "
                                + "Unidade 3: estudo de caso. Unidade 4: projeto aplicado.",
                        "Avaliações A1 (dissertativa), A2 (objetiva) e A3 (projeto em grupo). "
                                + "Média mínima 6,0 e frequência mínima de 75%.",
                        "SOMMERVILLE, I. Engenharia de Software. Pearson. | PRESSMAN, R. Engenharia de Software. McGraw-Hill.");
            }
        }

        // Gravações das aulas já lançadas
        if (escalar(c, "SELECT COUNT(*) FROM GRAVACAO") == 0) {
            DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter id = DateTimeFormatter.ofPattern("yyyyMMdd");
            for (String[] t : turmas) {
                int turmaId = idTurma(c, t[0]);
                if (turmaId < 0) continue;
                try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT f.aula_data FROM FREQUENCIA f "
                        + "JOIN MATRICULA m ON m.matricula_id = f.matricula_id WHERE m.turma_id = ? AND f.lancada = TRUE "
                        + "ORDER BY f.aula_data DESC")) {
                    ps.setInt(1, turmaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            LocalDate d = rs.getDate(1).toLocalDate();
                            exec(c, "INSERT INTO GRAVACAO (turma_id, aula_data, titulo, url) VALUES (?, ?, ?, ?)",
                                    turmaId, d, "Aula de " + d.format(br) + " — " + t[1],
                                    "https://example.com/ulife/gravacoes/" + t[0] + "/" + d.format(id));
                        }
                    }
                }
            }
        }

        // Atividades avaliativas e entregas
        if (escalar(c, "SELECT COUNT(*) FROM TAREFA") == 0) {
            String[][] titulos = {
                {"MS-2026-2", "Estudo de caso - Requisitos", "Diagrama UML", "Seminário de Modelagem"},
                {"BD-2026-2", "Modelagem ER", "Lista de SQL", "Projeto de Banco de Dados"},
                {"ES-2026-2", "Resenha de artigo", "Estudo de caso", "Apresentação em grupo"}};
            int[] prazos = {-12, 4, 18};
            double[] pesos = {2.0, 2.0, 3.0};
            int alunoId = escalar(c, "SELECT usuario_id FROM USUARIO WHERE usuario = 'aluno'");
            int seq = 1;
            for (String[] t : titulos) {
                int turmaId = idTurma(c, t[0]);
                if (turmaId < 0) continue;
                for (int i = 0; i < 3; i++) {
                    LocalDateTime prazo = hoje.plusDays(prazos[i]).atTime(23, 59);
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO TAREFA (turma_id, titulo, descricao, tipo, "
                            + "arquivo_obrigatorio, tamanho_max_mb, prazo_entrega, peso) VALUES (?, ?, ?, 'Trabalho', TRUE, 50, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, turmaId);
                        ps.setString(2, t[1 + i]);
                        ps.setString(3, "Entregue o arquivo em PDF ou DOCX até o prazo. Trabalhos fora do prazo não são aceitos.");
                        ps.setObject(4, prazo);
                        ps.setDouble(5, pesos[i]);
                        ps.executeUpdate();
                        if (i == 0 && alunoId > 0) { // aluno já entregou a primeira atividade de cada turma
                            try (ResultSet k = ps.getGeneratedKeys()) {
                                k.next();
                                exec(c, "INSERT INTO ENTREGA (tarefa_id, aluno_id, protocolo_digital, arquivo_nome, "
                                        + "tamanho_arquivo_bytes, data_entrega, prazo_valido, status) "
                                        + "VALUES (?, ?, ?, ?, 245760, ?, TRUE, 'ENTREGUE')", k.getInt(1), alunoId,
                                        "ULF-" + hoje.getYear() + "-DEMO" + String.format("%04d", seq++),
                                        "entrega.pdf", hoje.plusDays(prazos[0] - 1).atTime(20, 15));
                            }
                        }
                    }
                }
            }
        }

        // Avaliações com horários para agendar
        if (escalar(c, "SELECT COUNT(*) FROM AVALIACAO") == 0) {
            for (String[] t : turmas) {
                int turmaId = idTurma(c, t[0]);
                if (turmaId < 0) continue;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO AVALIACAO (turma_id, tipo, descricao, agendar_ate) "
                        + "VALUES (?, 'A1', ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, turmaId);
                    ps.setString(2, "Prova A1 · " + t[1]);
                    ps.setObject(3, hoje.plusDays(10));
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        int av = k.getInt(1);
                        exec(c, "INSERT INTO HORARIO_AVALIACAO (avaliacao_id, data_hora, vagas) VALUES (?, ?, 3)",
                                av, hoje.plusDays(12).atTime(19, 0));
                        exec(c, "INSERT INTO HORARIO_AVALIACAO (avaliacao_id, data_hora, vagas) VALUES (?, ?, 3)",
                                av, hoje.plusDays(13).atTime(19, 0));
                        exec(c, "INSERT INTO HORARIO_AVALIACAO (avaliacao_id, data_hora, vagas) VALUES (?, ?, 3)",
                                av, hoje.plusDays(14).atTime(14, 0));
                    }
                }
            }
        }

        // Acervo das bibliotecas
        if (escalar(c, "SELECT COUNT(*) FROM ACERVO") == 0) {
            String[][] online = {
                {"Engenharia de Software", "Ian Sommerville"}, {"Sistemas de Banco de Dados", "Elmasri; Navathe"},
                {"UML Essencial", "Martin Fowler"}, {"Código Limpo", "Robert C. Martin"},
                {"Artigos ACM — Software Engineering", "ACM Digital Library"}};
            for (String[] l : online) {
                exec(c, "INSERT INTO ACERVO (titulo, autor, tipo, url, exemplares) VALUES (?, ?, 'ONLINE', ?, 0)",
                        l[0], l[1], "https://example.com/ulife/biblioteca/" + l[0].toLowerCase().replace(' ', '-'));
            }
            Object[][] fisico = {
                {"Modelagem de Dados", "Heuser", "Estante B3", 3}, {"Algoritmos: Teoria e Prática", "Cormen", "Estante A1", 5},
                {"Redes de Computadores", "Tanenbaum", "Estante C2", 2},
                {"Banco de Dados: Projeto e Implementação", "Machado", "Estante B1", 4},
                {"Engenharia de Requisitos", "Wiegers", "Estante B4", 0}};
            for (Object[] l : fisico) {
                exec(c, "INSERT INTO ACERVO (titulo, autor, tipo, localizacao, exemplares) VALUES (?, ?, 'FISICO', ?, ?)",
                        l[0], l[1], "Biblioteca do campus · " + l[2], l[3]);
            }
        }

        // Extensão
        if (escalar(c, "SELECT COUNT(*) FROM EXTENSAO") == 0) {
            exec(c, "INSERT INTO EXTENSAO (nome, descricao, carga_horaria, inscricoes_ate, vagas) VALUES (?, ?, 40, ?, 30)",
                    "Programa de Extensão Comunitária", "Projetos de tecnologia para a comunidade do entorno.", hoje.plusDays(30));
            exec(c, "INSERT INTO EXTENSAO (nome, descricao, carga_horaria, inscricoes_ate, vagas) VALUES (?, ?, 60, ?, 10)",
                    "Projeto de Iniciação Científica", "Pesquisa orientada por professor, com apresentação final.", hoje.plusDays(20));
            exec(c, "INSERT INTO EXTENSAO (nome, descricao, carga_horaria, inscricoes_ate, vagas) VALUES (?, ?, 20, ?, 40)",
                    "Curso de Libras", "Introdução à Língua Brasileira de Sinais (online).", hoje.plusDays(45));
        }

        // Atividades complementares do aluno de demonstração
        int alunoId = escalar(c, "SELECT usuario_id FROM USUARIO WHERE usuario = 'aluno'");
        if (alunoId > 0 && escalar(c, "SELECT COUNT(*) FROM ATIVIDADE_COMPLEMENTAR WHERE aluno_id = ?", alunoId) == 0) {
            Object[][] a = {
                {"Semana de Tecnologia", "Evento acadêmico", 8, "VALIDADA"},
                {"Curso de Libras", "Curso extracurricular", 20, "VALIDADA"},
                {"Monitoria de Banco de Dados", "Monitoria", 30, "EM_ANALISE"},
                {"Projeto Voluntário", "Extensão", 15, "EM_ANALISE"}};
            for (Object[] x : a) {
                exec(c, "INSERT INTO ATIVIDADE_COMPLEMENTAR (aluno_id, descricao, categoria, carga_horaria, status) "
                        + "VALUES (?, ?, ?, ?, ?)", alunoId, x[0], x[1], x[2], x[3]);
            }
        }
    }
}
