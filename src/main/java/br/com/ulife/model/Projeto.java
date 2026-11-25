package br.com.ulife.model;

import java.time.LocalDate;

/** Projeto vinculado a um dos 17 Objetivos de Desenvolvimento Sustentável (ODS). */
public class Projeto {

    public enum Status {
        PLANEJADO("Planejado"),
        EM_ANDAMENTO("Em andamento"),
        CONCLUIDO("Concluído"),
        CANCELADO("Cancelado");

        private final String rotulo;

        Status(String rotulo) { this.rotulo = rotulo; }

        public String getRotulo() { return rotulo; }

        @Override
        public String toString() { return rotulo; }
    }

    public static final String[] ODS = {
            "Erradicação da Pobreza",
            "Fome Zero e Agricultura Sustentável",
            "Saúde e Bem-Estar",
            "Educação de Qualidade",
            "Igualdade de Gênero",
            "Água Potável e Saneamento",
            "Energia Limpa e Acessível",
            "Trabalho Decente e Crescimento Econômico",
            "Indústria, Inovação e Infraestrutura",
            "Redução das Desigualdades",
            "Cidades e Comunidades Sustentáveis",
            "Consumo e Produção Responsáveis",
            "Ação Contra a Mudança Global do Clima",
            "Vida na Água",
            "Vida Terrestre",
            "Paz, Justiça e Instituições Eficazes",
            "Parcerias e Meios de Implementação"
    };

    private Integer projetoId;
    private String codigo;
    private String descricao;
    private int ods;
    private String responsavel;
    private String telefone;
    private LocalDate dataCriacao;
    private Status status = Status.PLANEJADO;

    public Projeto() {}

    public Integer getProjetoId() { return projetoId; }
    public void setProjetoId(Integer projetoId) { this.projetoId = projetoId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getOds() { return ods; }
    public void setOds(int ods) { this.ods = ods; }

    public String getOdsRotulo() { return "ODS " + ods + " – " + ODS[ods - 1]; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public LocalDate getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDate dataCriacao) { this.dataCriacao = dataCriacao; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
