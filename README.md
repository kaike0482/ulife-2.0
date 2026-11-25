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
