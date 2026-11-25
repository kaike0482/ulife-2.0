package br.com.ulife.model;

/** Frequência de um aluno em uma aula, com os minutos registrados pela videochamada. */
public class LinhaFrequencia {
    /** Duração padrão da aula (19h00 às 21h50) e mínimo para sugerir presença (75%). */
    public static final int DURACAO_AULA_MIN = 170;
    public static final int MINIMO_PRESENCA_MIN = (int) Math.ceil(DURACAO_AULA_MIN * 0.75);

    private int matriculaId;
    private String nome;
    private String ra;
    private Integer minutos; // null = sem registro da videochamada
    private boolean presenca;
    private String justificativa;
    private boolean lancada;

    public int getMatriculaId() { return matriculaId; }
    public void setMatriculaId(int matriculaId) { this.matriculaId = matriculaId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRa() { return ra; }
    public void setRa(String ra) { this.ra = ra; }

    public Integer getMinutos() { return minutos; }
    public void setMinutos(Integer minutos) { this.minutos = minutos; }

    public boolean isPresenca() { return presenca; }
    public void setPresenca(boolean presenca) { this.presenca = presenca; }

    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }

    public boolean isLancada() { return lancada; }
    public void setLancada(boolean lancada) { this.lancada = lancada; }

    /** Presença sugerida automaticamente pelo tempo conectado na videochamada. */
    public boolean isPresencaSugerida() {
        return minutos != null && minutos >= MINIMO_PRESENCA_MIN;
    }
}
