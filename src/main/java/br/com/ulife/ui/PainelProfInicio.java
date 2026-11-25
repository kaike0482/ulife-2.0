package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.Turma;
import br.com.ulife.model.Usuario;
import br.com.ulife.service.ResumoTurma;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Painel inicial do professor: resumo, pendências (frequência e notas) e suas turmas. */
public class PainelProfInicio extends JPanel {

    public PainelProfInicio(Usuario prof, Runnable irFrequencia, Runnable irNotas) {
        super(new BorderLayout(0, 18));
        setOpaque(false);

        List<Turma> turmas;
        List<AcademicoDAO.Pendencia> pend;
        try {
            turmas = new TurmaDAO().listar("", prof.getUsuarioId());
            pend = new AcademicoDAO().pendenciasDeFrequencia(prof.getUsuarioId());
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro ao carregar o painel: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }

        List<ResumoTurma> resumos = new ArrayList<>();
        int alunos = 0, semNotas = 0;
        for (Turma t : turmas) {
            ResumoTurma r = ResumoTurma.calcular(t.getTurmaId());
            resumos.add(r);
            alunos += r.alunos;
            semNotas += r.semNotaCompleta;
        }

        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("pt", "BR")));
        JPanel topo = new JPanel(new GridLayout(2, 1));
        topo.setOpaque(false);
        topo.add(Estilo.rotulo("Olá, " + primeiroNome(prof.getNome()) + "!", Font.BOLD, 20, Estilo.TX));
        topo.add(Estilo.rotulo(data.substring(0, 1).toUpperCase() + data.substring(1), Font.PLAIN, 13, Estilo.MU));

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(Estilo.indicador("Minhas turmas", String.valueOf(turmas.size()), "no semestre atual", Estilo.AC));
        cards.add(Estilo.indicador("Alunos", String.valueOf(alunos), "matrículas ativas", Estilo.AC));
        cards.add(Estilo.indicador("Frequências a lançar", String.valueOf(pend.size()),
                pend.isEmpty() ? "tudo em dia" : "aulas aguardando você", pend.isEmpty() ? Estilo.OK : new Color(0xe8862a)));
        cards.add(Estilo.indicador("Alunos sem nota completa", String.valueOf(semNotas), "falta A1, A2 ou A3",
                semNotas == 0 ? Estilo.OK : new Color(0xe8862a)));

        JPanel norte = new JPanel(new BorderLayout(0, 16));
        norte.setOpaque(false);
        norte.add(topo, BorderLayout.NORTH);
        norte.add(cards, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        // pendências
        Estilo.Cartao box = new Estilo.Cartao(new GridLayout(0, 1), 0);
        JLabel tit = Estilo.rotulo("Pendências", Font.BOLD, 16, Estilo.TX);
        tit.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
        box.add(tit);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
        int linhas = 0;
        for (AcademicoDAO.Pendencia p : pend) {
            box.add(linhaPendencia(Icone.Tipo.CAL, "Frequência — " + p.turmaNome + " (aula de " + p.data.format(f) + ")",
                    "Os minutos da videochamada já foram registrados: confira a sugestão e lance.", "Lançar", irFrequencia));
            linhas++;
        }
        for (int i = 0; i < turmas.size(); i++) {
            ResumoTurma r = resumos.get(i);
            if (r.semNotaCompleta > 0) {
                box.add(linhaPendencia(Icone.Tipo.STAR, "Notas — " + turmas.get(i).getNome(),
                        r.semNotaCompleta + (r.semNotaCompleta == 1 ? " aluno sem" : " alunos sem") + " todas as notas (A1–A3).",
                        "Lançar notas", irNotas));
                linhas++;
            }
        }
        if (linhas == 0) {
            JLabel ok = Estilo.rotulo("Nenhuma pendência. Tudo em dia!", Font.PLAIN, 14, Estilo.OK);
            ok.setBorder(BorderFactory.createEmptyBorder(6, 18, 18, 18));
            box.add(ok);
        }
        centro.add(box);
        centro.add(Box.createVerticalStrut(18));

        // turmas
        DefaultTableModel m = new DefaultTableModel(new String[]{"Código", "Turma", "Semestre", "Alunos", "Frequência", "Média"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 0; i < turmas.size(); i++) {
            Turma t = turmas.get(i);
            ResumoTurma r = resumos.get(i);
            m.addRow(new Object[]{t.getCodigo(), t.getNome(), t.getSemestre(), r.alunos, r.frequenciaTexto(), r.mediaTexto()});
        }
        JLabel tt = Estilo.rotulo("Minhas turmas", Font.BOLD, 16, Estilo.TX);
        tt.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(tt);
        centro.add(Box.createVerticalStrut(10));
        JTable t = new JTable(m);
        Estilo.estilizarTabela(t);
        JPanel tab = Estilo.tabelaSemRolagem(t);
        tab.setAlignmentX(Component.LEFT_ALIGNMENT);
        centro.add(tab);
        add(centro, BorderLayout.CENTER);
    }

    private static String primeiroNome(String nome) {
        int i = nome.indexOf(' ');
        return i > 0 ? nome.substring(0, i) : nome;
    }

    private JComponent linhaPendencia(Icone.Tipo tipo, String titulo, String sub, String botao, Runnable acao) {
        JPanel l = new JPanel(new BorderLayout(14, 0));
        l.setOpaque(false);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Estilo.BD),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        l.add(new JLabel(new Icone(tipo, 24, new Color(0xe8862a))), BorderLayout.WEST);
        JPanel t = new JPanel(new GridLayout(2, 1));
        t.setOpaque(false);
        t.add(Estilo.rotulo(titulo, Font.BOLD, 14, Estilo.TX));
        t.add(Estilo.rotulo(sub, Font.PLAIN, 13, Estilo.MU));
        l.add(t, BorderLayout.CENTER);
        Estilo.Botao b = Estilo.primario(botao);
        b.addActionListener(e -> acao.run());
        JPanel d = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        d.setOpaque(false);
        d.add(b);
        l.add(d, BorderLayout.EAST);
        return l;
    }
}
