package br.com.ulife.ui;

import br.com.ulife.dao.ProfessorConteudoDAO;
import br.com.ulife.dao.ProfessorConteudoDAO.MaterialProf;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Materiais das turmas do professor: enviar arquivos para os alunos, abrir e excluir. */
public class PainelProfMateriais extends JPanel {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ProfessorConteudoDAO DAO = new ProfessorConteudoDAO();

    public PainelProfMateriais(Usuario prof) {
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
        add(new Recarregavel(rec -> montar(prof, turmas, sel, rec)), BorderLayout.CENTER);
    }

    private static String tamanho(long bytes) {
        if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", bytes / 1048576.0).replace('.', ',');
        }
        return Math.max(1, bytes / 1024) + " KB";
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
        Estilo.Botao novo = Estilo.primario("+ Enviar material");
        novo.addActionListener(e -> enviar(novo, prof, turma, recarregar));
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        dir.setOpaque(false);
        dir.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        dir.add(novo);
        barra.add(dir, BorderLayout.EAST);
        raiz.add(barra, BorderLayout.NORTH);

        Estilo.Cartao lista = new Estilo.Cartao(new GridLayout(0, 1), 0);
        List<MaterialProf> materiais = DAO.materiais(turma.getTurmaId(), prof.getUsuarioId());
        if (materiais.isEmpty()) {
            JLabel vazio = Estilo.rotulo("Nenhum material enviado para esta turma. Clique em “+ Enviar material”.",
                    Font.PLAIN, 14, Estilo.MU);
            vazio.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
            lista.add(vazio);
        }
        for (int i = 0; i < materiais.size(); i++) {
            MaterialProf m = materiais.get(i);
            JPanel l = new JPanel(new BorderLayout(16, 0));
            l.setOpaque(false);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(i == 0 ? 0 : 1, 0, 0, 0, Estilo.BD),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
            l.add(new JLabel(new Icone(Icone.Tipo.DOC, 26, Estilo.AC)), BorderLayout.WEST);
            JPanel t = new JPanel(new GridLayout(2, 1));
            t.setOpaque(false);
            t.add(Estilo.rotulo(m.titulo(), Font.BOLD, 15, Estilo.TX));
            t.add(Estilo.rotulo(m.arquivoNome() + " · " + tamanho(m.tamanho()) + " · publicado em " + m.publicado().format(DATA),
                    Font.PLAIN, 13, Estilo.MU));
            l.add(t, BorderLayout.CENTER);
            JPanel d = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
            d.setOpaque(false);
            Estilo.Botao abrir = Estilo.neutro("Abrir");
            abrir.addActionListener(e -> abrirLocal(abrir, m.caminho()));
            Estilo.Botao excluir = Estilo.perigo("Excluir");
            excluir.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(excluir, "Excluir \"" + m.titulo() + "\"? Os alunos deixarão de vê-lo.",
                        "Excluir material", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    try {
                        DAO.excluirMaterial(m.id(), prof.getUsuarioId());
                        recarregar.run();
                    } catch (RuntimeException ex) {
                        Estilo.erro(excluir, ex.getMessage());
                    }
                }
            });
            d.add(abrir);
            d.add(excluir);
            l.add(d, BorderLayout.EAST);
            lista.add(l);
        }
        raiz.add(lista, BorderLayout.CENTER);
        return raiz;
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

    private void enviar(Component pai, Usuario prof, Turma turma, Runnable recarregar) {
        JTextField titulo = Estilo.campo();
        File[] arquivo = new File[1];
        JLabel lblArq = Estilo.rotulo("Nenhum arquivo selecionado", Font.PLAIN, 12, Estilo.MU);
        Estilo.Botao escolher = Estilo.neutro("Escolher arquivo");
        escolher.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(pai) == JFileChooser.APPROVE_OPTION) {
                arquivo[0] = fc.getSelectedFile();
                lblArq.setText(arquivo[0].getName());
                if (titulo.getText().trim().isEmpty()) {
                    String n = arquivo[0].getName();
                    titulo.setText(n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n);
                }
            }
        });
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 8));
        p.add(Estilo.rotulo("Título do material", Font.BOLD, 13, Estilo.MU));
        p.add(titulo);
        p.add(escolher);
        p.add(lblArq);
        p.setPreferredSize(new Dimension(400, 220));
        if (JOptionPane.showConfirmDialog(pai, p, "Enviar material — " + turma.getNome(), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            DAO.enviarMaterial(turma.getTurmaId(), prof.getUsuarioId(), titulo.getText(), arquivo[0]);
            recarregar.run();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Estilo.erro(pai, ex.getMessage());
        } catch (RuntimeException ex) {
            Estilo.erro(pai, ex.getMessage());
        }
    }
}
