package br.com.ulife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pagamento confirmado (ou em processamento) de uma cobrança. */
public class Pagamento {

    public enum Metodo {
        PIX("PIX"), BOLETO("Boleto");

        private final String rotulo;

        Metodo(String rotulo) { this.rotulo = rotulo; }

        public String getRotulo() { return rotulo; }
    }

    private Integer pagamentoId;
    private Integer cobrancaId;
    private String descricaoCobranca;
    private Metodo metodo;
    private BigDecimal valorPago;
    private LocalDateTime dataPagamento;
    private String transacaoId;
    private String status;
    private String alunoNome; // preenchido nas consultas de gestão

    public String getAlunoNome() { return alunoNome; }
    public void setAlunoNome(String alunoNome) { this.alunoNome = alunoNome; }

    public Integer getPagamentoId() { return pagamentoId; }
    public void setPagamentoId(Integer pagamentoId) { this.pagamentoId = pagamentoId; }

    public Integer getCobrancaId() { return cobrancaId; }
    public void setCobrancaId(Integer cobrancaId) { this.cobrancaId = cobrancaId; }

    public String getDescricaoCobranca() { return descricaoCobranca; }
    public void setDescricaoCobranca(String descricaoCobranca) { this.descricaoCobranca = descricaoCobranca; }

    public Metodo getMetodo() { return metodo; }
    public void setMetodo(Metodo metodo) { this.metodo = metodo; }

    public BigDecimal getValorPago() { return valorPago; }
    public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }

    public String getTransacaoId() { return transacaoId; }
    public void setTransacaoId(String transacaoId) { this.transacaoId = transacaoId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
