package br.com.ulife.service;

import br.com.ulife.model.Cobranca;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Geração dos dados de pagamento em AMBIENTE DE DEMONSTRAÇÃO (sandbox): o PIX "copia e cola" segue o
 * formato BR Code (EMV + CRC16) e vira um QR Code real, e o boleto tem linha digitável fictícia.
 * Nenhum valor real é cobrado — a integração com gateway bancário é uma evolução futura (RF-006).
 */
public final class PagamentoSimulado {

    private PagamentoSimulado() {}

    private static String tlv(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** Código PIX "copia e cola" (BR Code estático) para a cobrança. */
    public static String pixCopiaECola(Cobranca c) {
        String valor = c.getValor().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        String txid = "ULIFE" + String.format("%06d", c.getCobrancaId());
        String conta = tlv("00", "br.gov.bcb.pix") + tlv("01", "pagamentos@ulife.edu.br");
        String payload = tlv("00", "01")
                + tlv("26", conta)
                + tlv("52", "0000")
                + tlv("53", "986")
                + tlv("54", valor)
                + tlv("58", "BR")
                + tlv("59", "ULIFE PORTAL DO ALUNO")
                + tlv("60", "SAO BERNARDO")
                + tlv("62", tlv("05", txid))
                + "6304";
        return payload + crc16(payload);
    }

    /** CRC16-CCITT (polinômio 0x1021, valor inicial 0xFFFF) exigido pelo BR Code. */
    static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    /** Linha digitável fictícia (47 dígitos) derivada do número, valor e vencimento da cobrança. */
    public static String linhaDigitavel(Cobranca c) {
        String base = String.format("%010d", c.getCobrancaId())
                + c.getVencimento().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%010d", c.getValor().movePointRight(2).max(BigDecimal.ZERO).longValue());
        StringBuilder d = new StringBuilder("0019");
        d.append(base);
        long semente = base.hashCode() & 0xFFFFFFFFL;
        while (d.length() < 47) {
            semente = (semente * 1103515245L + 12345L) & 0x7FFFFFFFL;
            d.append(semente % 10);
        }
        String s = d.substring(0, 47);
        return s.substring(0, 5) + "." + s.substring(5, 10) + " " + s.substring(10, 15) + "." + s.substring(15, 21)
                + " " + s.substring(21, 26) + "." + s.substring(26, 32) + " " + s.charAt(32) + " " + s.substring(33);
    }

    /** QR Code (imagem) para o texto informado. */
    public static BufferedImage qrCode(String texto, int tamanho) {
        Map<EncodeHintType, Object> dicas = new EnumMap<>(EncodeHintType.class);
        dicas.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        dicas.put(EncodeHintType.MARGIN, 1);
        dicas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        try {
            BitMatrix m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamanho, tamanho, dicas);
            BufferedImage img = new BufferedImage(m.getWidth(), m.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            return img;
        } catch (WriterException e) {
            throw new RuntimeException("Erro ao gerar o QR Code: " + e.getMessage(), e);
        }
    }
}
