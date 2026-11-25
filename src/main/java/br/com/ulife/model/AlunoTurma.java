package br.com.ulife.model;

/** Aluno visto no contexto de uma turma (matrícula, RA, nome e e-mail). */
public class AlunoTurma {
    private Integer matriculaId; // null quando o aluno ainda não está matriculado
    private Integer usuarioId;
    private String nome;
    private String ra;
    private String email;

    public Integer getMatriculaId() { return matriculaId; }
    public void setMatriculaId(Integer matriculaId) { this.matriculaId = matriculaId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRa() { return ra; }
    public void setRa(String ra) { this.ra = ra; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() { return nome + " (" + ra + ")"; }
}
