package br.com.ulife.ui;

import br.com.ulife.dao.CobrancaDAO;
import br.com.ulife.model.Cobranca;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/** Mensalidades do aluno: resumo, lista de cobranças e botão Pagar (PIX ou boleto). */
public class PainelPagamentos extends JPanel {

    private final CobrancaDAO dao = new CobrancaDAO();
    private final Usuario usuario;
    private final Runnable aoMudar;

    public PainelPagamentos(Usuario usuario, Runnable aoMudar) {
        super(new BorderLayout(0, 18));
        this.usuario = usuario;
        this.aoMudar = aoMudar;
        setOpaque(false);
        montar();
    }

    private void montar() {
        removeAll();
        List<Cobranca> lista;
        try {
            lista = dao.listar(usuario.getUsuarioId());
        } catch (RuntimeException ex) {
            add(Estilo.rotulo("Erro ao carregar cobranças: " + ex.getMessage(), Font.PLAIN, 14, Estilo.ERRO),
                    BorderLayout.NORTH);
            return;
        }

        BigDecimal aberto = BigDecimal.ZERO, vencido = BigDecimal.ZERO, pago = BigDecimal.ZERO;
        int nAberto = 0, nVencido = 0, nPago = 0;
        for (Cobranca c : lista) {
            switch (c.getStatus()) {
                case ABERTA: aberto = aberto.add(c.getValor()); nAberto++; break;
                case VENCIDA: vencido = vencido.add(c.getValor()); nVencido++; break;
                case PAGA: pago = pago.add(c.getValor()); nPago++; break;
                default: break;
            }
        }

        JPanel resumo = new JPanel(new GridLayout(1, 3, 16, 0));
        resumo.setOpaque(false);
        resumo.add(cartaoResumo("Em aberto", aberto, nAberto, Estilo.AC));
        resumo.add(cartaoResumo("Vencidas", vencido, nVencido, nVencido > 0 ? Estilo.ERRO : Estilo.MU));
        resumo.add(cartaoResumo("Pagas", pago, nPago, Estilo.OK));
        add(resumo, BorderLayout.NORTH);

        Estilo.Cartao card = new Estilo.Cartao(new GridLayout(0, 1), 0);
        if (lista.isEmpty()) {
            JLabel vazio = Estilo.rotulo("Nenhuma cobrança encontrada para a sua conta.", Font.PLAIN, 14, Estilo.MU);
            vazio.setBorder(BorderFactory.createEmptyBorder(26, 20, 26, 20));
            card.add(vazio);
        }
        for (int i = 0; i < lista.size(); i++) {
            card.add(linha(lista.get(i), i == 0));
        }
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(titulo("Mensalidades"));
        col.add(Box.createVerticalStrut(12));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(card);
        col.add(Box.createVerticalStrut(28));
        col.add(titulo("Histórico de pagamentos"));
        col.add(Box.createVerticalStrut(12));
        PainelHistorico historico = new PainelHistorico(usuario);
        historico.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(historico);
        add(col, BorderLayout.CENTER);
    }

    private JComponent titulo(String texto) {
        JLabel l = Estilo.rotulo(texto, Font.BOLD, 18, Estilo.TX);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JComponent cartaoResumo(String titulo, BigDecimal valor, int qtd, Color cor) {
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 4), 18);
        c.add(Estilo.rotulo(titulo, Font.PLAIN, 13, Estilo.MU), BorderLayout.NORTH);
        c.add(Estilo.rotulo(DialogoPagamento.MOEDA.format(valor), Font.BOLD, 24, cor), BorderLayout.CENTER);
        c.add(Estilo.rotulo(qtd + (qtd == 1 ? " cobrança" : " cobranças"), Font.PLAIN, 12, Estilo.MU),
                BorderLayout.SOUTH);
        return c;
    }

    private JComponent linha(Cobranca c, boolean primeira) {
        boolean vencida = c.getStatus() == Cobranca.Status.VENCIDA;
        JPanel l = new JPanel(new BorderLayout(16, 0));
        l.setOpaque(false);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(primeira ? 0 : 1, vencida ? 4 : 0, 0, 0, vencida ? Estilo.ERRO : Estilo.BD),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        l.add(new JLabel(new Icone(Icone.Tipo.DOC, 26, Estilo.AC)), BorderLayout.WEST);

        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(Estilo.rotulo(c.getDescricao(), Font.BOLD, 15, Estilo.TX));
        textos.add(Estilo.rotulo("Vencimento: " + c.getVencimento().format(DialogoPagamento.DATA),
                Font.PLAIN, 13, vencida ? Estilo.ERRO : Estilo.MU));
        l.add(textos, BorderLayout.CENTER);

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
        dir.setOpaque(false);
        dir.add(new Estilo.Tag(c.getStatus().getRotulo()));
        JLabel valor = Estilo.rotulo(DialogoPagamento.MOEDA.format(c.getValor()), Font.BOLD, 16, Estilo.TX);
        valor.setPreferredSize(new Dimension(110, 24));
        valor.setHorizontalAlignment(SwingConstants.RIGHT);
        dir.add(valor);
        if (c.isPagavel()) {
            Estilo.Botao pagar = Estilo.primario("Pagar");
            pagar.setPreferredSize(new Dimension(110, 40));
            pagar.addActionListener(e -> {
                if (DialogoPagamento.mostrar(this, c, usuario.getUsuarioId())) {
                    montar();
                    revalidate();
                    repaint();
                    if (aoMudar != null) {
                        aoMudar.run();
                    }
                }
            });
            dir.add(pagar);
        } else {
            JLabel ok = Estilo.rotulo(c.getStatus() == Cobranca.Status.PAGA ? "Quitada" : "—",
                    Font.PLAIN, 13, Estilo.MU);
            ok.setPreferredSize(new Dimension(110, 40));
            ok.setHorizontalAlignment(SwingConstants.CENTER);
            dir.add(ok);
        }
        l.add(dir, BorderLayout.EAST);
        return l;
    }
}
