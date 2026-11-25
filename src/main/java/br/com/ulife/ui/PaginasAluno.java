package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.PortalAlunoDAO;
import br.com.ulife.dao.PortalAlunoDAO.*;
import br.com.ulife.model.Usuario;
import br.com.ulife.service.DocumentoHtml;
import br.com.ulife.ui.Icone.Tipo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Telas do aluno nas seções Aulas e Curso, com dados reais do banco. */
public final class PaginasAluno {

    private PaginasAluno() {}

    private static final PortalAlunoDAO DAO = new PortalAlunoDAO();
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int META_COMPLEMENTARES = 100;
    private static final int LIMITE_HIBRIDEZ = 40;
    private static final int MIN_HORAS_PARTICIPACAO = 20;

    // ------------------------------------------------------------------ blocos reutilizáveis

    private static JComponent linha(Tipo ic, String titulo, String sub, boolean primeira, JComponent... direita) {
        JPanel l = new JPanel(new BorderLayout(16, 0));
        l.setOpaque(false);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(primeira ? 0 : 1, 0, 0, 0, Estilo.BD),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        l.add(new JLabel(new Icone(ic, 26, Estilo.AC)), BorderLayout.WEST);
        JPanel t = new JPanel(new GridLayout(2, 1));
        t.setOpaque(false);
        t.add(Estilo.rotulo(titulo, Font.BOLD, 15, Estilo.TX));
        t.add(Estilo.rotulo(sub, Font.PLAIN, 13, Estilo.MU));
        l.add(t, BorderLayout.CENTER);
        JPanel d = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        d.setOpaque(false);
        for (JComponent c : direita) {
            d.add(c);
        }
        l.add(d, BorderLayout.EAST);
        return l;
    }

    private static JComponent lista(List<JComponent> linhas, String vazio) {
        Estilo.Cartao c = new Estilo.Cartao(new GridLayout(0, 1), 0);
        if (linhas.isEmpty()) {
            JLabel l = Estilo.rotulo(vazio, Font.PLAIN, 14, Estilo.MU);
            l.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
            c.add(l);
        }
        linhas.forEach(c::add);
        return c;
    }

    private static JComponent grade(List<JComponent> itens, String vazio) {
        if (itens.isEmpty()) {
            return Estilo.rotulo(vazio, Font.PLAIN, 14, Estilo.MU);
        }
        JPanel g = new JPanel(new GridLayout(0, 3, 16, 16));
        g.setOpaque(false);
        itens.forEach(g::add);
        while (g.getComponentCount() % 3 != 0) {
            JPanel v = new JPanel();
            v.setOpaque(false);
            g.add(v);
        }
        return g;
    }

    /** Cartão: ícone, título, linhas de texto e uma ação opcional no rodapé. */
    private static JComponent cartao(Tipo ic, String titulo, JComponent acao, String... textos) {
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 8), 20);
        c.add(new JLabel(new Icone(ic, 28, Estilo.AC)), BorderLayout.NORTH);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        JLabel a = Estilo.texto(titulo, 240, Font.BOLD, 16, Estilo.TX);
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.add(a);
        for (String s : textos) {
            t.add(Box.createVerticalStrut(5));
            JLabel b = Estilo.texto(s, 240, Font.PLAIN, 13, Estilo.MU);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            t.add(b);
        }
        c.add(t, BorderLayout.CENTER);
        if (acao != null) {
            JPanel s = Estilo.acoes(0);
            s.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            s.add(acao);
            c.add(s, BorderLayout.SOUTH);
        }
        return c;
    }

    private static JComponent secao(String texto) {
        JLabel l = Estilo.rotulo(texto, Font.BOLD, 18, Estilo.TX);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return l;
    }

    private static JComponent pilha(JComponent... itens) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (int i = 0; i < itens.length; i++) {
            itens[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(itens[i]);
            if (i < itens.length - 1) {
                p.add(Box.createVerticalStrut(24));
            }
        }
        return p;
    }

    private static void abrirUrl(Component pai, String url) {
        try {
            Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            Estilo.erro(pai, "Não foi possível abrir o link: " + url);
        }
    }

    private static void abrirArquivo(Component pai, Path p) {
        try {
            Desktop.getDesktop().browse(p.toAbsolutePath().toUri());
        } catch (Exception e) {
            Estilo.erro(pai, "Não foi possível abrir o documento: " + p);
        }
    }

    private static String nota(BigDecimal v) {
        if (v == null) return "—";
        BigDecimal s = v.stripTrailingZeros();
        return (s.scale() < 1 ? s.setScale(1) : s).toPlainString().replace('.', ',');
    }

    private static JComponent comErro(Supplier<JComponent> s) {
        try {
            return s.get();
        } catch (RuntimeException ex) {
            return Estilo.rotulo("Erro ao carregar: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO);
        }
    }

    private static String ra(Usuario u) {
        return DAO.ra(u.getUsuarioId());
    }

    // ------------------------------------------------------------------ AULAS

    public static JComponent quadro(Usuario u) {
        return comErro(() -> new QuadroAulasAluno(u));
    }

    public static JComponent gravadas(Usuario u) {
        return comErro(() -> {
            List<JComponent> l = new ArrayList<>();
            for (Gravacao g : DAO.gravacoes(u.getUsuarioId())) {
                Estilo.Botao b = Estilo.primario("Assistir");
                b.addActionListener(e -> abrirUrl(b, g.url()));
                l.add(cartao(Tipo.MON, g.turma(), b, "Aula de " + g.data().format(DATA)));
            }
            return grade(l, "Ainda não há aulas gravadas das suas turmas.");
        });
    }

    public static JComponent ucs(Usuario u) {
        return comErro(() -> {
            List<JComponent> l = new ArrayList<>();
            for (UC uc : DAO.ucs(u.getUsuarioId())) {
                Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 8), 20);
                c.add(new JLabel(new Icone(Tipo.CAP, 28, Estilo.AC)), BorderLayout.NORTH);
                JPanel t = new JPanel();
                t.setOpaque(false);
                t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
                JLabel a = Estilo.texto(uc.turma(), 240, Font.BOLD, 16, Estilo.TX);
                JLabel b = Estilo.texto(uc.professor(), 240, Font.PLAIN, 13, Estilo.MU);
                JComponent barra = Estilo.barra(uc.progresso(), Estilo.AC);
                JLabel n = Estilo.rotulo(uc.progresso() + "% do conteúdo · " + Math.round(uc.horasMinistradas())
                        + "h de " + uc.cargaHoraria() + "h", Font.PLAIN, 13, Estilo.MU);
                for (JComponent j : new JComponent[]{a, b, barra, n}) {
                    j.setAlignmentX(Component.LEFT_ALIGNMENT);
                }
                t.add(a);
                t.add(Box.createVerticalStrut(5));
                t.add(b);
                t.add(Box.createVerticalStrut(12));
                t.add(barra);
                t.add(Box.createVerticalStrut(6));
                t.add(n);
                c.add(t, BorderLayout.CENTER);
                l.add(c);
            }
            return grade(l, "Você ainda não está matriculado em nenhuma turma.");
        });
    }

    public static JComponent atividades(Usuario u) {
        return new Recarregavel(recarregar -> comErro(() -> {
            List<JComponent> l = new ArrayList<>();
            List<Atividade> lista = DAO.atividades(u.getUsuarioId());
            for (int i = 0; i < lista.size(); i++) {
                Atividade a = lista.get(i);
                JLabel info = Estilo.rotulo("Prazo " + a.prazo().format(DATA_HORA) + " · peso " + nota(a.peso()),
                        Font.PLAIN, 13, Estilo.MU);
                JComponent acao;
                if (a.protocolo() != null) {
                    acao = Estilo.rotulo("Protocolo " + a.protocolo(), Font.PLAIN, 12, Estilo.MU);
                } else if ("Atrasada".equals(a.status())) {
                    acao = Estilo.rotulo("Prazo expirado", Font.PLAIN, 13, Estilo.ERRO);
                } else {
                    Estilo.Botao b = Estilo.primario("Entregar");
                    b.addActionListener(e -> entregar(b, u, a, recarregar));
                    acao = b;
                }
                l.add(linha(Tipo.DOC, a.titulo() + "  ·  " + a.turma(), a.descricao() == null ? "" : resumo(a.descricao()),
                        i == 0, info, new Estilo.Tag(a.status()), acao));
            }
            return lista(l, "Nenhuma atividade publicada para as suas turmas.");
        }));
    }

    private static String resumo(String s) {
        return s.length() > 90 ? s.substring(0, 87) + "..." : s;
    }

    private static void entregar(Component pai, Usuario u, Atividade a, Runnable recarregar) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Escolha o arquivo da atividade \"" + a.titulo() + "\"");
        if (fc.showOpenDialog(pai) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String protocolo = DAO.entregar(a.tarefaId(), u.getUsuarioId(), fc.getSelectedFile());
            Estilo.aviso(pai, "Atividade entregue com sucesso!\nProtocolo digital: " + protocolo);
            recarregar.run();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Estilo.erro(pai, ex.getMessage());
        } catch (RuntimeException ex) {
            Estilo.erro(pai, ex.getMessage());
        }
    }

    public static JComponent plano(Usuario u) {
        return comErro(() -> {
            List<JComponent> l = new ArrayList<>();
            for (Plano p : DAO.planos(u.getUsuarioId())) {
                Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 10), 22);
                JPanel cab = new JPanel(new GridLayout(2, 1));
                cab.setOpaque(false);
                cab.add(Estilo.rotulo(p.turma(), Font.BOLD, 17, Estilo.TX));
                cab.add(Estilo.rotulo(p.professor(), Font.PLAIN, 13, Estilo.MU));
                c.add(cab, BorderLayout.NORTH);
                JPanel corpo = new JPanel();
                corpo.setOpaque(false);
                corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
                String[][] partes = {{"Ementa", p.ementa()}, {"Objetivos", p.objetivos()}, {"Conteúdo programático", p.conteudo()},
                    {"Critérios de avaliação", p.criterios()}, {"Bibliografia", p.bibliografia()}};
                for (String[] s : partes) {
                    JLabel t = Estilo.rotulo(s[0], Font.BOLD, 13, Estilo.P);
                    t.setAlignmentX(Component.LEFT_ALIGNMENT);
                    t.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
                    JLabel x = Estilo.texto(s[1] == null ? "—" : s[1], 700, Font.PLAIN, 13, Estilo.TX);
                    x.setAlignmentX(Component.LEFT_ALIGNMENT);
                    corpo.add(t);
                    corpo.add(x);
                }
                c.add(corpo, BorderLayout.CENTER);
                l.add(c);
            }
            if (l.isEmpty()) {
                return Estilo.rotulo("Nenhum plano de ensino publicado para as suas turmas.", Font.PLAIN, 14, Estilo.MU);
            }
            JPanel p = new JPanel(new GridLayout(0, 1, 0, 16));
            p.setOpaque(false);
            l.forEach(p::add);
            return p;
        });
    }

    public static JComponent agendamento(Usuario u) {
        return new Recarregavel(recarregar -> comErro(() -> {
            List<Avaliacao> lista = DAO.avaliacoes(u.getUsuarioId());
            if (lista.isEmpty()) {
                return Estilo.rotulo("Nenhuma avaliação para agendar nas suas turmas.", Font.PLAIN, 14, Estilo.MU);
            }
            JPanel p = new JPanel(new GridLayout(0, 1, 0, 16));
            p.setOpaque(false);
            for (Avaliacao av : lista) {
                boolean aberto = !av.agendarAte().isBefore(LocalDate.now());
                List<JComponent> linhas = new ArrayList<>();
                JPanel cab = new JPanel(new BorderLayout());
                cab.setOpaque(false);
                cab.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
                JPanel tt = new JPanel(new GridLayout(2, 1));
                tt.setOpaque(false);
                tt.add(Estilo.rotulo(av.descricao(), Font.BOLD, 16, Estilo.TX));
                tt.add(Estilo.rotulo(aberto ? "Agende até " + av.agendarAte().format(DATA) : "Prazo de agendamento encerrado",
                        Font.PLAIN, 13, aberto ? Estilo.MU : Estilo.ERRO));
                cab.add(tt, BorderLayout.CENTER);
                linhas.add(cab);
                for (Horario h : av.horarios()) {
                    int livres = h.vagas() - h.ocupadas();
                    JComponent acao;
                    JComponent tag;
                    if (h.meu()) {
                        tag = new Estilo.Tag("Agendado");
                        Estilo.Botao b = Estilo.perigo("Cancelar");
                        b.setEnabled(aberto);
                        b.addActionListener(e -> {
                            DAO.cancelarAgendamento(h.horarioId(), u.getUsuarioId());
                            recarregar.run();
                        });
                        acao = b;
                    } else {
                        tag = Estilo.rotulo(livres + (livres == 1 ? " vaga" : " vagas"), Font.PLAIN, 13,
                                livres > 0 ? Estilo.MU : Estilo.ERRO);
                        Estilo.Botao b = Estilo.primario("Agendar");
                        b.setEnabled(aberto && livres > 0);
                        b.addActionListener(e -> {
                            try {
                                DAO.agendar(h.horarioId(), u.getUsuarioId());
                                recarregar.run();
                            } catch (RuntimeException ex) {
                                Estilo.erro(b, ex.getMessage());
                            }
                        });
                        acao = b;
                    }
                    linhas.add(linha(Tipo.CAL, h.dataHora().format(DATA_HORA), "Horário da prova", false, tag, acao));
                }
                p.add(lista(linhas, ""));
            }
            return p;
        }));
    }

    public static JComponent biblioteca(Usuario u, boolean online) {
        String tipo = online ? "ONLINE" : "FISICO";
        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setOpaque(false);
        JTextField busca = Estilo.campo();
        JPanel barra = new JPanel(new BorderLayout(8, 0));
        barra.setOpaque(false);
        barra.add(Estilo.rotulo("Buscar:", Font.PLAIN, 14, Estilo.MU), BorderLayout.WEST);
        barra.add(busca, BorderLayout.CENTER);
        JPanel resultado = new JPanel(new BorderLayout());
        resultado.setOpaque(false);
        Runnable atualizar = () -> {
            resultado.removeAll();
            resultado.add(comErro(() -> {
                List<JComponent> l = new ArrayList<>();
                List<ItemAcervo> itens = DAO.acervo(tipo, busca.getText());
                for (int i = 0; i < itens.size(); i++) {
                    ItemAcervo it = itens.get(i);
                    if (online) {
                        Estilo.Botao b = Estilo.primario("Acessar");
                        b.addActionListener(e -> abrirUrl(b, it.url()));
                        l.add(linha(Tipo.LIB, it.titulo(), it.autor(), i == 0, b));
                    } else {
                        boolean disp = it.exemplares() > 0;
                        l.add(linha(Tipo.BOOK, it.titulo(), it.autor() + " · " + it.localizacao(), i == 0,
                                Estilo.rotulo(it.exemplares() + (it.exemplares() == 1 ? " exemplar" : " exemplares"),
                                        Font.PLAIN, 13, Estilo.MU),
                                new Estilo.Tag(disp ? "Disponível" : "Indisponível")));
                    }
                }
                return lista(l, "Nenhum item encontrado.");
            }), BorderLayout.CENTER);
            resultado.revalidate();
            resultado.repaint();
        };
        busca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { atualizar.run(); }
            public void removeUpdate(DocumentEvent e) { atualizar.run(); }
            public void changedUpdate(DocumentEvent e) { atualizar.run(); }
        });
        raiz.add(barra, BorderLayout.NORTH);
        raiz.add(resultado, BorderLayout.CENTER);
        atualizar.run();
        return raiz;
    }

    // ------------------------------------------------------------------ CURSO

    /** Gera o documento, registra a emissão e abre no navegador (para imprimir ou salvar em PDF). */
    private static void emitir(Component pai, Usuario u, String tipo, String titulo, java.util.function.Function<Void, String> corpo,
                               Runnable recarregar) {
        try {
            Documento d = DAO.registrarDocumento(u.getUsuarioId(), tipo);
            String html = DocumentoHtml.pagina(titulo, corpo.apply(null), d.codigo(), d.emitidoEm());
            Path p = DocumentoHtml.salvar(d.codigo(), html);
            abrirArquivo(pai, p);
            recarregar.run();
        } catch (RuntimeException ex) {
            Estilo.erro(pai, ex.getMessage());
        }
    }

    private static JComponent documentosEmitidos(Usuario u, String... tipos) {
        List<JComponent> l = new ArrayList<>();
        List<String> filtro = List.of(tipos);
        int n = 0;
        for (Documento d : DAO.documentos(u.getUsuarioId())) {
            if (!filtro.contains(d.tipo())) {
                continue;
            }
            Path p = Path.of("documentos", d.codigo() + ".html");
            Estilo.Botao b = Estilo.neutro("Abrir");
            b.setEnabled(Files.exists(p));
            b.addActionListener(e -> abrirArquivo(b, p));
            l.add(linha(Tipo.DOC, d.tipo(), "Código " + d.codigo() + " · emitido em " + d.emitidoEm().format(DATA_HORA),
                    n++ == 0, b));
        }
        return pilha(secao("Documentos emitidos"), lista(l, "Nenhum documento emitido ainda."));
    }

    public static JComponent certificados(Usuario u) {
        return new Recarregavel(recarregar -> comErro(() -> {
            int id = u.getUsuarioId();
            int horas = DAO.horasComplementaresValidadas(id);
            int ext = DAO.extensoesConcluidas(id);
            boolean concluido = DAO.curriculoCompleto(id);
            Object[][] tipos = {
                {"Certificado de Conclusão de Curso", concluido, "Disponível após concluir todas as unidades do plano curricular.",
                    "Concluiu todas as unidades curriculares do curso de " + DocumentoHtml.CURSO + "."},
                {"Certificado de Participação", horas >= MIN_HORAS_PARTICIPACAO,
                    "Requer " + MIN_HORAS_PARTICIPACAO + "h de atividades complementares validadas (você tem " + horas + "h).",
                    "Participou de atividades acadêmicas complementares, totalizando " + horas + " horas validadas."},
                {"Certificado NEST/HUB", ext >= 1, "Requer um programa de extensão concluído (você tem " + ext + ").",
                    "Concluiu programas de extensão e inovação NEST/HUB."}};
            List<JComponent> cards = new ArrayList<>();
            for (Object[] t : tipos) {
                boolean ok = (Boolean) t[1];
                Estilo.Botao b = Estilo.primario("Emitir certificado");
                b.setEnabled(ok);
                b.addActionListener(e -> emitir(b, u, (String) t[0], (String) t[0],
                        x -> DocumentoHtml.certificado((String) t[0], u.getNome(), ra(u), (String) t[3]), recarregar));
                cards.add(cartao(Tipo.DOC, (String) t[0], b, ok ? "Disponível para emissão." : "Ainda indisponível.", (String) t[2]));
            }
            return pilha(grade(cards, ""), documentosEmitidos(u, "Certificado de Conclusão de Curso",
                    "Certificado de Participação", "Certificado NEST/HUB"));
        }));
    }

    public static JComponent historico(Usuario u) {
        return new Recarregavel(recarregar -> comErro(() -> {
            List<AcademicoDAO.Boletim> bol = new AcademicoDAO().boletim(u.getUsuarioId());
            DefaultTableModel m = new DefaultTableModel(new String[]{"Unidade de curso", "A1", "A2", "A3", "Média", "Frequência",
                "Situação"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            List<String[]> linhasDoc = new ArrayList<>();
            for (AcademicoDAO.Boletim b : bol) {
                BigDecimal media = b.media();
                Double f = b.frequencia();
                String sit = f != null && f < 75 ? "Frequência baixa" : media == null ? "Em andamento"
                        : media.compareTo(AcademicoDAO.MEDIA_APROVACAO) >= 0 ? "Aprovado" : "Reprovado";
                String[] linha = {b.turma, nota(b.notas.get("A1")), nota(b.notas.get("A2")), nota(b.notas.get("A3")),
                    nota(media), f == null ? "—" : String.format("%.0f%%", f), sit};
                linhasDoc.add(linha);
                m.addRow((Object[]) linha);
            }
            Estilo.Botao emitir = Estilo.primario("Emitir histórico escolar");
            emitir.addActionListener(e -> emitir(emitir, u, "Histórico Escolar", "Histórico Escolar",
                    x -> DocumentoHtml.historico(u.getNome(), ra(u), linhasDoc), recarregar));
            JTable t = new JTable(m);
            Estilo.estilizarTabela(t, "Situação");
            JPanel acoes = Estilo.acoes(0);
            acoes.add(emitir);
            JComponent tabela = bol.isEmpty()
                    ? Estilo.rotulo("Você ainda não tem unidades cursadas neste semestre.", Font.PLAIN, 14, Estilo.MU)
                    : Estilo.tabelaSemRolagem(t);
            return pilha(secao("Semestre em curso"), tabela, acoes, documentosEmitidos(u, "Histórico Escolar"));
        }));
    }

    public static JComponent extensao(Usuario u) {
        return new Recarregavel(recarregar -> comErro(() -> {
            List<JComponent> l = new ArrayList<>();
            for (Extensao e : DAO.extensao(u.getUsuarioId())) {
                String st = e.minhaInscricao();
                JComponent acao;
                Estilo.Botao b;
                if ("CONCLUIDO".equals(st)) {
                    acao = new Estilo.Tag("Concluído");
                } else if ("INSCRITO".equals(st)) {
                    b = Estilo.perigo("Cancelar inscrição");
                    b.addActionListener(ev -> {
                        DAO.cancelarExtensao(e.id(), u.getUsuarioId());
                        recarregar.run();
                    });
                    acao = b;
                } else {
                    b = Estilo.primario("Inscrever-se");
                    b.addActionListener(ev -> {
                        try {
                            DAO.inscreverExtensao(e.id(), u.getUsuarioId());
                            recarregar.run();
                        } catch (RuntimeException ex) {
                            Estilo.erro(ev.getSource() instanceof Component c ? c : null, ex.getMessage());
                        }
                    });
                    acao = b;
                }
                l.add(cartao(Tipo.STAR, e.nome(), acao, e.descricao() == null ? "" : e.descricao(),
                        e.horas() + "h · " + e.inscritos() + "/" + e.vagas() + " vagas",
                        "Inscrições até " + e.ate().format(DATA) + ("INSCRITO".equals(st) ? " · você está inscrito" : "")));
            }
            return grade(l, "Nenhum programa de extensão aberto.");
        }));
    }

    public static JComponent atividadesComplementares(Usuario u) {
        return new Recarregavel(recarregar -> comErro(() -> {
            List<AtivComp> lista = DAO.atividadesComplementares(u.getUsuarioId());
            int validadas = 0, analise = 0;
            DefaultTableModel m = new DefaultTableModel(new String[]{"Atividade", "Categoria", "Carga horária", "Status", "Parecer"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            for (AtivComp a : lista) {
                if ("VALIDADA".equals(a.status())) validadas += a.horas();
                if ("EM_ANALISE".equals(a.status())) analise += a.horas();
                m.addRow(new Object[]{a.descricao(), a.categoria(), a.horas() + "h", rotuloAtiv(a.status()),
                        a.parecer() == null ? "" : a.parecer()});
            }
            JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
            cards.setOpaque(false);
            cards.add(Estilo.indicador("Horas validadas", validadas + "h", "meta do curso: " + META_COMPLEMENTARES + "h", Estilo.OK));
            cards.add(Estilo.indicador("Em análise", analise + "h", "aguardando a coordenação", Estilo.AC));
            cards.add(Estilo.indicador("Faltam", Math.max(0, META_COMPLEMENTARES - validadas) + "h", "para a meta", Estilo.MU));
            Estilo.Botao novo = Estilo.primario("+ Registrar atividade");
            novo.addActionListener(e -> registrarAtividade(novo, u, recarregar));
            JTable t = new JTable(m);
            Estilo.estilizarTabela(t, "Status");
            JPanel acoes = Estilo.acoes(0);
            acoes.add(novo);
            return pilha(cards, acoes, lista.isEmpty()
                    ? Estilo.rotulo("Você ainda não registrou atividades complementares.", Font.PLAIN, 14, Estilo.MU)
                    : Estilo.tabelaSemRolagem(t));
        }));
    }

    private static String rotuloAtiv(String s) {
        switch (s) {
            case "VALIDADA": return "Validada";
            case "REJEITADA": return "Rejeitada";
            default: return "Em análise";
        }
    }

    private static void registrarAtividade(Component pai, Usuario u, Runnable recarregar) {
        JTextField desc = Estilo.campo();
        JComboBox<String> cat = new JComboBox<>(new String[]{"Evento acadêmico", "Curso extracurricular", "Monitoria", "Extensão",
            "Estágio", "Outro"});
        JSpinner horas = new JSpinner(new SpinnerNumberModel(8, 1, 200, 1));
        File[] comprovante = new File[1];
        JLabel lblArq = Estilo.rotulo("Nenhum arquivo (opcional)", Font.PLAIN, 12, Estilo.MU);
        Estilo.Botao escolher = Estilo.neutro("Anexar comprovante");
        escolher.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(pai) == JFileChooser.APPROVE_OPTION) {
                comprovante[0] = fc.getSelectedFile();
                lblArq.setText(comprovante[0].getName());
            }
        });
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 8));
        p.add(Estilo.rotulo("Descrição", Font.BOLD, 13, Estilo.MU));
        p.add(desc);
        p.add(Estilo.rotulo("Categoria", Font.BOLD, 13, Estilo.MU));
        p.add(cat);
        p.add(Estilo.rotulo("Carga horária (horas)", Font.BOLD, 13, Estilo.MU));
        p.add(horas);
        p.add(escolher);
        p.add(lblArq);
        p.setPreferredSize(new Dimension(380, 330));
        if (JOptionPane.showConfirmDialog(pai, p, "Registrar atividade complementar", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String d = desc.getText().trim();
        if (d.isEmpty() || d.length() > 150) {
            Estilo.erro(pai, "Informe a descrição (até 150 caracteres).");
            return;
        }
        try {
            DAO.registrarAtividadeComplementar(u.getUsuarioId(), d, (String) cat.getSelectedItem(), (Integer) horas.getValue(),
                    comprovante[0]);
            Estilo.aviso(pai, "Atividade registrada. Ela ficará em análise até a validação da coordenação.");
            recarregar.run();
        } catch (RuntimeException ex) {
            Estilo.erro(pai, ex.getMessage());
        }
    }

    public static JComponent planoCurricular(Usuario u) {
        return comErro(() -> {
            Map<Integer, List<ItemCurriculo>> porSem = new java.util.TreeMap<>();
            for (ItemCurriculo i : DAO.curriculo(u.getUsuarioId())) {
                porSem.computeIfAbsent(i.semestre(), k -> new ArrayList<>()).add(i);
            }
            List<JComponent> cards = new ArrayList<>();
            for (Map.Entry<Integer, List<ItemCurriculo>> e : porSem.entrySet()) {
                Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 10), 18);
                c.add(Estilo.rotulo(e.getKey() + "º semestre", Font.BOLD, 16, Estilo.TX), BorderLayout.NORTH);
                JPanel itens = new JPanel(new GridLayout(0, 1, 0, 6));
                itens.setOpaque(false);
                for (ItemCurriculo i : e.getValue()) {
                    Color cor = "Concluída".equals(i.status()) ? Estilo.OK : "Em andamento".equals(i.status())
                            ? new Color(0xe8862a) : Estilo.MU;
                    JPanel l = new JPanel(new BorderLayout(8, 0));
                    l.setOpaque(false);
                    l.add(Estilo.texto(i.nome() + " (" + i.horas() + "h)", 190, Font.PLAIN, 13, Estilo.TX), BorderLayout.CENTER);
                    l.add(Estilo.rotulo(i.status(), Font.BOLD, 12, cor), BorderLayout.EAST);
                    itens.add(l);
                }
                c.add(itens, BorderLayout.CENTER);
                cards.add(c);
            }
            return grade(cards, "Plano curricular não cadastrado.");
        });
    }

    public static JComponent declaracao(Usuario u) {
        return new Recarregavel(recarregar -> comErro(() -> {
            String motivo = DAO.motivoIrregular(u.getUsuarioId());
            boolean regular = motivo == null;
            Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 10), 22);
            JPanel topo = Estilo.acoes(14);
            topo.add(new JLabel(new Icone(Tipo.DOC, 28, Estilo.AC)));
            topo.add(Estilo.rotulo("Declaração de Regularidade Acadêmica", Font.BOLD, 17, Estilo.TX));
            topo.add(new Estilo.Tag(regular ? "Regular" : "Irregular"));
            c.add(topo, BorderLayout.NORTH);
            c.add(Estilo.texto(regular ? "Sua situação está regular: matrícula ativa e sem mensalidades vencidas. "
                    + "A declaração comprova seu vínculo e é válida com o código de verificação."
                    : motivo, 620, Font.PLAIN, 13, regular ? Estilo.MU : Estilo.ERRO), BorderLayout.CENTER);
            Estilo.Botao b = Estilo.primario("Emitir declaração");
            b.setEnabled(regular);
            b.addActionListener(e -> emitir(b, u, "Declaração de Regularidade", "Declaração de Regularidade Acadêmica",
                    x -> DocumentoHtml.declaracao(u.getNome(), ra(u)), recarregar));
            JPanel s = Estilo.acoes(0);
            s.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            s.add(b);
            c.add(s, BorderLayout.SOUTH);
            JPanel limite = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            limite.setOpaque(false);
            c.setPreferredSize(new Dimension(700, c.getPreferredSize().height));
            limite.add(c);
            return pilha(limite, documentosEmitidos(u, "Declaração de Regularidade"));
        }));
    }

    public static JComponent hibridez(Usuario u) {
        return comErro(() -> {
            Hibridez h = DAO.hibridez(u.getUsuarioId());
            int pct = (int) Math.round(h.percentual());
            boolean acima = pct > LIMITE_HIBRIDEZ;
            Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 8), 22);
            JPanel top = Estilo.acoes(14);
            top.add(new JLabel(new Icone(Tipo.CLA, 28, Estilo.AC)));
            top.add(Estilo.rotulo("Percentual de Hibridez", Font.BOLD, 17, Estilo.TX));
            c.add(top, BorderLayout.NORTH);
            JPanel m = new JPanel();
            m.setOpaque(false);
            m.setLayout(new BoxLayout(m, BoxLayout.Y_AXIS));
            JLabel d = Estilo.texto("Percentual da carga horária cursada em formato digital em relação ao total de horas "
                    + "já ministradas nas suas turmas. O limite permitido pela modalidade do curso é de " + LIMITE_HIBRIDEZ + "%.",
                    560, Font.PLAIN, 13, Estilo.MU);
            d.setAlignmentX(Component.LEFT_ALIGNMENT);
            JComponent barra = Estilo.barra(Math.min(100, pct), acima ? Estilo.ERRO : Estilo.AC);
            barra.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel n = Estilo.rotulo(pct + "% utilizado (limite de " + LIMITE_HIBRIDEZ + "%) · "
                    + Math.round(h.horasDigitais()) + "h digitais de " + Math.round(h.horasTotais()) + "h cursadas",
                    Font.BOLD, 13, acima ? Estilo.ERRO : Estilo.TX);
            n.setAlignmentX(Component.LEFT_ALIGNMENT);
            m.add(d);
            m.add(Box.createVerticalStrut(12));
            m.add(barra);
            m.add(Box.createVerticalStrut(8));
            m.add(n);
            c.add(m, BorderLayout.CENTER);
            JPanel limite = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            limite.setOpaque(false);
            c.setPreferredSize(new Dimension(700, c.getPreferredSize().height));
            limite.add(c);
            return limite;
        });
    }
}
