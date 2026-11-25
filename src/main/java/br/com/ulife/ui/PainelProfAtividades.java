package br.com.ulife.ui;

import br.com.ulife.dao.ProfessorConteudoDAO;
import br.com.ulife.dao.ProfessorConteudoDAO.EntregaProf;
import br.com.ulife.dao.ProfessorConteudoDAO.TarefaProf;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Atividades das turmas do professor: publicar, acompanhar as entregas e corrigir. */
public class PainelProfAtividades extends JPanel {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ProfessorConteudoDAO DAO = new ProfessorConteudoDAO();

    public PainelProfAtividades(Usuario prof) {
        super(new BorderLayout());
        setOpaque(false);
        List<Turma> turmas;
        try {
            turmas = new TurmaDAO().listar("", prof.getUsuarioId());
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }
        if (turmas.isEmpty()) {
            add(Estilo.rotulo("Você ainda não tem turmas atribuídas.", Font.PLAIN, 14, Estilo.MU));
            return;
        }
        int[] sel = {0};
        for (int i = 0; i < turmas.size(); i++) { // abre na primeira turma que já tem atividades
            try {
                if (!DAO.tarefas(turmas.get(i).getTurmaId(), prof.getUsuarioId()).isEmpty()) {
                    sel[0] = i;
                    break;
                }
            } catch (RuntimeException ignorada) {
                // mantém a primeira turma
            }
        }
        add(new Recarregavel(rec -> montar(prof, turmas, sel, rec)), BorderLayout.CENTER);
    }

    private JComponent montar(Usuario prof, List<Turma> turmas, int[] sel, Runnable recarregar) {
        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setOpaque(false);

        JComboBox<Turma> cmb = new JComboBox<>(turmas.toArray(new Turma[0]));
        cmb.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmb.setSelectedIndex(sel[0]);
        cmb.addActionListener(e -> {
            sel[0] = cmb.getSelectedIndex();
            recarregar.run();
        });
        Turma turma = turmas.get(sel[0]);

        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setOpaque(false);
        barra.add(Estilo.rotulo("Turma:", Font.BOLD, 14, Estilo.MU), BorderLayout.WEST);
        barra.add(cmb, BorderLayout.CENTER);
        Estilo.Botao nova = Estilo.primario("+ Nova atividade");
        nova.addActionListener(e -> novaAtividade(nova, prof, turma, recarregar));
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        dir.setOpaque(false);
        dir.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        dir.add(nova);
        barra.add(dir, BorderLayout.EAST);
        raiz.add(barra, BorderLayout.NORTH);

        Estilo.Cartao lista = new Estilo.Cartao(new GridLayout(0, 1), 0);
        List<TarefaProf> tarefas = DAO.tarefas(turma.getTurmaId(), prof.getUsuarioId());
        if (tarefas.isEmpty()) {
            JLabel vazio = Estilo.rotulo("Nenhuma atividade publicada nesta turma. Clique em “+ Nova atividade”.",
                    Font.PLAIN, 14, Estilo.MU);
            vazio.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
            lista.add(vazio);
        }
        for (int i = 0; i < tarefas.size(); i++) {
            lista.add(linha(tarefas.get(i), i == 0, prof, recarregar));
        }
        raiz.add(lista, BorderLayout.CENTER);
        return raiz;
    }

    private JComponent linha(TarefaProf t, boolean primeira, Usuario prof, Runnable recarregar) {
        JPanel l = new JPanel(new BorderLayout(16, 0));
        l.setOpaque(false);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(primeira ? 0 : 1, 0, 0, 0, Estilo.BD),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        l.add(new JLabel(new Icone(Icone.Tipo.DOC, 26, Estilo.AC)), BorderLayout.WEST);
        JPanel texto = new JPanel(new GridLayout(2, 1));
        texto.setOpaque(false);
        texto.add(Estilo.rotulo(t.titulo(), Font.BOLD, 15, Estilo.TX));
        texto.add(Estilo.rotulo("Prazo " + t.prazo().format(DATA_HORA) + " · peso " + t.peso().stripTrailingZeros().toPlainString()
                .replace('.', ','), Font.PLAIN, 13, Estilo.MU));
        l.add(texto, BorderLayout.CENTER);
        JPanel d = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        d.setOpaque(false);
        d.add(Estilo.rotulo(t.entregas() + " de " + t.alunos() + " entregues · " + t.corrigidas() + " corrigidas",
                Font.PLAIN, 13, t.entregas() > t.corrigidas() ? new Color(0xe8862a) : Estilo.MU));
        Estilo.Botao ver = Estilo.primario("Ver entregas");
        ver.addActionListener(e -> {
            mostrarEntregas(ver, t, prof);
            recarregar.run();
        });
        d.add(ver);
        l.add(d, BorderLayout.EAST);
        return l;
    }

    // ------------------------------------------------------------------ nova atividade

    private void novaAtividade(Component pai, Usuario prof, Turma turma, Runnable recarregar) {
        JTextField titulo = Estilo.campo();
        JTextArea desc = new JTextArea(3, 20);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(Estilo.fonte(Font.PLAIN, 14));
        JScrollPane spDesc = new JScrollPane(desc);
        JTextField prazo = Estilo.campo();
        prazo.setText(LocalDateTime.now().plusDays(7).withHour(23).withMinute(59).format(DATA_HORA));
        JTextField peso = Estilo.campo();
        peso.setText("2,0");
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(Estilo.rotulo("Título", Font.BOLD, 13, Estilo.MU));
        p.add(titulo);
        p.add(Estilo.rotulo("Descrição / instruções (opcional)", Font.BOLD, 13, Estilo.MU));
        p.add(spDesc);
        p.add(Estilo.rotulo("Prazo de entrega (dd/mm/aaaa hh:mm)", Font.BOLD, 13, Estilo.MU));
        p.add(prazo);
        p.add(Estilo.rotulo("Peso (0 a 10)", Font.BOLD, 13, Estilo.MU));
        p.add(peso);
        p.setPreferredSize(new Dimension(420, 400));
        if (JOptionPane.showConfirmDialog(pai, p, "Nova atividade — " + turma.getNome(), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(prazo.getText().trim(), DATA_HORA);
            BigDecimal pe = new BigDecimal(peso.getText().trim().replace(',', '.'));
            DAO.criarTarefa(turma.getTurmaId(), prof.getUsuarioId(), titulo.getText(), desc.getText(), dt, pe);
            recarregar.run();
        } catch (DateTimeParseException ex) {
            Estilo.erro(pai, "Prazo inválido. Use o formato dd/mm/aaaa hh:mm (ex.: 15/12/2026 23:59).");
        } catch (NumberFormatException ex) {
            Estilo.erro(pai, "Peso inválido. Use um número de 0 a 10.");
        } catch (RuntimeException ex) {
            Estilo.erro(pai, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ entregas e correção

    private static String rotuloStatus(String s) {
        switch (s) {
            case "CORRIGIDO": return "Corrigida";
            case "ENTREGUE": return "Entregue";
            case "PRAZO_EXPIRADO": return "Prazo expirado";
            default: return "Inválida";
        }
    }

    private static String fmt(BigDecimal n) {
        return n == null ? "—" : n.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private void mostrarEntregas(Component pai, TarefaProf tarefa, Usuario prof) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(pai), "Entregas — " + tarefa.titulo(),
                Dialog.ModalityType.APPLICATION_MODAL);
        DefaultTableModel m = new DefaultTableModel(new String[]{"Aluno", "RA", "Entregue em", "Arquivo", "Status", "Nota"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(m);
        Estilo.estilizarTabela(tabela, "Status");
        List<EntregaProf>[] dados = new List[1];
        Runnable carregar = () -> {
            dados[0] = DAO.entregas(tarefa.id(), prof.getUsuarioId());
            m.setRowCount(0);
            for (EntregaProf e : dados[0]) {
                m.addRow(new Object[]{e.aluno(), e.ra(), e.data().format(DATA_HORA), e.arquivoNome() == null ? "—" : e.arquivoNome(),
                        rotuloStatus(e.status()), fmt(e.nota())});
            }
        };
        carregar.run();

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Estilo.BG);
        raiz.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        raiz.add(Estilo.rotulo(tarefa.titulo() + "  ·  " + tarefa.entregas() + " de " + tarefa.alunos() + " entregues",
                Font.BOLD, 15, Estilo.P), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(tabela);
        sp.setBorder(BorderFactory.createLineBorder(Estilo.BD));
        sp.getViewport().setBackground(Estilo.CARD);
        raiz.add(sp, BorderLayout.CENTER);

        JPanel acoes = Estilo.acoes(10);
        Estilo.Botao abrir = Estilo.neutro("Abrir arquivo");
        abrir.addActionListener(e -> {
            EntregaProf sel = selecionada(tabela, dados[0], dlg);
            if (sel != null) {
                abrirLocal(dlg, sel.caminho());
            }
        });
        Estilo.Botao corrigir = Estilo.primario("Corrigir");
        corrigir.addActionListener(e -> {
            EntregaProf sel = selecionada(tabela, dados[0], dlg);
            if (sel != null && corrigirEntrega(dlg, sel, prof)) {
                carregar.run();
            }
        });
        Estilo.Botao fechar = Estilo.neutro("Fechar");
        fechar.addActionListener(e -> dlg.dispose());
        acoes.add(corrigir);
        acoes.add(abrir);
        acoes.add(fechar);
        raiz.add(acoes, BorderLayout.SOUTH);

        dlg.setContentPane(raiz);
        dlg.setSize(820, 470);
        dlg.setLocationRelativeTo(pai);
        dlg.setVisible(true);
    }

    private EntregaProf selecionada(JTable t, List<EntregaProf> dados, Component pai) {
        int row = t.getSelectedRow();
        if (row < 0 || row >= dados.size()) {
            Estilo.aviso(pai, "Selecione uma entrega na lista.");
            return null;
        }
        return dados.get(row);
    }

    private static void abrirLocal(Component pai, String caminho) {
        File f = caminho == null ? null : new File(caminho);
        if (f == null || !f.isFile()) {
            Estilo.erro(pai, "O arquivo não está disponível neste computador.");
            return;
        }
        try {
            Desktop.getDesktop().open(f);
        } catch (Exception ex) {
            Estilo.erro(pai, "Não foi possível abrir o arquivo: " + f.getName());
        }
    }

    private boolean corrigirEntrega(Component pai, EntregaProf e, Usuario prof) {
        JTextField nota = Estilo.campo();
        nota.setText(e.nota() == null ? "" : fmt(e.nota()));
        JTextArea fb = new JTextArea(4, 20);
        fb.setLineWrap(true);
        fb.setWrapStyleWord(true);
        fb.setFont(Estilo.fonte(Font.PLAIN, 14));
        fb.setText(e.feedback() == null ? "" : e.feedback());
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(Estilo.rotulo("Aluno: " + e.aluno() + " · protocolo " + e.protocolo(), Font.PLAIN, 13, Estilo.MU));
        p.add(Estilo.rotulo("Nota (0 a 10)", Font.BOLD, 13, Estilo.MU));
        p.add(nota);
        p.add(Estilo.rotulo("Comentário para o aluno (opcional)", Font.BOLD, 13, Estilo.MU));
        p.add(new JScrollPane(fb));
        p.setPreferredSize(new Dimension(420, 300));
        if (JOptionPane.showConfirmDialog(pai, p, "Corrigir entrega", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return false;
        }
        try {
            DAO.corrigir(e.id(), prof.getUsuarioId(), new BigDecimal(nota.getText().trim().replace(',', '.')), fb.getText());
            return true;
        } catch (NumberFormatException ex) {
            Estilo.erro(pai, "Nota inválida. Use um número de 0 a 10.");
        } catch (RuntimeException ex) {
            Estilo.erro(pai, ex.getMessage());
        }
        return false;
    }
}
