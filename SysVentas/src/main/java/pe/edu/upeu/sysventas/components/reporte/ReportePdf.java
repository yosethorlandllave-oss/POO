package pe.edu.upeu.sysventas.components.reporte;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Motor que convierte un {@link Reporte} + valores de parámetros en un PDF (bytes).
 * Organiza el documento en tres zonas:
 * <ul>
 *   <li><b>Cabecera</b>: logo, datos de empresa, título, subtítulo y filtros aplicados
 *       (en las páginas siguientes se repite una mini-cabecera).</li>
 *   <li><b>Cuerpo</b>: secciones previas, tabla (zebra, agrupación, subtotales, total),
 *       resumen de indicadores y secciones posteriores.</li>
 *   <li><b>Pie</b>: notas, usuario, fecha de emisión y "Página X de Y" en cada hoja.</li>
 * </ul>
 */
public final class ReportePdf {

    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReportePdf() {}

    /** Genera el PDF completo. Ejecuta la fuente de datos, así que conviene llamarlo fuera del hilo de UI. */
    public static byte[] generar(Reporte rep, Map<String, Object> params) throws Exception {
        List<Object[]> filas = rep.fuente.obtener(params);
        if (filas == null) filas = List.of();
        return new Generador(rep, params, filas).generar();
    }

    // ------------------------------------------------------------------
    private static final class Generador {
        final Reporte rep;
        final Map<String, Object> params;
        final List<Object[]> filas;
        final Color primario, acento, claro = new Color(0xf4, 0xf6, 0xf7);
        final Font fNormal, fBold, fTitulo, fSub, fBlanco, fPequena;

        Generador(Reporte rep, Map<String, Object> params, List<Object[]> filas) {
            this.rep = rep; this.params = params; this.filas = filas;
            this.primario = Color.decode(rep.colorPrimarioHex);
            this.acento = Color.decode(rep.colorAcentoHex);
            fNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, primario);
            fBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, primario);
            fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, acento);
            fSub = FontFactory.getFont(FontFactory.HELVETICA, 10, primario);
            fBlanco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            fPequena = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        }

        byte[] generar() throws Exception {
            var rect = rep.orientacion == Reporte.Orientacion.HORIZONTAL ? PageSize.A4.rotate() : PageSize.A4;
            Document doc = new Document(rect, 36, 36, 36, 50);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, bos);
            writer.setPageEvent(new PieYMiniCabecera());
            doc.open();

            // ---------- CABECERA ----------
            doc.add(cabecera());
            doc.add(espacio(6));
            doc.add(tituloReporte());
            if (rep.mostrarFiltros) {
                PdfPTable f = filtros();
                if (f != null) { doc.add(espacio(6)); doc.add(f); }
            }
            doc.add(espacio(10));

            // ---------- CUERPO ----------
            var ctx = new Reporte.ContextoReporte(rep, params, filas);
            for (Reporte.Seccion s : rep.seccionesAntes) s.escribir(doc, ctx);

            doc.add(filas.isEmpty() ? sinDatos() : tabla());

            if (rep.resumen != null && !filas.isEmpty()) {
                Map<String, String> r = rep.resumen.apply(filas);
                if (r != null && !r.isEmpty()) { doc.add(espacio(10)); doc.add(resumen(r)); }
            }
            for (Reporte.Seccion s : rep.seccionesDespues) s.escribir(doc, ctx);

            // ---------- NOTAS (parte del pie, tras el cuerpo) ----------
            if (!rep.notasPie.isEmpty()) {
                doc.add(espacio(12));
                for (String n : rep.notasPie) doc.add(new Paragraph(n, fPequena));
            }
            doc.close();
            return bos.toByteArray();
        }

        // ---------------- CABECERA ----------------
        PdfPTable cabecera() throws Exception {
            Image logo = cargarLogo();
            PdfPTable t = new PdfPTable(logo != null ? new float[]{18, 52, 30} : new float[]{70, 30});
            t.setWidthPercentage(100);
            if (logo != null) {
                logo.scaleToFit(60, 60);
                PdfPCell c = new PdfPCell(logo, false);
                c.setBorder(Rectangle.NO_BORDER);
                c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                t.addCell(c);
            }
            PdfPCell emp = new PdfPCell();
            emp.setBorder(Rectangle.NO_BORDER);
            for (int i = 0; i < rep.lineasEmpresa.size(); i++) {
                emp.addElement(new Paragraph(rep.lineasEmpresa.get(i),
                        i == 0 ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primario) : fNormal));
            }
            t.addCell(emp);

            PdfPCell der = new PdfPCell();
            der.setBorder(Rectangle.NO_BORDER);
            Paragraph p1 = new Paragraph("Emitido: " + LocalDateTime.now().format(F_EMISION), fPequena);
            p1.setAlignment(Element.ALIGN_RIGHT);
            der.addElement(p1);
            if (!rep.usuario.isBlank()) {
                Paragraph p2 = new Paragraph("Usuario: " + rep.usuario, fPequena);
                p2.setAlignment(Element.ALIGN_RIGHT);
                der.addElement(p2);
            }
            t.addCell(der);
            return t;
        }

        Image cargarLogo() {
            try {
                if (rep.logoBytes != null) return Image.getInstance(rep.logoBytes);
                if (rep.logoRecurso != null) {
                    try (InputStream in = ReportePdf.class.getResourceAsStream(rep.logoRecurso)) {
                        if (in != null) return Image.getInstance(in.readAllBytes());
                    }
                }
            } catch (Exception ignored) { /* sin logo */ }
            return null;
        }

        PdfPTable tituloReporte() {
            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderColor(acento);
            c.setBorderWidth(2);
            c.setPaddingBottom(6);
            Paragraph titulo = new Paragraph(rep.titulo.toUpperCase(), fTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            c.addElement(titulo);
            if (!rep.subtitulo.isBlank()) {
                Paragraph s = new Paragraph(rep.subtitulo, fSub);
                s.setAlignment(Element.ALIGN_CENTER);
                c.addElement(s);
            }
            t.addCell(c);
            return t;
        }

        /** Cuadro "Filtros aplicados" con los valores de los parámetros. */
        PdfPTable filtros() {
            List<String[]> pares = new ArrayList<>();
            for (ParametroReporte p : rep.parametros) {
                Object v = params.get(p.clave());
                if (v == null || v.toString().isBlank()) continue;
                pares.add(new String[]{p.etiqueta(), textoValor(v)});
            }
            if (pares.isEmpty()) return null;
            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            Phrase fr = new Phrase();
            fr.add(new Chunk("Filtros:  ", fBold));
            for (int i = 0; i < pares.size(); i++) {
                fr.add(new Chunk(pares.get(i)[0] + ": ", fBold));
                fr.add(new Chunk(pares.get(i)[1] + (i < pares.size() - 1 ? "     " : ""), fNormal));
            }
            PdfPCell c = new PdfPCell(fr);
            c.setBackgroundColor(claro);
            c.setBorderColor(claro);
            c.setPadding(6);
            t.addCell(c);
            return t;
        }

        String textoValor(Object v) {
            if (v instanceof LocalDate d) return d.format(F_FECHA);
            if (v instanceof Boolean b) return b ? "Sí" : "No";
            return v.toString();
        }

        // ---------------- CUERPO ----------------
        PdfPTable tabla() {
            List<ColumnaReporte> cols = rep.columnas;
            boolean num = rep.numerarFilas;
            int n = cols.size() + (num ? 1 : 0);
            float[] anchos = new float[n];
            int k = 0;
            if (num) anchos[k++] = 0.6f;
            for (ColumnaReporte c : cols) anchos[k++] = c.ancho();

            PdfPTable t = new PdfPTable(anchos);
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            t.setSplitLate(false);

            if (num) t.addCell(celda("N°", fBlanco, primario, Element.ALIGN_CENTER, primario));
            for (ColumnaReporte c : cols)
                t.addCell(celda(c.titulo(), fBlanco, primario, alinear(c), primario));

            int agr = rep.columnaAgrupar;
            Object grupoActual = null;
            double[] subtotal = new double[cols.size()];
            double[] total = new double[cols.size()];
            int correlativo = 0, enGrupo = 0;

            for (int i = 0; i < filas.size(); i++) {
                Object[] fila = filas.get(i);
                if (agr >= 0 && agr < fila.length) {
                    Object g = fila[agr];
                    if (i == 0 || !Objects.equals(g, grupoActual)) {
                        if (i > 0) filaSubtotal(t, subtotal, enGrupo);
                        grupoActual = g; enGrupo = 0;
                        java.util.Arrays.fill(subtotal, 0);
                        PdfPCell gc = celda(cols.get(agr).titulo() + ": " + cols.get(agr).formatear(g),
                                fBold, new Color(0xdf, 0xe6, 0xe9), Element.ALIGN_LEFT, claro);
                        gc.setColspan(n);
                        t.addCell(gc);
                    }
                }
                correlativo++; enGrupo++;
                Color bg = (correlativo % 2 == 0) ? claro : Color.WHITE;
                if (num) t.addCell(celda(String.valueOf(correlativo), fNormal, bg, Element.ALIGN_CENTER, claro));
                for (int c = 0; c < cols.size(); c++) {
                    Object v = c < fila.length ? fila[c] : null;
                    t.addCell(celda(cols.get(c).formatear(v), fNormal, bg, alinear(cols.get(c)), claro));
                    if (cols.get(c).esSumable() && v != null) {
                        try {
                            double d = ColumnaReporte.aNumero(v);
                            subtotal[c] += d; total[c] += d;
                        } catch (NumberFormatException ignored) { /* no numérico */ }
                    }
                }
            }
            if (agr >= 0) filaSubtotal(t, subtotal, enGrupo);

            if (rep.mostrarTotales) filaTotal(t, "TOTAL (" + correlativo + " registros)", total);
            return t;
        }

        private void filaTotal(PdfPTable t, String etiqueta, double[] total) {
            List<ColumnaReporte> cols = rep.columnas;
            int primeraSuma = -1;
            for (int c = 0; c < cols.size(); c++) if (cols.get(c).esSumable()) { primeraSuma = c; break; }
            int inicio = primeraSuma < 0 ? cols.size() : primeraSuma;
            PdfPCell et = celda(etiqueta, fBlanco, acento, Element.ALIGN_RIGHT, acento);
            et.setColspan(Math.max(inicio + (rep.numerarFilas ? 1 : 0), 1));
            t.addCell(et);
            for (int c = inicio; c < cols.size(); c++)
                t.addCell(celda(cols.get(c).esSumable() ? cols.get(c).formatear(total[c]) : "",
                        fBlanco, acento, alinear(cols.get(c)), acento));
        }

        private void filaSubtotal(PdfPTable t, double[] sub, int cantidad) {
            List<ColumnaReporte> cols = rep.columnas;
            int primeraSuma = -1;
            for (int c = 0; c < cols.size(); c++) if (cols.get(c).esSumable()) { primeraSuma = c; break; }
            if (primeraSuma < 0) return;
            int span = primeraSuma + (rep.numerarFilas ? 1 : 0);
            Color bg = new Color(0xec, 0xf0, 0xf1);
            PdfPCell et = celda("Subtotal (" + cantidad + ")", fBold, bg, Element.ALIGN_RIGHT, claro);
            et.setColspan(Math.max(span, 1));
            t.addCell(et);
            for (int c = primeraSuma; c < cols.size(); c++)
                t.addCell(celda(cols.get(c).esSumable() ? cols.get(c).formatear(sub[c]) : "", fBold, bg, alinear(cols.get(c)), claro));
        }

        PdfPTable resumen(Map<String, String> datos) {
            PdfPTable t = new PdfPTable(new float[]{40, 25});
            t.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.setWidthPercentage(45);
            t.setKeepTogether(true);
            PdfPCell cab = celda("RESUMEN", fBlanco, primario, Element.ALIGN_LEFT, primario);
            cab.setColspan(2);
            t.addCell(cab);
            datos.forEach((k, v) -> {
                t.addCell(celda(k, fBold, claro, Element.ALIGN_LEFT, claro));
                t.addCell(celda(v, fNormal, Color.WHITE, Element.ALIGN_RIGHT, claro));
            });
            return t;
        }

        PdfPTable sinDatos() {
            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            PdfPCell c = celda("No se encontraron datos para los criterios seleccionados.",
                    fSub, claro, Element.ALIGN_CENTER, claro);
            c.setPadding(20);
            t.addCell(c);
            return t;
        }

        // ---------------- helpers ----------------
        PdfPCell celda(String texto, Font f, Color bg, int align, Color borde) {
            PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, f));
            c.setBackgroundColor(bg);
            c.setHorizontalAlignment(align);
            c.setBorderColor(borde);
            c.setPadding(4);
            return c;
        }

        int alinear(ColumnaReporte c) {
            return switch (c.alineacionEfectiva()) {
                case DERECHA -> Element.ALIGN_RIGHT;
                case CENTRO -> Element.ALIGN_CENTER;
                default -> Element.ALIGN_LEFT;
            };
        }

        Paragraph espacio(float alto) {
            Paragraph p = new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, alto));
            p.setLeading(alto);
            return p;
        }

        // ---------------- PIE + MINI-CABECERA (evento por página) ----------------
        final class PieYMiniCabecera extends PdfPageEventHelper {
            PdfTemplate total;
            BaseFont bf;

            @Override public void onOpenDocument(PdfWriter w, Document d) {
                total = w.getDirectContent().createTemplate(40, 12);
                try { bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false); }
                catch (Exception e) { throw new IllegalStateException(e); }
            }

            @Override public void onEndPage(PdfWriter w, Document d) {
                PdfContentByte cb = w.getDirectContent();
                float izq = d.left(), der = d.right();
                // línea del pie
                cb.setColorStroke(claro);
                cb.setLineWidth(1);
                cb.moveTo(izq, d.bottom() - 10);
                cb.lineTo(der, d.bottom() - 10);
                cb.stroke();
                cb.setColorFill(Color.GRAY);
                cb.beginText();
                cb.setFontAndSize(bf, 8);
                cb.showTextAligned(Element.ALIGN_LEFT, rep.titulo, izq, d.bottom() - 24, 0);
                cb.endText();
                String txt = "Página " + w.getPageNumber() + " de ";
                cb.beginText();
                cb.setFontAndSize(bf, 8);
                cb.showTextAligned(Element.ALIGN_RIGHT, txt, der - 14, d.bottom() - 24, 0);
                cb.endText();
                cb.addTemplate(total, der - 14, d.bottom() - 24);
                // mini-cabecera desde la 2.ª hoja
                if (w.getPageNumber() > 1) {
                    cb.beginText();
                    cb.setFontAndSize(bf, 8);
                    cb.showTextAligned(Element.ALIGN_LEFT, rep.titulo + " (continuación)", izq, d.top() + 14, 0);
                    cb.endText();
                }
            }

            @Override public void onCloseDocument(PdfWriter w, Document d) {
                total.beginText();
                total.setFontAndSize(bf, 8);
                total.setColorFill(Color.GRAY);
                total.showTextAligned(Element.ALIGN_LEFT, String.valueOf(w.getPageNumber() - 1), 0, 0, 0);
                total.endText();
            }
        }
    }
}
