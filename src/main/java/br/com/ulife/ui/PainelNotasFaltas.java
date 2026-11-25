package br.com.ulife.ui;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Notas e faltas do aluno, vindas dos lançamentos feitos pelos professores. */
public class PainelNotasFaltas extends JPanel {

    private static String nota(BigDecimal v) {
        if (v == null) {
            return "—";
        }
        BigDecimal s = v.stripTrailingZeros();
        return (s.scale() < 1 ? s.setScale(1) : s).toPlainString().replace('.', ',');
    }

    public PainelNotasFaltas(Usuario aluno) {
        super(new BorderLayout(0, 18));
        setOpaque(false);

        List<AcademicoDAO.Boletim> boletim;
        try {
            boletim = new AcademicoDAO().boletim(aluno.getUsuarioId());
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro ao carregar suas notas: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO));
            return;
        }
        if (boletim.isEmpty()) {
            add(Estilo.rotulo("Você ainda não está matriculado em nenhuma turma.", Font.PLAIN, 14, Estilo.MU));
            return;
        }

        BigDecimal somaMedias = BigDecimal.ZERO;
        int comMedia = 0, presencas = 0, aulas = 0, faltas = 0;
        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Turma", "Professor", "A1", "A2", "A3", "Média", "Faltas", "Frequência", "Situação"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (AcademicoDAO.Boletim b : boletim) {
            BigDecimal media = b.media();
            Double freq = b.frequencia();
            if (media != null) {
                somaMedias = somaMedias.add(media);
                comMedia++;
            }
            presencas += b.presencas;
            aulas += b.aulas;
            faltas += b.faltas();

            String situacao;
            if (freq != null && freq < 75) {
                situacao = "Frequência baixa";
            } else if (media != null) {
                situacao = media.compareTo(AcademicoDAO.MEDIA_APROVACAO) >= 0 ? "Aprovado" : "Reprovado";
            } else {
                situacao = "Em andamento";
            }
            m.addRow(new Object[]{b.turma, b.professor, nota(b.notas.get("A1")), nota(b.notas.get("A2")),
                    nota(b.notas.get("A3")), nota(media), b.faltas() + " de " + b.aulas,
                    freq == null ? "—" : String.format("%.0f%%", freq), situacao});
        }

        String mediaGeral = comMedia == 0 ? "—"
                : nota(somaMedias.divide(new BigDecimal(comMedia), 1, RoundingMode.HALF_UP));
        Double freqGeral = aulas == 0 ? null : presencas * 100.0 / aulas;

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(Estilo.indicador("Média geral", mediaGeral, comMedia == 0 ? "sem médias completas ainda"
                : "em " + comMedia + (comMedia == 1 ? " turma com A1–A3" : " turmas com A1–A3"), Estilo.AC));
        cards.add(Estilo.indicador("Frequência", freqGeral == null ? "—" : String.format("%.0f%%", freqGeral),
                "mínimo exigido: 75%", freqGeral != null && freqGeral < 75 ? Estilo.ERRO : Estilo.OK));
        cards.add(Estilo.indicador("Faltas", String.valueOf(faltas), "em " + aulas + " aulas lançadas",
                Estilo.AC));
        add(cards, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        JTable t = new JTable(m);
        Estilo.estilizarTabela(t, "Situação");
        int[] larguras = {260, 140, 60, 60, 60, 70, 90, 100, 170};
        for (int i = 0; i < larguras.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
        centro.add(Estilo.tabelaSemRolagem(t), BorderLayout.CENTER);
        centro.add(Estilo.texto("Os valores são os lançados pelos professores. A média é a média simples de A1, A2 e A3; "
                + "aprovação com média 6,0 ou mais e frequência mínima de 75%.", 900, Font.PLAIN, 12, Estilo.MU),
                BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
    }
}
