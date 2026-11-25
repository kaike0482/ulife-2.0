# Ulife 2.0 - Plataforma Acadêmica Integrada

## Descrição
Projeto de plataforma acadêmica para ensino híbrido, integrando autenticação SSO, cronogramas, materiais didáticos, submissão de atividades, controle de frequência, notas, pagamentos (PIX/boleto) e suporte automatizado.

## Tecnologias
- **Frontend**: Java Swing (aplicação desktop)
- **Backend**: Java (JDBC) + MySQL
- **Banco de Dados**: MySQL (container Docker)
- **Requisitos**:
  - Autenticação unificada (RF-001)
  - Cronograma e renderização de aulas em até 2,0s (RF-002)
  - Upload de tarefas com protocolo digital (RF-003)
  - Registro de frequência e notas (RF-004)
  - Pagamentos via PIX (QR Code) e boleto (RF-006)

## Arquitetura
- **Camadas**:
  - Apresentação: Swing (telas de login, painel acadêmico, formulários)
  - Aplicação: Lógica de negócios (validação de upload, cálculo de média)
  - Dados: Conexão JDBC com MySQL
- **Fluxo de Autenticação**:
  1. Login SSO via App/Portal → validação no MySQL
  2. Token SSO validado pelo provedor externo
  3. Sessão iniciada para acesso aos módulos

## Próximos Passos
1. Instalar WSL2 para usar Docker Desktop
2. Configurar container MySQL via docker-compose.yml
3. Desenvolver tela de login com Swing
4. Implementar conexão com o banco de dados