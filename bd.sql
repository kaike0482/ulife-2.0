-- ========================================
-- Script SQL - Ulife 2.0 (MySQL 8.0)
-- ========================================
CREATE DATABASE IF NOT EXISTS ulife CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ulife;

-- -----------------------------------------------------
-- Tabela USUARIO
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS USUARIO (
    usuario_id INT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(30) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    senha_hash VARCHAR(255) NOT NULL,
    perfil ENUM('ALUNO', 'PROFESSOR', 'SECRETARIA', 'TESOURARIA', 'COORDENADOR') NOT NULL DEFAULT 'ALUNO',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela ALUNO (especialização de USUARIO)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS ALUNO (
    aluno_id INT NOT NULL,
    ra VARCHAR(20) NOT NULL UNIQUE,
    semestre_atual INT DEFAULT 1,
    PRIMARY KEY (aluno_id),
    CONSTRAINT fk_aluno_usuario FOREIGN KEY (aluno_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela PROFESSOR (especialização de USUARIO)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS PROFESSOR (
    professor_id INT NOT NULL,
    matricula_funcional VARCHAR(20) NOT NULL UNIQUE,
    departamento VARCHAR(100),
    PRIMARY KEY (professor_id),
    CONSTRAINT fk_professor_usuario FOREIGN KEY (professor_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela TURMA
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS TURMA (
    turma_id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    professor_id INT NOT NULL,
    semestre VARCHAR(10) NOT NULL,
    ano INT NOT NULL,
    carga_horaria_total INT DEFAULT 60,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_turma_professor FOREIGN KEY (professor_id) REFERENCES PROFESSOR(professor_id)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela MATRICULA (eixo do modelo: ALUNO x TURMA)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS MATRICULA (
    matricula_id INT AUTO_INCREMENT PRIMARY KEY,
    aluno_id INT NOT NULL,
    turma_id INT NOT NULL,
    data_matricula DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ATIVA', 'TRANCADA', 'CONCLUIDA', 'CANCELADA') NOT NULL DEFAULT 'ATIVA',
    UNIQUE KEY uk_aluno_turma (aluno_id, turma_id),
    CONSTRAINT fk_matricula_aluno FOREIGN KEY (aluno_id) REFERENCES ALUNO(aluno_id) ON DELETE CASCADE,
    CONSTRAINT fk_matricula_turma FOREIGN KEY (turma_id) REFERENCES TURMA(turma_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela TAREFA (ATIVIDADE)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS TAREFA (
    tarefa_id INT AUTO_INCREMENT PRIMARY KEY,
    turma_id INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(50) NOT NULL,
    arquivo_obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
    tamanho_max_mb INT DEFAULT 50,
    data_publicacao DATETIME DEFAULT CURRENT_TIMESTAMP,
    prazo_entrega DATETIME NOT NULL,
    peso DECIMAL(4,2) DEFAULT 0.00,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_tarefa_turma FOREIGN KEY (turma_id) REFERENCES TURMA(turma_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela ENTREGA (protocolo digital)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS ENTREGA (
    entrega_id INT AUTO_INCREMENT PRIMARY KEY,
    tarefa_id INT NOT NULL,
    aluno_id INT NOT NULL,
    protocolo_digital VARCHAR(50) NOT NULL UNIQUE,
    arquivo_nome VARCHAR(255),
    arquivo_caminho VARCHAR(500),
    tamanho_arquivo_bytes BIGINT,
    data_entrega DATETIME DEFAULT CURRENT_TIMESTAMP,
    prazo_valido BOOLEAN NOT NULL DEFAULT TRUE,
    status ENUM('ENTREGUE', 'INVALIDO', 'PRAZO_EXPIRADO', 'CORRIGIDO') NOT NULL DEFAULT 'ENTREGUE',
    nota DECIMAL(4,2),
    feedback TEXT,
    CONSTRAINT fk_entrega_tarefa FOREIGN KEY (tarefa_id) REFERENCES TAREFA(tarefa_id) ON DELETE CASCADE,
    CONSTRAINT fk_entrega_aluno FOREIGN KEY (aluno_id) REFERENCES ALUNO(aluno_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela FREQUENCIA
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS FREQUENCIA (
    frequencia_id INT AUTO_INCREMENT PRIMARY KEY,
    matricula_id INT NOT NULL,
    aula_data DATE NOT NULL,
    presenca BOOLEAN NOT NULL DEFAULT FALSE,
    justificativa VARCHAR(200),
    UNIQUE KEY uk_matricula_data (matricula_id, aula_data),
    CONSTRAINT fk_frequencia_matricula FOREIGN KEY (matricula_id) REFERENCES MATRICULA(matricula_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela NOTA
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS NOTA (
    nota_id INT AUTO_INCREMENT PRIMARY KEY,
    matricula_id INT NOT NULL,
    tarefa_id INT NOT NULL,
    valor DECIMAL(4,2) NOT NULL,
    tipo_avaliacao VARCHAR(20) NOT NULL, -- ex: N1, N2, N3, Trabalho
    data_lancamento DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nota_matricula FOREIGN KEY (matricula_id) REFERENCES MATRICULA(matricula_id) ON DELETE CASCADE,
    CONSTRAINT fk_nota_tarefa FOREIGN KEY (tarefa_id) REFERENCES TAREFA(tarefa_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela COBRANCA
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS COBRANCA (
    cobranca_id INT AUTO_INCREMENT PRIMARY KEY,
    matricula_id INT NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    vencimento DATE NOT NULL,
    status ENUM('ABERTA', 'PAGA', 'VENCIDA', 'CANCELADA') NOT NULL DEFAULT 'ABERTA',
    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cobranca_matricula FOREIGN KEY (matricula_id) REFERENCES MATRICULA(matricula_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela PAGAMENTO
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS PAGAMENTO (
    pagamento_id INT AUTO_INCREMENT PRIMARY KEY,
    cobranca_id INT NOT NULL,
    metodo ENUM('PIX', 'BOLETO') NOT NULL,
    valor_pago DECIMAL(10,2) NOT NULL,
    data_pagamento DATETIME DEFAULT CURRENT_TIMESTAMP,
    qr_code_pix TEXT,
    boleto_codigo_barras VARCHAR(100),
    transacao_id VARCHAR(100),
    status ENUM('PROCESSANDO', 'CONFIRMADO', 'REJEITADO', 'EXPIRADO') NOT NULL DEFAULT 'PROCESSANDO',
    CONSTRAINT fk_pagamento_cobranca FOREIGN KEY (cobranca_id) REFERENCES COBRANCA(cobranca_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela MENSAGEM (suporte / comunicação)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS MENSAGEM (
    mensagem_id INT AUTO_INCREMENT PRIMARY KEY,
    remetente_id INT NOT NULL,
    destinatario_id INT,
    assunto VARCHAR(150) NOT NULL,
    corpo TEXT NOT NULL,
    data_envio DATETIME DEFAULT CURRENT_TIMESTAMP,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    tipo ENUM('CHAMADO', 'AVISO', 'MENSAGEM_DIRETA') NOT NULL DEFAULT 'MENSAGEM_DIRETA',
    CONSTRAINT fk_mensagem_remetente FOREIGN KEY (remetente_id) REFERENCES USUARIO(usuario_id) ON DELETE CASCADE,
    CONSTRAINT fk_mensagem_destinatario FOREIGN KEY (destinatario_id) REFERENCES USUARIO(usuario_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela CHAMADO (suporte)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS CHAMADO (
    chamado_id INT AUTO_INCREMENT PRIMARY KEY,
    aluno_id INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    prioridade ENUM('BAIXA', 'MEDIA', 'ALTA', 'URGENTE') NOT NULL DEFAULT 'MEDIA',
    status ENUM('ABERTO', 'EM_ANDAMENTO', 'RESOLVIDO', 'FECHADO') NOT NULL DEFAULT 'ABERTO',
    data_abertura DATETIME DEFAULT CURRENT_TIMESTAMP,
    data_fechamento DATETIME,
    protocolo_atendimento VARCHAR(50),
    CONSTRAINT fk_chamado_aluno FOREIGN KEY (aluno_id) REFERENCES ALUNO(aluno_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Tabela PROJETO (Projeto A3 - projetos ligados aos ODS)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS PROJETO (
    projeto_id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(500) NOT NULL,
    ods TINYINT NOT NULL,
    responsavel VARCHAR(100) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    data_criacao DATE NOT NULL,
    status ENUM('PLANEJADO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO') NOT NULL DEFAULT 'PLANEJADO',
    CONSTRAINT ck_projeto_ods CHECK (ods BETWEEN 1 AND 17)
) ENGINE=InnoDB;

-- -----------------------------------------------------
-- Índices adicionais para performance (RNF-001)
-- -----------------------------------------------------
CREATE INDEX idx_usuario_email ON USUARIO(email);
CREATE INDEX idx_aluno_ra ON ALUNO(ra);
CREATE INDEX idx_turma_professor ON TURMA(professor_id);
CREATE INDEX idx_matricula_aluno ON MATRICULA(aluno_id);
CREATE INDEX idx_matricula_turma ON MATRICULA(turma_id);
CREATE INDEX idx_tarefa_turma ON TAREFA(turma_id);
CREATE INDEX idx_entrega_aluno ON ENTREGA(aluno_id);
CREATE INDEX idx_entrega_protocolo ON ENTREGA(protocolo_digital);
CREATE INDEX idx_frequencia_matricula ON FREQUENCIA(matricula_id);
CREATE INDEX idx_nota_matricula ON NOTA(matricula_id);
CREATE INDEX idx_cobranca_matricula ON COBRANCA(matricula_id);
CREATE INDEX idx_pagamento_cobranca ON PAGAMENTO(cobranca_id);
CREATE INDEX idx_chamado_aluno ON CHAMADO(aluno_id);

SELECT 'Banco de dados ulife criado com sucesso!' AS msg;
