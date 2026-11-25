# Preparar a apresentação na outra máquina

Registro do que fazer **antes** de apresentar o Ulife 2.0 na máquina que já tem Docker e um container antigo.

## 1. Descobrir o que existe lá

```powershell
docker ps -a --format "table {{.Names}}`t{{.Image}}`t{{.Ports}}`t{{.Status}}"
docker exec NOME mysql --version
```

(troque `NOME` pelo nome do container que aparecer)

## 2. Escolher o caminho

**Caso 1 — é o container do Ulife (`ulife-mysql`, imagem `mysql:8.0`, porta 3306).** Não precisa recriar.
- Ligar: `docker start ulife-mysql` e abrir o app.
- O app atualiza o banco antigo sozinho (testado: ~2 s, mantém os usuários).
- Cuidado com as senhas: `aluno` e `admin` continuam com **123456**. `professor` (prof123) e `coordenador` (coord123) são criados na hora.

**Caso 2 — é o Ulife, mas quero começar limpo.** Apaga os dados daquele banco!
```powershell
docker compose down -v
docker compose up -d
Get-Content bd.sql -Raw -Encoding UTF8 | docker exec -i ulife-mysql mysql -uroot -proot123 --default-character-set=utf8mb4
```
O `bd.sql` precisa ser carregado **antes** de abrir o app num banco vazio. Depois o app completa o resto e cria as contas:
aluno/aluno123, professor/prof123, coordenador/coord123, admin/admin123.

**Caso 3 — é outro projeto, outro nome ou outra imagem na porta 3306.** Não mexer nele. Dois bancos não podem usar a 3306
ao mesmo tempo; será preciso subir o do Ulife em outra porta e ajustar `src/main/resources/database.properties`.

**MySQL 5.7 ou mais antigo?** Só foi testado o 8.0 (8.0.46). Criar um container novo do Ulife e deixar o antigo parado.

## 3. Conferir na máquina da apresentação
- Java 17 instalado (`java -version`).
- Docker aberto e o container do MySQL rodando.
- Como o `mvnw` do projeto está corrompido, **compilar em outra máquina e levar a pasta `target/`** (classes + `lib`), ou instalar o Maven lá.
- Rodar: `java -cp "target/classes;target/lib/*" br.com.ulife.Main`
- Fazer um **ensaio completo** antes: login de cada perfil, entregar uma atividade, pagar uma mensalidade, lançar frequência.
- Os arquivos enviados (entregas dos alunos e materiais do professor) ficam na pasta `uploads/` **do computador onde o app roda**. Na apresentação, faça o fluxo todo na mesma máquina (aluno entrega → professor abre e corrige → professor envia material → aluno abre).
- Roteiro sugerido do fluxo novo: professor publica uma atividade e envia um material; aluno entrega; professor corrige com nota e comentário; aluno vê a nota.

## 4. Pendências em aberto (decidir depois)
- [ ] Fazer o app redefinir as senhas de demonstração (aluno123 / admin123) ao abrir, para não depender do 123456.
- [ ] Fazer o `docker-compose.yml` carregar o `bd.sql` sozinho e fixar a imagem em `mysql:8.0.46` (igual à máquina de desenvolvimento).
- [ ] Corrigir o `mvnw` (`.mvn/wrapper/maven-wrapper.jar` corrompido).
- [ ] Commitar e enviar (push): documentação em `docs/`, projeto ODS de demonstração (PRJ-001) e README.
- [ ] Abrir o `.docx` no Word, atualizar o índice (F9) e conferir as páginas do DER em paisagem.
- [ ] Capa da documentação tem 7 integrantes; o enunciado do A3 permite até 6.
- [ ] Redesenhar os diagramas de casos de uso e de autenticação (ainda mostram SSO e tesouraria).
- [ ] Gravar o vídeo e montar o roteiro da demonstração (coordenador → professor → aluno, ~5 min).
- [x] Professor publica atividades, vê e corrige as entregas e envia materiais (feito).
