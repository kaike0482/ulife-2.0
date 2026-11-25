# Ulife 2.0 - Plataforma Acadêmica Integrada (Projeto A3)

Aplicação **desktop em Java (javax.swing) com MySQL**, com controle de acesso e o visual do protótipo
HTML "Portal do Aluno" (`prototipo/portal-do-aluno.html`).

## Requisitos do enunciado atendidos
- Interface gráfica com `javax.swing`; banco de dados **MySQL** (container Docker).
- **Controle de acesso:** só usuários autenticados entram (senha com hash BCrypt).
- **Usuários** (menu *Usuários*): cadastro, alteração, consulta e exclusão — usuário, nome, e-mail, senha, telefone.
- **Projetos** (menu *Projetos ODS*): cadastro, alteração, consulta e exclusão — código, descrição, ODS (1 a 17),
  nome do responsável, telefone, data da criação e status.
- Demais telas do portal (Aulas, Curso, Serviços, Comunicação...) reproduzem o protótipo, com conteúdo ilustrativo.

## Perfis de acesso (contas de demonstração)
| Perfil | Usuário | Senha | O que vê |
|---|---|---|---|
| Aluno | `aluno` | `aluno123` | Aulas, **Pagamentos**, Curso, Academia, Serviços, Comunicação |
| Professor | `professor` | `prof123` | Início (pendências), **Aulas** (quadro semanal das turmas dele), Minhas turmas, **Lançar notas** (A1–A3), **Lançar frequência** (sugestão automática pela videochamada) |
| Coordenador | `coordenador` | `coord123` | Início (indicadores), **Turmas** (cadastro e matrículas), **Pagamentos** (cobranças, inadimplência e recebimentos), **Usuários** (cria contas e define perfis), Projetos ODS |

O cabeçalho muda conforme o perfil: "Portal do Aluno" (botão **Pagamentos**), "Portal do Professor" (botão
**Frequência**, com as aulas a lançar) e "Portal da Coordenação" (botão **Inadimplentes**). O sino mostra as
notificações do papel de cada um.
| Coordenador (admin) | `admin` | `admin123` | Igual ao coordenador |

Também é possível entrar com o e-mail (ex.: `aluno@ulife.com`). As contas são criadas automaticamente
quando ainda não existem; contas já existentes no banco não são alteradas.

## Pagamentos do aluno (MVP)
O botão laranja **Pagamentos** fica no cabeçalho (com a quantidade de mensalidades pendentes), há um ícone
dedicado no menu lateral e um aviso com atalho nas demais telas. Na tela, o aluno vê o resumo (em aberto,
vencidas, pagas), a lista de mensalidades e paga por **PIX** (QR Code real + copia e cola no padrão BR Code) ou
**boleto** (linha digitável). O pagamento é um **ambiente de demonstração**: nenhum valor real é cobrado e o botão
"Já paguei" simula a confirmação do banco. O histórico mostra o protocolo de cada transação.

## Como rodar
1. Suba o MySQL: `docker compose up -d` (usa `docker-compose.yml`; ajuste `src/main/resources/database.properties`
   se o seu container tiver outro host/porta/senha).
2. Compile e execute (JDK 17+):
   ```
   .\mvnw.cmd -q package
   java -cp "target/classes;target/lib/*" br.com.ulife.Main
   ```
3. Ao abrir, o app **ajusta o banco automaticamente** (`SchemaInitializer`): adiciona as colunas `usuario` e
   `telefone` em `USUARIO`, cria a tabela `PROJETO` e, se o banco estiver sem usuários, cria `admin` / `admin123`.
   Em bancos já existentes, o login de cada usuário passa a ser a parte do e-mail antes do `@`
   (ex.: `admin@ulife.com` → `admin`); as senhas não mudam. Também é possível entrar com o e-mail.

Para um banco novo do zero, `bd.sql` já contém o esquema completo.

## Estrutura
- `model/` — `Usuario`, `Projeto` (com a lista dos 17 ODS)
- `dao/` — `DatabaseConnection`, `UsuarioDAO`, `ProjetoDAO`, `SchemaInitializer`
- `ui/` — `TelaLogin`, `TelaPrincipal` (cabeçalho, rail, painel flutuante, abas), `PainelUsuarios`,
  `PainelProjetos`, `QuadroAulas`, `Paginas`, `Estilo`, `Icone`
