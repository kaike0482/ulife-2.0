package br.com.ulife.ui;

import br.com.ulife.dao.CobrancaDAO;
import br.com.ulife.dao.UsuarioDAO;
import br.com.ulife.model.Cobranca;
import br.com.ulife.model.Usuario;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Gestão de cobranças (coordenador): acompanhar inadimplência, gerar e cancelar cobranças. */
public class PainelGestaoPagamentos extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] FILTROS = {"Todas", "Em aberto", "Vencidas", "Pagas", "Canceladas"};
    private static final Cobranca.Status[] STATUS = {null, Cobranca.Status.ABERTA, Cobranca.Status.VENCIDA,
            Cobranca.Status.PAGA, Cobranca.Status.CANCELADA};

    private final CobrancaDAO dao = new CobrancaDAO();
    private final CardLayout cards = new CardLayout();
    private final JPanel raiz = new JPanel(cards);

    private final JPanel resumo = new JPanel(new GridLayout(1, 4, 16, 0));
    private final JTextField txtBusca = Estilo.campo();
    private final JComboBox<String> cmbStatus = new JComboBox<>(FILTROS);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Aluno", "Descrição", "Vencimento", "Valor", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private List<Cobranca> exibidas = new ArrayList<>();

    private final JComboBox<Usuario> cmbAluno = new JComboBox<>();
    private final JTextField txtDescricao = Estilo.campo();
    private final JTextField txtValor = Estilo.campo();
    private final JTextField txtVencimento = Estilo.campo();
    private final JLabel lblErro = Estilo.rotulo(" ", Font.PLAIN, 13, Estilo.ERRO);

    public PainelGestaoPagamentos() {
        super(new BorderLayout());
        setOpaque(false);
        raiz.setOpaque(false);
        resumo.setOpaque(false);
        raiz.add(montarLista(), "lista");
        raiz.add(montarForm(), "form");
        add(raiz, BorderLayout.CENTER);
        recarregar();
    }

    private JComponent montarLista() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);

        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        txtBusca.setToolTipText("Consultar por aluno, usuário ou descrição");
        txtBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recarregar(); }
            public void removeUpdate(DocumentEvent e) { recarregar(); }
            public void changedUpdate(DocumentEvent e) { recarregar(); }
        });
        cmbStatus.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbStatus.addActionListener(e -> recarregar());
        JPanel busca = new JPanel(new BorderLayout(8, 0));
        busca.setOpaque(false);
        busca.add(Estilo.rotulo("Consultar:", Font.PLAIN, 14, Estilo.MU), BorderLayout.WEST);
        busca.add(txtBusca, BorderLayout.CENTER);
        busca.add(cmbStatus, BorderLayout.EAST);
        barra.add(busca, BorderLayout.CENTER);
        Estilo.Botao nova = Estilo.primario("+ Nova cobrança");
        nova.addActionListener(e -> abrirForm());
        barra.add(nova, BorderLayout.EAST);

        Estilo.estilizarTabela(tabela, "Status");
        JPanel sp = Estilo.tabelaSemRolagem(tabela);

        JPanel acoes = Estilo.acoes(10);
        acoes.setOpaque(false);
        Estilo.Botao cancelar = Estilo.perigo("Cancelar cobrança");
        cancelar.addActionListener(e -> cancelar());
        acoes.add(cancelar);
        acoes.add(Estilo.rotulo("Selecione uma cobrança na lista.", Font.PLAIN, 13, Estilo.MU));

        JPanel topo = new JPanel(new BorderLayout(0, 16));
        topo.setOpaque(false);
        topo.add(resumo, BorderLayout.NORTH);
        topo.add(barra, BorderLayout.SOUTH);

        JPanel recebidos = new JPanel(new BorderLayout(0, 12));
        recebidos.setOpaque(false);
        recebidos.add(Estilo.rotulo("Pagamentos recebidos", Font.BOLD, 18, Estilo.TX), BorderLayout.NORTH);
        recebidos.add(new PainelRecebimentos(false), BorderLayout.CENTER);
        JPanel sul = new JPanel(new BorderLayout(0, 28));
        sul.setOpaque(false);
        sul.add(acoes, BorderLayout.NORTH);
        sul.add(recebidos, BorderLayout.CENTER);

        p.add(topo, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(sul, BorderLayout.SOUTH);
        return p;
    }

    private void recarregar() {
        try {
            exibidas = dao.listarTodas(txtBusca.getText(), STATUS[cmbStatus.getSelectedIndex()]);
            atualizarResumo(dao.listarTodas("", null));
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
            return;
        }
        modelo.setRowCount(0);
        for (Cobranca c : exibidas) {
            modelo.addRow(new Object[]{c.getAlunoNome(), c.getDescricao(), c.getVencimento().format(FMT),
                    DialogoPagamento.MOEDA.format(c.getValor()), c.getStatus().getRotulo()});
        }
    }

    private void atualizarResumo(List<Cobranca> todas) {
        BigDecimal aberto = BigDecimal.ZERO, vencido = BigDecimal.ZERO, pago = BigDecimal.ZERO;
        java.util.Set<Integer> inadimplentes = new java.util.HashSet<>();
        for (Cobranca c : todas) {
            switch (c.getStatus()) {
                case ABERTA: aberto = aberto.add(c.getValor()); break;
                case VENCIDA: vencido = vencido.add(c.getValor()); inadimplentes.add(c.getUsuarioId()); break;
                case PAGA: pago = pago.add(c.getValor()); break;
                default: break;
            }
        }
        resumo.removeAll();
        int n = inadimplentes.size();
        resumo.add(Estilo.indicador("Recebido", DialogoPagamento.MOEDA.format(pago), "cobranças pagas", Estilo.OK));
        resumo.add(Estilo.indicador("Em aberto", DialogoPagamento.MOEDA.format(aberto), "a vencer", Estilo.AC));
        resumo.add(Estilo.indicador("Vencidas", DialogoPagamento.MOEDA.format(vencido),
                "inadimplência", vencido.signum() > 0 ? Estilo.ERRO : Estilo.MU));
        resumo.add(Estilo.indicador("Alunos inadimplentes", String.valueOf(n), "com cobrança vencida",
                n > 0 ? Estilo.ERRO : Estilo.OK));
        resumo.revalidate();
        resumo.repaint();
    }

    private void cancelar() {
        int row = tabela.getSelectedRow();
        if (row < 0 || row >= exibidas.size()) {
            Estilo.aviso(this, "Selecione uma cobrança na lista.");
            return;
        }
        Cobranca c = exibidas.get(row);
        if (!c.isPagavel()) {
            Estilo.aviso(this, "Só é possível cancelar cobranças em aberto ou vencidas.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "Cancelar \"" + c.getDescricao() + "\" de " + c.getAlunoNome()
                + " (" + DialogoPagamento.MOEDA.format(c.getValor()) + ")?", "Cancelar cobrança",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            try {
                dao.cancelar(c.getCobrancaId());
                recarregar();
            } catch (RuntimeException ex) {
                Estilo.erro(this, ex.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ nova cobrança

    private JComponent montarForm() {
        cmbAluno.setFont(Estilo.fonte(Font.PLAIN, 14));
        cmbAluno.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v == null ? "" : ((Usuario) v).getNome(), i, s, f);
            }
        });
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);
        p.add(Estilo.rotulo("Nova cobrança", Font.BOLD, 18, Estilo.TX), BorderLayout.NORTH);

        Estilo.Cartao c = new Estilo.Cartao(new GridBagLayout(), 24);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 16, 22);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        PainelUsuarios.campo(c, g, 0, 0, "Aluno", cmbAluno);
        PainelUsuarios.campo(c, g, 1, 0, "Descrição (ex.: Mensalidade 12/2026)", txtDescricao);
        PainelUsuarios.campo(c, g, 0, 1, "Valor (R$)", txtValor);
        PainelUsuarios.campo(c, g, 1, 1, "Vencimento (dd/mm/aaaa)", txtVencimento);
        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        c.add(lblErro, g);
        JPanel btns = Estilo.acoes(12);
        btns.setOpaque(false);
        Estilo.Botao salvar = Estilo.primario("Gerar cobrança");
        salvar.addActionListener(e -> salvar());
        Estilo.Botao cancelar = Estilo.neutro("Cancelar");
        cancelar.addActionListener(e -> cards.show(raiz, "lista"));
        btns.add(salvar);
        btns.add(cancelar);
        g.gridy = 3;
        c.add(btns, g);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(c, BorderLayout.NORTH);
        p.add(wrap, BorderLayout.CENTER);
        return p;
    }

    private void abrirForm() {
        lblErro.setText(" ");
        cmbAluno.removeAllItems();
        try {
            for (Usuario u : new UsuarioDAO().listarPorPerfil(Usuario.Perfil.ALUNO)) {
                cmbAluno.addItem(u);
            }
        } catch (RuntimeException ex) {
            Estilo.erro(this, ex.getMessage());
            return;
        }
        if (cmbAluno.getItemCount() == 0) {
            Estilo.aviso(this, "Não há alunos cadastrados.");
            return;
        }
        txtDescricao.setText("Mensalidade " + LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("MM/yyyy")));
        txtValor.setText("890,00");
        txtVencimento.setText(LocalDate.now().plusDays(30).format(FMT));
        cards.show(raiz, "form");
        txtDescricao.requestFocusInWindow();
    }

    private void salvar() {
        Usuario aluno = (Usuario) cmbAluno.getSelectedItem();
        String desc = txtDescricao.getText().trim();
        if (aluno == null || desc.isEmpty() || desc.length() > 150) {
            lblErro.setText("Informe o aluno e uma descrição de até 150 caracteres.");
            return;
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(txtValor.getText().trim().replace(".", "").replace(',', '.'));
            if (valor.signum() <= 0 || valor.scale() > 2 || valor.compareTo(new BigDecimal("99999999")) > 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            lblErro.setText("Valor inválido. Use um valor positivo, como 890,00.");
            return;
        }
        LocalDate venc;
        try {
            venc = LocalDate.parse(txtVencimento.getText().trim(), FMT);
        } catch (DateTimeParseException ex) {
            lblErro.setText("Data inválida. Use o formato dd/mm/aaaa.");
            return;
        }
        try {
            dao.inserir(aluno.getUsuarioId(), desc, valor, venc);
        } catch (RuntimeException ex) {
            lblErro.setText(ex.getMessage());
            return;
        }
        cards.show(raiz, "lista");
        recarregar();
    }
}
