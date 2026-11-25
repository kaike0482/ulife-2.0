-- Esquema completo do banco do Ulife 2.0 (MySQL 8.0), exportado do banco implementado.
-- Para um banco novo, rode o bd.sql; as tabelas de conteudo tambem sao criadas pela aplicacao na inicializacao.
CREATE DATABASE IF NOT EXISTS ulife CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ulife;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ACERVO` (
  `acervo_id` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(200) NOT NULL,
  `autor` varchar(150) NOT NULL,
  `tipo` enum('ONLINE','FISICO') NOT NULL,
  `url` varchar(500) DEFAULT NULL,
  `localizacao` varchar(150) DEFAULT NULL,
  `exemplares` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`acervo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AGENDAMENTO_AVALIACAO` (
  `horario_id` int NOT NULL,
  `aluno_id` int NOT NULL,
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`horario_id`,`aluno_id`),
  KEY `fk_agend_usr` (`aluno_id`),
  CONSTRAINT `fk_agend_hor` FOREIGN KEY (`horario_id`) REFERENCES `HORARIO_AVALIACAO` (`horario_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agend_usr` FOREIGN KEY (`aluno_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ALUNO` (
  `aluno_id` int NOT NULL,
  `ra` varchar(20) NOT NULL,
  `semestre_atual` int DEFAULT '1',
  PRIMARY KEY (`aluno_id`),
  UNIQUE KEY `ra` (`ra`),
  KEY `idx_aluno_ra` (`ra`),
  CONSTRAINT `fk_aluno_usuario` FOREIGN KEY (`aluno_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ATIVIDADE_COMPLEMENTAR` (
  `atividade_id` int NOT NULL AUTO_INCREMENT,
  `aluno_id` int NOT NULL,
  `descricao` varchar(150) NOT NULL,
  `categoria` varchar(60) NOT NULL,
  `carga_horaria` int NOT NULL,
  `status` enum('EM_ANALISE','VALIDADA','REJEITADA') NOT NULL DEFAULT 'EM_ANALISE',
  `comprovante` varchar(500) DEFAULT NULL,
  `parecer` varchar(300) DEFAULT NULL,
  `registrado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`atividade_id`),
  KEY `fk_atcomp_usr` (`aluno_id`),
  CONSTRAINT `fk_atcomp_usr` FOREIGN KEY (`aluno_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AVALIACAO` (
  `avaliacao_id` int NOT NULL AUTO_INCREMENT,
  `turma_id` int NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `descricao` varchar(150) NOT NULL,
  `agendar_ate` date NOT NULL,
  PRIMARY KEY (`avaliacao_id`),
  KEY `fk_aval_turma` (`turma_id`),
  CONSTRAINT `fk_aval_turma` FOREIGN KEY (`turma_id`) REFERENCES `TURMA` (`turma_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CHAMADO` (
  `chamado_id` int NOT NULL AUTO_INCREMENT,
  `aluno_id` int NOT NULL,
  `titulo` varchar(150) NOT NULL,
  `descricao` text NOT NULL,
  `categoria` varchar(50) NOT NULL,
  `prioridade` enum('BAIXA','MEDIA','ALTA','URGENTE') NOT NULL DEFAULT 'MEDIA',
  `status` enum('ABERTO','EM_ANDAMENTO','RESOLVIDO','FECHADO') NOT NULL DEFAULT 'ABERTO',
  `data_abertura` datetime DEFAULT CURRENT_TIMESTAMP,
  `data_fechamento` datetime DEFAULT NULL,
  `protocolo_atendimento` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`chamado_id`),
  KEY `idx_chamado_aluno` (`aluno_id`),
  CONSTRAINT `fk_chamado_aluno` FOREIGN KEY (`aluno_id`) REFERENCES `ALUNO` (`aluno_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `COBRANCA` (
  `cobranca_id` int NOT NULL AUTO_INCREMENT,
  `matricula_id` int DEFAULT NULL,
  `usuario_id` int DEFAULT NULL,
  `descricao` varchar(150) NOT NULL,
  `valor` decimal(10,2) NOT NULL,
  `vencimento` date NOT NULL,
  `status` enum('ABERTA','PAGA','VENCIDA','CANCELADA') NOT NULL DEFAULT 'ABERTA',
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cobranca_id`),
  KEY `idx_cobranca_matricula` (`matricula_id`),
  KEY `fk_cobranca_usuario` (`usuario_id`),
  CONSTRAINT `fk_cobranca_matricula` FOREIGN KEY (`matricula_id`) REFERENCES `MATRICULA` (`matricula_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cobranca_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `DOCUMENTO_EMITIDO` (
  `documento_id` int NOT NULL AUTO_INCREMENT,
  `aluno_id` int NOT NULL,
  `tipo` varchar(40) NOT NULL,
  `codigo` varchar(40) NOT NULL,
  `emitido_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`documento_id`),
  UNIQUE KEY `codigo` (`codigo`),
  KEY `fk_doc_usr` (`aluno_id`),
  CONSTRAINT `fk_doc_usr` FOREIGN KEY (`aluno_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ENTREGA` (
  `entrega_id` int NOT NULL AUTO_INCREMENT,
  `tarefa_id` int NOT NULL,
  `aluno_id` int NOT NULL,
  `protocolo_digital` varchar(50) NOT NULL,
  `arquivo_nome` varchar(255) DEFAULT NULL,
  `arquivo_caminho` varchar(500) DEFAULT NULL,
  `tamanho_arquivo_bytes` bigint DEFAULT NULL,
  `data_entrega` datetime DEFAULT CURRENT_TIMESTAMP,
  `prazo_valido` tinyint(1) NOT NULL DEFAULT '1',
  `status` enum('ENTREGUE','INVALIDO','PRAZO_EXPIRADO','CORRIGIDO') NOT NULL DEFAULT 'ENTREGUE',
  `nota` decimal(4,2) DEFAULT NULL,
  `feedback` text,
  PRIMARY KEY (`entrega_id`),
  UNIQUE KEY `protocolo_digital` (`protocolo_digital`),
  KEY `fk_entrega_tarefa` (`tarefa_id`),
  KEY `idx_entrega_aluno` (`aluno_id`),
  KEY `idx_entrega_protocolo` (`protocolo_digital`),
  CONSTRAINT `fk_entrega_aluno` FOREIGN KEY (`aluno_id`) REFERENCES `ALUNO` (`aluno_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_entrega_tarefa` FOREIGN KEY (`tarefa_id`) REFERENCES `TAREFA` (`tarefa_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `EXTENSAO` (
  `extensao_id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(150) NOT NULL,
  `descricao` varchar(300) DEFAULT NULL,
  `carga_horaria` int NOT NULL,
  `inscricoes_ate` date NOT NULL,
  `vagas` int NOT NULL,
  PRIMARY KEY (`extensao_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FREQUENCIA` (
  `frequencia_id` int NOT NULL AUTO_INCREMENT,
  `matricula_id` int NOT NULL,
  `aula_data` date NOT NULL,
  `presenca` tinyint(1) NOT NULL DEFAULT '0',
  `justificativa` varchar(200) DEFAULT NULL,
  `minutos_conectado` int DEFAULT NULL,
  `lancada` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`frequencia_id`),
  UNIQUE KEY `uk_matricula_data` (`matricula_id`,`aula_data`),
  KEY `idx_frequencia_matricula` (`matricula_id`),
  CONSTRAINT `fk_frequencia_matricula` FOREIGN KEY (`matricula_id`) REFERENCES `MATRICULA` (`matricula_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `GRAVACAO` (
  `gravacao_id` int NOT NULL AUTO_INCREMENT,
  `turma_id` int NOT NULL,
  `aula_data` date NOT NULL,
  `titulo` varchar(150) NOT NULL,
  `url` varchar(500) NOT NULL,
  PRIMARY KEY (`gravacao_id`),
  KEY `fk_grav_turma` (`turma_id`),
  CONSTRAINT `fk_grav_turma` FOREIGN KEY (`turma_id`) REFERENCES `TURMA` (`turma_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `HORARIO_AVALIACAO` (
  `horario_id` int NOT NULL AUTO_INCREMENT,
  `avaliacao_id` int NOT NULL,
  `data_hora` datetime NOT NULL,
  `vagas` int NOT NULL DEFAULT '10',
  PRIMARY KEY (`horario_id`),
  KEY `fk_hor_aval` (`avaliacao_id`),
  CONSTRAINT `fk_hor_aval` FOREIGN KEY (`avaliacao_id`) REFERENCES `AVALIACAO` (`avaliacao_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `INSCRICAO_EXTENSAO` (
  `extensao_id` int NOT NULL,
  `aluno_id` int NOT NULL,
  `status` enum('INSCRITO','CONCLUIDO','CANCELADO') NOT NULL DEFAULT 'INSCRITO',
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`extensao_id`,`aluno_id`),
  KEY `fk_insc_usr` (`aluno_id`),
  CONSTRAINT `fk_insc_ext` FOREIGN KEY (`extensao_id`) REFERENCES `EXTENSAO` (`extensao_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_insc_usr` FOREIGN KEY (`aluno_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MATERIAL` (
  `material_id` int NOT NULL AUTO_INCREMENT,
  `turma_id` int NOT NULL,
  `titulo` varchar(150) NOT NULL,
  `arquivo_nome` varchar(255) NOT NULL,
  `arquivo_caminho` varchar(500) NOT NULL,
  `tamanho_bytes` bigint NOT NULL DEFAULT '0',
  `publicado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`material_id`),
  KEY `fk_material_turma` (`turma_id`),
  CONSTRAINT `fk_material_turma` FOREIGN KEY (`turma_id`) REFERENCES `TURMA` (`turma_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MATRICULA` (
  `matricula_id` int NOT NULL AUTO_INCREMENT,
  `aluno_id` int NOT NULL,
  `turma_id` int NOT NULL,
  `data_matricula` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` enum('ATIVA','TRANCADA','CONCLUIDA','CANCELADA') NOT NULL DEFAULT 'ATIVA',
  PRIMARY KEY (`matricula_id`),
  UNIQUE KEY `uk_aluno_turma` (`aluno_id`,`turma_id`),
  KEY `idx_matricula_aluno` (`aluno_id`),
  KEY `idx_matricula_turma` (`turma_id`),
  CONSTRAINT `fk_matricula_aluno` FOREIGN KEY (`aluno_id`) REFERENCES `ALUNO` (`aluno_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_matricula_turma` FOREIGN KEY (`turma_id`) REFERENCES `TURMA` (`turma_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MENSAGEM` (
  `mensagem_id` int NOT NULL AUTO_INCREMENT,
  `remetente_id` int NOT NULL,
  `destinatario_id` int DEFAULT NULL,
  `assunto` varchar(150) NOT NULL,
  `corpo` text NOT NULL,
  `data_envio` datetime DEFAULT CURRENT_TIMESTAMP,
  `lida` tinyint(1) NOT NULL DEFAULT '0',
  `tipo` enum('CHAMADO','AVISO','MENSAGEM_DIRETA') NOT NULL DEFAULT 'MENSAGEM_DIRETA',
  PRIMARY KEY (`mensagem_id`),
  KEY `fk_mensagem_remetente` (`remetente_id`),
  KEY `fk_mensagem_destinatario` (`destinatario_id`),
  CONSTRAINT `fk_mensagem_destinatario` FOREIGN KEY (`destinatario_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_mensagem_remetente` FOREIGN KEY (`remetente_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `NOTA` (
  `nota_id` int NOT NULL AUTO_INCREMENT,
  `matricula_id` int NOT NULL,
  `tarefa_id` int DEFAULT NULL,
  `valor` decimal(4,2) NOT NULL,
  `tipo_avaliacao` varchar(20) NOT NULL,
  `data_lancamento` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`nota_id`),
  UNIQUE KEY `uk_nota_aval` (`matricula_id`,`tipo_avaliacao`),
  KEY `fk_nota_tarefa` (`tarefa_id`),
  KEY `idx_nota_matricula` (`matricula_id`),
  CONSTRAINT `fk_nota_matricula` FOREIGN KEY (`matricula_id`) REFERENCES `MATRICULA` (`matricula_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nota_tarefa` FOREIGN KEY (`tarefa_id`) REFERENCES `TAREFA` (`tarefa_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PAGAMENTO` (
  `pagamento_id` int NOT NULL AUTO_INCREMENT,
  `cobranca_id` int NOT NULL,
  `metodo` enum('PIX','BOLETO') NOT NULL,
  `valor_pago` decimal(10,2) NOT NULL,
  `data_pagamento` datetime DEFAULT CURRENT_TIMESTAMP,
  `qr_code_pix` text,
  `boleto_codigo_barras` varchar(100) DEFAULT NULL,
  `transacao_id` varchar(100) DEFAULT NULL,
  `status` enum('PROCESSANDO','CONFIRMADO','REJEITADO','EXPIRADO') NOT NULL DEFAULT 'PROCESSANDO',
  PRIMARY KEY (`pagamento_id`),
  KEY `idx_pagamento_cobranca` (`cobranca_id`),
  CONSTRAINT `fk_pagamento_cobranca` FOREIGN KEY (`cobranca_id`) REFERENCES `COBRANCA` (`cobranca_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PLANO_ENSINO` (
  `turma_id` int NOT NULL,
  `ementa` text,
  `objetivos` text,
  `conteudo` text,
  `criterios` text,
  `bibliografia` text,
  PRIMARY KEY (`turma_id`),
  CONSTRAINT `fk_plano_turma` FOREIGN KEY (`turma_id`) REFERENCES `TURMA` (`turma_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PROFESSOR` (
  `professor_id` int NOT NULL,
  `matricula_funcional` varchar(20) NOT NULL,
  `departamento` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`professor_id`),
  UNIQUE KEY `matricula_funcional` (`matricula_funcional`),
  CONSTRAINT `fk_professor_usuario` FOREIGN KEY (`professor_id`) REFERENCES `USUARIO` (`usuario_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PROJETO` (
  `projeto_id` int NOT NULL AUTO_INCREMENT,
  `codigo` varchar(20) NOT NULL,
  `descricao` varchar(500) NOT NULL,
  `ods` tinyint NOT NULL,
  `responsavel` varchar(100) NOT NULL,
  `telefone` varchar(20) NOT NULL,
  `data_criacao` date NOT NULL,
  `status` enum('PLANEJADO','EM_ANDAMENTO','CONCLUIDO','CANCELADO') NOT NULL DEFAULT 'PLANEJADO',
  PRIMARY KEY (`projeto_id`),
  UNIQUE KEY `codigo` (`codigo`),
  CONSTRAINT `ck_projeto_ods` CHECK ((`ods` between 1 and 17))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `TAREFA` (
  `tarefa_id` int NOT NULL AUTO_INCREMENT,
  `turma_id` int NOT NULL,
  `titulo` varchar(150) NOT NULL,
  `descricao` text,
  `tipo` varchar(50) NOT NULL,
  `arquivo_obrigatorio` tinyint(1) NOT NULL DEFAULT '0',
  `tamanho_max_mb` int DEFAULT '50',
  `data_publicacao` datetime DEFAULT CURRENT_TIMESTAMP,
  `prazo_entrega` datetime NOT NULL,
  `peso` decimal(4,2) DEFAULT '0.00',
  `ativa` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`tarefa_id`),
  KEY `idx_tarefa_turma` (`turma_id`),
  CONSTRAINT `fk_tarefa_turma` FOREIGN KEY (`turma_id`) REFERENCES `TURMA` (`turma_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `TURMA` (
  `turma_id` int NOT NULL AUTO_INCREMENT,
  `codigo` varchar(20) NOT NULL,
  `nome` varchar(100) NOT NULL,
  `descricao` text,
  `professor_id` int NOT NULL,
  `semestre` varchar(10) NOT NULL,
  `ano` int NOT NULL,
  `carga_horaria_total` int DEFAULT '60',
  `ativo` tinyint(1) NOT NULL DEFAULT '1',
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  `dia_semana` tinyint DEFAULT NULL,
  `hora_inicio` time NOT NULL DEFAULT '19:00:00',
  `hora_fim` time NOT NULL DEFAULT '21:50:00',
  `modalidade` enum('PRESENCIAL','DIGITAL') NOT NULL DEFAULT 'PRESENCIAL',
  `local_aula` varchar(150) DEFAULT NULL,
  `uc_id` int DEFAULT NULL,
  PRIMARY KEY (`turma_id`),
  UNIQUE KEY `codigo` (`codigo`),
  KEY `idx_turma_professor` (`professor_id`),
  CONSTRAINT `fk_turma_professor` FOREIGN KEY (`professor_id`) REFERENCES `PROFESSOR` (`professor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `UNIDADE_CURRICULAR` (
  `uc_id` int NOT NULL AUTO_INCREMENT,
  `semestre` int NOT NULL,
  `nome` varchar(150) NOT NULL,
  `carga_horaria` int NOT NULL DEFAULT '60',
  PRIMARY KEY (`uc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USUARIO` (
  `usuario_id` int NOT NULL AUTO_INCREMENT,
  `usuario` varchar(30) NOT NULL,
  `nome` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `telefone` varchar(20) DEFAULT NULL,
  `senha_hash` varchar(255) NOT NULL,
  `perfil` enum('ALUNO','PROFESSOR','COORDENADOR') NOT NULL DEFAULT 'ALUNO',
  `ativo` tinyint(1) NOT NULL DEFAULT '1',
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`usuario_id`),
  UNIQUE KEY `uk_usuario_login` (`usuario`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_usuario_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

