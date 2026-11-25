package br.com.ulife.service;

import br.com.ulife.dao.AcademicoDAO;
import br.com.ulife.dao.TurmaDAO;
import br.com.ulife.model.AlunoTurma;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** Números consolidados de uma turma: alunos, frequência média, média das notas e pendências de notas. */
public final class ResumoTurma {
    public int alunos;
    public Double frequencia;      // % de presença nas aulas lançadas, ou null
    public BigDecimal media;       // média das médias completas (A1–A3), ou null
    public int semNotaCompleta;    // alunos que ainda não têm A1, A2 e A3

    private ResumoTurma() {}

    public static ResumoTurma calcular(int turmaId) {
        AcademicoDAO adao = new AcademicoDAO();
        List<AlunoTurma> lista = new TurmaDAO().alunosDaTurma(turmaId);
        Map<Integer, Map<String, BigDecimal>> notas = adao.notasDaTurma(turmaId);
        Map<Integer, int[]> freq = adao.resumoFrequencia(turmaId);

        ResumoTurma r = new ResumoTurma();
        r.alunos = lista.size();
        BigDecimal soma = BigDecimal.ZERO;
        int comMedia = 0;
        int presencas = 0, aulas = 0;
        for (AlunoTurma a : lista) {
            BigDecimal m = AcademicoDAO.media(notas.get(a.getMatriculaId()));
            if (m == null) {
                r.semNotaCompleta++;
            } else {
                soma = soma.add(m);
                comMedia++;
            }
            int[] f = freq.get(a.getMatriculaId());
            if (f != null) {
                presencas += f[0];
                aulas += f[1];
            }
        }
        if (comMedia > 0) {
            r.media = soma.divide(new BigDecimal(comMedia), 1, RoundingMode.HALF_UP);
        }
        if (aulas > 0) {
            r.frequencia = presencas * 100.0 / aulas;
        }
        return r;
    }

    public String frequenciaTexto() {
        return frequencia == null ? "—" : String.format("%.0f%%", frequencia);
    }

    public String mediaTexto() {
        return media == null ? "—" : media.toPlainString().replace('.', ',');
    }
}
