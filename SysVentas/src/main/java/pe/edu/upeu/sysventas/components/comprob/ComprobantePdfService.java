package pe.edu.upeu.sysventas.components.comprob;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import pe.edu.upeu.sysventas.dto.comprobante.Comprobante;
import pe.edu.upeu.sysventas.dto.comprobante.ItemComprobante;

import java.awt.*;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Boleta en PDF con el estilo de la tienda Coolbox (blanco, rojo y negro)
 * y el logo en la esquina superior izquierda.
 */
public class ComprobantePdfService {
    private static final Color ROJO  = new Color(0xE3, 0x06, 0x13);
    private static final Color NEGRO = new Color(0x1A, 0x1A, 0x1A);
    private static final Color GRIS  = new Color(0x6B, 0x6B, 0x6B);
    private static final Color CLARO = new Color(0xF7, 0xF7, 0xF7);
    private static final Color FILA  = new Color(0xFA, 0xFA, 0xFA);
    private static final Color LINEA = new Color(0xE3, 0xE3, 0xE3);

    private static final Font F_NORMAL  = FontFactory.getFont(FontFactory.HELVETICA, 9, NEGRO);
    private static final Font F_GRIS    = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);
    private static final Font F_PEQUE   = FontFactory.getFont(FontFactory.HELVETICA, 7, GRIS);
    private static final Font F_BOLD    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NEGRO);
    private static final Font F_EMPRESA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NEGRO);
    private static final Font F_SERIE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, ROJO);
    private static final Font F_BLANCO  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font F_TIPO    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font F_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, NEGRO);
    private static final Font F_SON     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, ROJO);
    private static final Font F_TOTAL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font F_DCTO    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, ROJO);
    private static final Font F_GRACIAS = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, NEGRO);

    public void exportar(Comprobante boleta, Path destino) throws Exception {
        try (OutputStream os = Files.newOutputStream(destino)) {
            exportar(boleta, os);
        }
    }

    public void exportar(Comprobante boleta, OutputStream os) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 30, 36);
        PdfWriter.getInstance(doc, os);
        doc.open();

        doc.add(franjaRoja());
        doc.add(espacio(8));
        doc.add(tablaCabecera(boleta));
        doc.add(espacio(8));
        doc.add(tituloSeccion("DATOS DEL CLIENTE"));
        doc.add(tablaCliente(boleta));
        doc.add(espacio(8));
        doc.add(tituloSeccion("DETALLE DE LA COMPRA"));
        doc.add(tablaItems(boleta));
        doc.add(espacio(8));
        doc.add(tablaTotales(boleta));
        doc.add(espacio(10));
        doc.add(pie(boleta));

        doc.close();
    }

    private PdfPTable franjaRoja() {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setFixedHeight(5);
        c.setBackgroundColor(ROJO);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        return t;
    }

    // Logo en la esquina + datos de la tienda + recuadro de la boleta
    private PdfPTable tablaCabecera(Comprobante b) {
        PdfPTable tabla = new PdfPTable(new float[]{12, 50, 38});
        tabla.setWidthPercentage(100);

        PdfPCell logo;
        try {
            URL url = getClass().getResource("/img/logo-coolbox.png");
            Image img = Image.getInstance(url);
            img.scaleToFit(54, 54);
            logo = new PdfPCell(img, false);
        } catch (Exception e) {
            logo = new PdfPCell(new Phrase(""));
        }
        logo.setBorder(Rectangle.NO_BORDER);
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logo.setPaddingTop(4);

        PdfPCell emisor = new PdfPCell();
        emisor.setBorder(Rectangle.NO_BORDER);
        emisor.setPaddingRight(10);
        emisor.addElement(new Paragraph(b.getRazonSocial(), F_EMPRESA));
        emisor.addElement(new Paragraph("RUC: " + b.getRucEmisor(), F_BOLD));
        emisor.addElement(new Paragraph(b.getDireccion(), F_GRIS));
        emisor.addElement(new Paragraph(b.getUbigeo(), F_GRIS));
        emisor.addElement(new Paragraph("Tel: " + b.getTelefono(), F_GRIS));

        PdfPTable caja = new PdfPTable(1);
        caja.setWidthPercentage(100);
        PdfPCell tipo = new PdfPCell(new Phrase(b.getTipoDocumento(), F_TIPO));
        tipo.setBackgroundColor(ROJO);
        tipo.setHorizontalAlignment(Element.ALIGN_CENTER);
        tipo.setPadding(6);
        tipo.setBorderColor(ROJO);
        tipo.setBorderWidth(1.5f);
        caja.addCell(tipo);

        PdfPCell datos = new PdfPCell();
        datos.setBorderColor(ROJO);
        datos.setBorderWidth(1.5f);
        datos.setPadding(7);
        agregarCentrado(datos, "R.U.C. " + b.getRucEmisor(), F_BOLD);
        agregarCentrado(datos, b.getSerie() + " - " + b.getNumero(), F_SERIE);
        String hora = b.getHora() != null ? "   Hora: " + b.getHora() : "";
        agregarCentrado(datos, "Fecha: " + b.getFechaEmision() + hora, F_GRIS);
        caja.addCell(datos);

        PdfPCell docCelda = new PdfPCell(caja);
        docCelda.setBorder(Rectangle.NO_BORDER);

        tabla.addCell(logo);
        tabla.addCell(emisor);
        tabla.addCell(docCelda);
        return tabla;
    }

    private PdfPTable tablaCliente(Comprobante b) {
        PdfPTable tabla = new PdfPTable(new float[]{14, 46, 14, 26});
        tabla.setWidthPercentage(100);
        agregarFila(tabla, b.getTipoDocCliente(), b.getNroDocCliente(), "N° artículos", String.valueOf(numeroArticulos(b)));
        agregarFila(tabla, "Nombre", b.getNombreCliente(), "Cajero", b.getCajero() != null ? b.getCajero() : "-");
        agregarFila(tabla, "Dirección", b.getDireccionCliente(), "Moneda", "PEN".equals(b.getMoneda()) ? "Soles (PEN)" : b.getMoneda());
        return tabla;
    }

    private PdfPTable tablaItems(Comprobante b) {
        PdfPTable tabla = new PdfPTable(new float[]{8, 7, 44, 14, 13, 14});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.addCell(celdaHeader("COD.", Element.ALIGN_LEFT));
        tabla.addCell(celdaHeader("CANT.", Element.ALIGN_CENTER));
        tabla.addCell(celdaHeader("DESCRIPCIÓN", Element.ALIGN_LEFT));
        tabla.addCell(celdaHeader("P. UNIT", Element.ALIGN_RIGHT));
        tabla.addCell(celdaHeader("DCTO.", Element.ALIGN_RIGHT));
        tabla.addCell(celdaHeader("SUBTOTAL", Element.ALIGN_RIGHT));

        for (int i = 0; i < b.getItems().size(); i++) {
            ItemComprobante item = b.getItems().get(i);
            Color bg = (i % 2 == 0) ? Color.WHITE : FILA;
            tabla.addCell(celdaDato(item.getCodigo() != null ? item.getCodigo() : "-", Element.ALIGN_LEFT, bg, F_GRIS));
            tabla.addCell(celdaDato(formatoCantidad(item.getCantidad()), Element.ALIGN_CENTER, bg, F_NORMAL));
            boolean enPromo = item.getDescuento() != null && item.getDescuento().signum() > 0;
            String descripcion = item.getDescripcion();
            if (enPromo && item.getPrecioAntes() != null) {
                descripcion += "\nPromoción · Antes: S/ " + item.getPrecioAntes();
            }
            tabla.addCell(celdaDato(descripcion, Element.ALIGN_LEFT, bg, F_NORMAL));
            tabla.addCell(celdaDato("S/ " + item.getPrecioUnitario(), Element.ALIGN_RIGHT, bg, F_NORMAL));
            tabla.addCell(celdaDato(enPromo ? "-S/ " + item.getDescuento() : "-", Element.ALIGN_RIGHT, bg, enPromo ? F_DCTO : F_GRIS));
            tabla.addCell(celdaDato("S/ " + item.getSubtotal(), Element.ALIGN_RIGHT, bg, F_BOLD));
        }
        return tabla;
    }

    private PdfPTable tablaTotales(Comprobante b) {
        PdfPTable tabla = new PdfPTable(new float[]{52, 48});
        tabla.setWidthPercentage(100);

        // Izquierda: monto en letras y agradecimiento
        PdfPCell izq = new PdfPCell();
        izq.setBorder(Rectangle.NO_BORDER);
        izq.setPaddingRight(12);
        PdfPTable son = new PdfPTable(1);
        son.setWidthPercentage(100);
        PdfPCell sonCelda = new PdfPCell();
        sonCelda.setBackgroundColor(CLARO);
        sonCelda.setBorder(Rectangle.LEFT);
        sonCelda.setBorderColorLeft(ROJO);
        sonCelda.setBorderWidthLeft(3);
        sonCelda.setPadding(8);
        sonCelda.addElement(new Paragraph("SON:", F_SON));
        sonCelda.addElement(new Paragraph(NumeroALetras.soles(b.getTotal()), F_BOLD));
        son.addCell(sonCelda);
        izq.addElement(son);
        Paragraph gracias = new Paragraph("¡Gracias por tu compra en Coolbox!", F_GRACIAS);
        gracias.setSpacingBefore(8);
        izq.addElement(gracias);
        BigDecimal ahorro = b.getTotalDescuento() != null ? b.getTotalDescuento() : BigDecimal.ZERO;
        if (ahorro.signum() > 0) {
            PdfPTable caja = new PdfPTable(1);
            caja.setWidthPercentage(100);
            caja.setSpacingBefore(6);
            PdfPCell c = new PdfPCell(new Phrase("¡Ahorraste S/ " + ahorro + " con las promociones Coolbox!", F_BLANCO));
            c.setBackgroundColor(ROJO);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(6);
            caja.addCell(c);
            izq.addElement(caja);
        }

        // Derecha: totales como en la boleta electrónica
        PdfPCell der = new PdfPCell();
        der.setBorder(Rectangle.NO_BORDER);
        PdfPTable montos = new PdfPTable(new float[]{58, 42});
        montos.setWidthPercentage(100);
        BigDecimal cero = BigDecimal.ZERO.setScale(2);
        agregarMonto(montos, "OP. GRAVADAS", b.getOpGravadas(), false);
        agregarMonto(montos, "OP. GRATUITA", cero, false);
        agregarMonto(montos, "OP. EXONERADO", cero, false);
        agregarMonto(montos, "OP. INAFECTO", cero, false);
        agregarMonto(montos, "TOTAL DESCUENTO", b.getTotalDescuento() != null ? b.getTotalDescuento() : cero, false);
        agregarMonto(montos, "I.G.V. (18%)", b.getIgv(), false);
        agregarMonto(montos, "TOTAL VENTA", b.getTotal(), true);
        der.addElement(montos);

        tabla.addCell(izq);
        tabla.addCell(der);
        return tabla;
    }

    private PdfPTable pie(Comprobante b) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.TOP);
        c.setBorderColorTop(LINEA);
        c.setPaddingTop(8);
        Paragraph rep = new Paragraph("Representación impresa de la Boleta de Venta Electrónica", F_BOLD);
        rep.setAlignment(Element.ALIGN_CENTER);
        Paragraph cambios = new Paragraph("Cambios y devoluciones: plazo de 7 días, productos nuevos y sin uso. "
                + "Revisa las políticas y garantía en tienda.", F_GRIS);
        cambios.setAlignment(Element.ALIGN_CENTER);
        Paragraph sistema = new Paragraph("Sistema Coolbox · Proyecto académico de POO - UPeU Juliaca", F_PEQUE);
        sistema.setAlignment(Element.ALIGN_CENTER);
        c.addElement(rep);
        c.addElement(cambios);
        c.addElement(sistema);
        t.addCell(c);
        return t;
    }

    // -- Ayudantes --

    private Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private PdfPTable tituloSeccion(String texto) {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 98.8f});
        t.setWidthPercentage(100);
        PdfPCell barra = new PdfPCell(new Phrase(""));
        barra.setBackgroundColor(ROJO);
        barra.setBorder(Rectangle.NO_BORDER);
        PdfPCell txt = new PdfPCell(new Phrase(texto, F_SECCION));
        txt.setBorder(Rectangle.NO_BORDER);
        txt.setPaddingLeft(6);
        txt.setPaddingBottom(4);
        t.addCell(barra);
        t.addCell(txt);
        t.setSpacingAfter(4);
        return t;
    }

    private void agregarCentrado(PdfPCell celda, String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(p);
    }

    private PdfPCell celdaHeader(String texto, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, F_BLANCO));
        c.setBackgroundColor(NEGRO);
        c.setPadding(6);
        c.setHorizontalAlignment(align);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell celdaDato(String texto, int align, Color bg, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(LINEA);
        return c;
    }

    private void agregarFila(PdfPTable tabla, String e1, String v1, String e2, String v2) {
        tabla.addCell(celdaCliente(e1, F_BOLD));
        tabla.addCell(celdaCliente(v1, F_NORMAL));
        tabla.addCell(celdaCliente(e2, F_BOLD));
        tabla.addCell(celdaCliente(v2, F_NORMAL));
    }

    private PdfPCell celdaCliente(String texto, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, font));
        c.setBackgroundColor(CLARO);
        c.setPadding(5);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private void agregarMonto(PdfPTable tabla, String etiqueta, BigDecimal valor, boolean esTotal) {
        Font fe = esTotal ? F_TOTAL : F_BOLD;
        Color bg = esTotal ? ROJO : Color.WHITE;
        String texto = "S/ " + (valor != null ? valor.toString() : "0.00");

        PdfPCell lEtiq = new PdfPCell(new Phrase(etiqueta, fe));
        lEtiq.setBackgroundColor(bg);
        lEtiq.setPadding(esTotal ? 7 : 4);
        lEtiq.setBorder(Rectangle.BOTTOM);
        lEtiq.setBorderColor(esTotal ? ROJO : LINEA);

        PdfPCell lVal = new PdfPCell(new Phrase(texto, fe));
        lVal.setBackgroundColor(bg);
        lVal.setPadding(esTotal ? 7 : 4);
        lVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lVal.setBorder(Rectangle.BOTTOM);
        lVal.setBorderColor(esTotal ? ROJO : LINEA);

        tabla.addCell(lEtiq);
        tabla.addCell(lVal);
    }

    private long numeroArticulos(Comprobante b) {
        double total = 0;
        for (ItemComprobante item : b.getItems()) {
            total += item.getCantidad() == null ? 0 : item.getCantidad();
        }
        return Math.round(total);
    }

    private String formatoCantidad(Double cantidad) {
        if (cantidad == null) return "";
        return cantidad == Math.floor(cantidad) ? String.valueOf(cantidad.longValue()) : String.valueOf(cantidad);
    }
}
