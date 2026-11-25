package br.com.ulife.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cobrança (mensalidade/parcela) de um usuário aluno. */
public class Cobranca {

    public enum Status {
        ABERTA("Em aberto"), PAGA("Paga"), VENCIDA("Vencida"), CANCELADA("Cancelada");

        private final String rotulo;

        Status(String rotulo) { this.rotulo = rotulo; }

        public String getRotulo() { return rotulo; }
    }

    private Integer cobrancaId;
    private Integer usuarioId;
    private String descricao;
    private BigDecimal valor;
    private LocalDate vencimento;
    private Status status;
    private String alunoNome; // preenchido nas consultas de gestão

    public String getAlunoNome() { return alunoNome; }
    public void setAlunoNome(String alunoNome) { this.alunoNome = alunoNome; }

    public Integer getCobrancaId() { return cobrancaId; }
    public void setCobrancaId(Integer cobrancaId) { this.cobrancaId = cobrancaId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    /** Pode ser paga: em aberto ou vencida. */
    public boolean isPagavel() {
        return status == Status.ABERTA || status == Status.VENCIDA;
    }
}
