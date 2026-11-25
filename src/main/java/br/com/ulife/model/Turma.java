package br.com.ulife.model;

/** Turma (disciplina em um semestre) com seu professor responsável. */
public class Turma {
    private Integer turmaId;
    private String codigo;
    private String nome;
    private String descricao;
    private Integer professorId;
    private String professorNome;
    private String semestre;
    private int ano;
    private int cargaHoraria = 60;
    private int qtdAlunos;

    public Integer getTurmaId() { return turmaId; }
    public void setTurmaId(Integer turmaId) { this.turmaId = turmaId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getProfessorId() { return professorId; }
    public void setProfessorId(Integer professorId) { this.professorId = professorId; }

    public String getProfessorNome() { return professorNome; }
    public void setProfessorNome(String professorNome) { this.professorNome = professorNome; }

    public String getSemestre() { return semestre; }
    public void setSemestre(String semestre) { this.semestre = semestre; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public int getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(int cargaHoraria) { this.cargaHoraria = cargaHoraria; }

    public int getQtdAlunos() { return qtdAlunos; }
    public void setQtdAlunos(int qtdAlunos) { this.qtdAlunos = qtdAlunos; }

    @Override
    public String toString() { return codigo + " — " + nome; }
}
