package br.com.ulife.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Documentos acadêmicos em HTML (para imprimir ou salvar como PDF), com código de verificação. */
public final class DocumentoHtml {

    public static final String CURSO = "Engenharia de Software";
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private DocumentoHtml() {}

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Moldura do documento: cabeçalho institucional, título, corpo e rodapé com o código de verificação. */
    public static String pagina(String titulo, String corpo, String codigo, LocalDateTime emitidoEm) {
        return "<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'><title>" + esc(titulo) + "</title><style>"
                + "body{font-family:'Segoe UI',Arial,sans-serif;color:#14233a;margin:0;background:#e9eef5}"
                + ".folha{max-width:800px;margin:24px auto;background:#fff;padding:48px 56px;border:1px solid #c9d4e3}"
                + "header{border-bottom:3px solid #1c4f8c;padding-bottom:12px;margin-bottom:28px}"
                + "header h1{margin:0;font-size:18px;color:#1c4f8c}header p{margin:2px 0 0;font-size:12px;color:#6b7a90}"
                + "h2{text-align:center;font-size:22px;margin:8px 0 26px;letter-spacing:1px}"
                + "p{line-height:1.6;font-size:15px}table{width:100%;border-collapse:collapse;font-size:13px;margin:14px 0}"
                + "th,td{border:1px solid #c9d4e3;padding:8px;text-align:left}th{background:#eef2f8}"
                + "footer{margin-top:40px;border-top:1px solid #c9d4e3;padding-top:12px;font-size:12px;color:#6b7a90}"
                + ".codigo{font-family:Consolas,monospace;font-weight:bold;color:#14233a}"
                + "@media print{body{background:#fff}.folha{border:none;margin:0}}"
                + "</style></head><body><div class='folha'><header><h1>Universidade São Judas Tadeu — Campus São Bernardo do Campo</h1>"
                + "<p>Ulife 2.0 · Plataforma Acadêmica Integrada</p></header><h2>" + esc(titulo).toUpperCase() + "</h2>"
                + corpo
                + "<footer>Código de verificação: <span class='codigo'>" + esc(codigo) + "</span> · Emitido em "
                + emitidoEm.format(DATA_HORA) + ".<br>Documento gerado eletronicamente em ambiente de demonstração.</footer>"
                + "</div></body></html>";
    }

    public static String declaracao(String nome, String ra) {
        return "<p>Declaramos, para os devidos fins, que <b>" + esc(nome) + "</b>, RA <b>" + esc(ra)
                + "</b>, está regularmente matriculado(a) no curso de <b>" + CURSO + "</b>, com situação acadêmica "
                + "e financeira regular na data de emissão deste documento.</p>";
    }

    public static String certificado(String tipo, String nome, String ra, String detalhe) {
        return "<p style='text-align:center;font-size:17px'>Certificamos que</p><p style='text-align:center;font-size:24px'><b>"
                + esc(nome) + "</b></p><p style='text-align:center'>RA " + esc(ra) + "</p><p style='text-align:center'>"
                + esc(detalhe) + "</p>";
    }

    /** Histórico: tabela com turma, notas, frequência e situação. */
    public static String historico(String nome, String ra, java.util.List<String[]> linhas) {
        StringBuilder sb = new StringBuilder("<p>Aluno(a): <b>" + esc(nome) + "</b> · RA <b>" + esc(ra) + "</b> · Curso: <b>"
                + CURSO + "</b></p><table><tr><th>Unidade de curso</th><th>A1</th><th>A2</th><th>A3</th><th>Média</th>"
                + "<th>Frequência</th><th>Situação</th></tr>");
        for (String[] l : linhas) {
            sb.append("<tr>");
            for (String c : l) {
                sb.append("<td>").append(esc(c)).append("</td>");
            }
            sb.append("</tr>");
        }
        return sb.append("</table><p style='font-size:12px;color:#6b7a90'>Semestres anteriores constam como concluídos "
                + "conforme o plano curricular.</p>").toString();
    }

    /** Grava em ./documentos/&lt;codigo&gt;.html e devolve o caminho. */
    public static Path salvar(String codigo, String html) {
        try {
            Path p = Path.of("documentos", codigo + ".html");
            Files.createDirectories(p.getParent());
            Files.write(p, html.getBytes(StandardCharsets.UTF_8));
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível salvar o documento: " + e.getMessage(), e);
        }
    }
}
