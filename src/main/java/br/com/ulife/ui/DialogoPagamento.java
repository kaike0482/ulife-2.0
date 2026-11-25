package br.com.ulife.ui;

import br.com.ulife.dao.CobrancaDAO;
import br.com.ulife.model.Cobranca;
import br.com.ulife.model.Pagamento;
import br.com.ulife.service.PagamentoSimulado;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Janela de pagamento de uma cobrança por PIX (QR Code) ou boleto — ambiente de demonstração. */
public class DialogoPagamento extends JDialog {

    static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Cobranca cobranca;
    private final int usuarioId;
    private final CobrancaDAO dao = new CobrancaDAO();
    private final String codigoPix;
    private final String linhaBoleto;

    private final CardLayout cards = new CardLayout();
    private final JPanel metodos = new JPanel(cards);
    private Pagamento.Metodo metodoAtual = Pagamento.Metodo.PIX;
    private Estilo.Botao btnPix;
    private Estilo.Botao btnBoleto;
    private boolean pago = false;

    private DialogoPagamento(Window dono, Cobranca cobranca, int usuarioId) {
        super(dono, "Pagar " + cobranca.getDescricao(), ModalityType.APPLICATION_MODAL);
        this.cobranca = cobranca;
        this.usuarioId = usuarioId;
        this.codigoPix = PagamentoSimulado.pixCopiaECola(cobranca);
        this.linhaBoleto = PagamentoSimulado.linhaDigitavel(cobranca);
        montar();
        pack();
        setMinimumSize(new Dimension(520, 0));
        setLocationRelativeTo(dono);
    }

    /** Abre o diálogo e retorna true se o pagamento foi confirmado. */
    public static boolean mostrar(Component pai, Cobranca cobranca, int usuarioId) {
        Window dono = SwingUtilities.getWindowAncestor(pai);
        DialogoPagamento d = new DialogoPagamento(dono, cobranca, usuarioId);
        d.setVisible(true);
        return d.pago;
    }

    private void montar() {
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Estilo.BG);
        raiz.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        // resumo
        Estilo.Cartao resumo = new Estilo.Cartao(new BorderLayout(), 16);
        JPanel esq = new JPanel(new GridLayout(2, 1));
        esq.setOpaque(false);
        esq.add(Estilo.rotulo(cobranca.getDescricao(), Font.BOLD, 16, Estilo.TX));
        esq.add(Estilo.rotulo("Vencimento: " + cobranca.getVencimento().format(DATA)
                + (cobranca.getStatus() == Cobranca.Status.VENCIDA ? "  (vencida)" : ""),
                Font.PLAIN, 13, cobranca.getStatus() == Cobranca.Status.VENCIDA ? Estilo.ERRO : Estilo.MU));
        resumo.add(esq, BorderLayout.WEST);
        resumo.add(Estilo.rotulo(MOEDA.format(cobranca.getValor()), Font.BOLD, 24, Estilo.P), BorderLayout.EAST);
        raiz.add(resumo, BorderLayout.NORTH);

        // escolha do método
        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        JPanel escolha = new JPanel(new GridLayout(1, 2, 10, 0));
        escolha.setOpaque(false);
        btnPix = Estilo.primario("PIX (QR Code)");
        btnBoleto = Estilo.neutro("Boleto");
        btnPix.addActionListener(e -> selecionar(Pagamento.Metodo.PIX));
        btnBoleto.addActionListener(e -> selecionar(Pagamento.Metodo.BOLETO));
        escolha.add(btnPix);
        escolha.add(btnBoleto);
        centro.add(escolha, BorderLayout.NORTH);

        metodos.setOpaque(false);
        metodos.add(painelPix(), "PIX");
        metodos.add(painelBoleto(), "BOLETO");
        centro.add(metodos, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);

        // rodapé
        JPanel sul = new JPanel(new BorderLayout(0, 12));
        sul.setOpaque(false);
        JLabel aviso = Estilo.texto("Ambiente de demonstração: nenhum valor real é cobrado. O botão abaixo "
                + "simula a confirmação do pagamento pelo banco.", 440, Font.PLAIN, 12, new Color(0x9a6312));
        JPanel faixa = new JPanel(new BorderLayout());
        faixa.setBackground(new Color(0xfbefd5));
        faixa.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        faixa.add(aviso, BorderLayout.CENTER);
        sul.add(faixa, BorderLayout.NORTH);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botoes.setOpaque(false);
        Estilo.Botao cancelar = Estilo.neutro("Cancelar");
        cancelar.addActionListener(e -> dispose());
        Estilo.Botao confirmar = Estilo.primario("Já paguei — confirmar pagamento");
        confirmar.addActionListener(e -> confirmar());
        botoes.add(cancelar);
        botoes.add(confirmar);
        sul.add(botoes, BorderLayout.SOUTH);
        raiz.add(sul, BorderLayout.SOUTH);

        setContentPane(raiz);
        getRootPane().setDefaultButton(confirmar);
    }

    private JComponent painelPix() {
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 12), 18);
        JLabel qr = new JLabel(new ImageIcon(PagamentoSimulado.qrCode(codigoPix, 230)));
        qr.setHorizontalAlignment(SwingConstants.CENTER);
        c.add(qr, BorderLayout.NORTH);

        JPanel meio = new JPanel(new BorderLayout(0, 8));
        meio.setOpaque(false);
        JLabel dica = Estilo.texto("Escaneie o QR Code no app do seu banco ou use o PIX copia e cola:",
                440, Font.PLAIN, 13, Estilo.MU);
        meio.add(dica, BorderLayout.NORTH);
        JTextArea area = new JTextArea(codigoPix, 3, 30);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(Estilo.ST);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        meio.add(area, BorderLayout.CENTER);
        c.add(meio, BorderLayout.CENTER);

        JPanel copiar = Estilo.acoes(0);
        copiar.setOpaque(false);
        Estilo.Botao b = Estilo.neutro("Copiar código PIX");
        b.addActionListener(e -> copiar(codigoPix));
        copiar.add(b);
        c.add(copiar, BorderLayout.SOUTH);
        return c;
    }

    private JComponent painelBoleto() {
        Estilo.Cartao c = new Estilo.Cartao(new BorderLayout(0, 12), 18);
        JPanel topo = new JPanel();
        topo.setOpaque(false);
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel t = Estilo.rotulo("Linha digitável do boleto", Font.BOLD, 14, Estilo.TX);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel d = Estilo.texto("Pague em qualquer banco, app ou lotérica. O boleto vence em "
                + cobranca.getVencimento().format(DATA) + ".", 440, Font.PLAIN, 13, Estilo.MU);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        topo.add(t);
        topo.add(Box.createVerticalStrut(6));
        topo.add(d);
        c.add(topo, BorderLayout.NORTH);

        JTextField linha = new JTextField(linhaBoleto);
        linha.setEditable(false);
        linha.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        linha.setBackground(Estilo.ST);
        linha.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        c.add(linha, BorderLayout.CENTER);

        JPanel copiar = Estilo.acoes(0);
        copiar.setOpaque(false);
        Estilo.Botao b = Estilo.neutro("Copiar linha digitável");
        b.addActionListener(e -> copiar(linhaBoleto));
        copiar.add(b);
        c.add(copiar, BorderLayout.SOUTH);
        return c;
    }

    private void selecionar(Pagamento.Metodo m) {
        metodoAtual = m;
        cards.show(metodos, m.name());
        Container pai = btnPix.getParent();
        pai.removeAll();
        btnPix = m == Pagamento.Metodo.PIX ? Estilo.primario("PIX (QR Code)") : Estilo.neutro("PIX (QR Code)");
        btnBoleto = m == Pagamento.Metodo.BOLETO ? Estilo.primario("Boleto") : Estilo.neutro("Boleto");
        btnPix.addActionListener(e -> selecionar(Pagamento.Metodo.PIX));
        btnBoleto.addActionListener(e -> selecionar(Pagamento.Metodo.BOLETO));
        pai.add(btnPix);
        pai.add(btnBoleto);
        pai.revalidate();
        pai.repaint();
    }

    private void copiar(String texto) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto), null);
        Estilo.aviso(this, "Copiado para a área de transferência.");
    }

    private void confirmar() {
        try {
            String transacao = dao.pagar(cobranca.getCobrancaId(), usuarioId, metodoAtual,
                    metodoAtual == Pagamento.Metodo.PIX ? codigoPix : null,
                    metodoAtual == Pagamento.Metodo.BOLETO ? linhaBoleto : null);
            pago = true;
            Estilo.aviso(this, "Pagamento confirmado!\n" + cobranca.getDescricao() + " — "
                    + MOEDA.format(cobranca.getValor()) + " via " + metodoAtual.getRotulo()
                    + "\nProtocolo da transação: " + transacao);
            dispose();
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
        }
    }
}
