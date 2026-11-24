package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.dto.SessionManager;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import pe.edu.upeu.sysventas.dto.comprobante.Comprobante;
//import pe.edu.upeu.sysventas.dto.comprobante.ItemComprobante;
import pe.edu.upeu.sysventas.dto.comprobante.Comprobante;
import pe.edu.upeu.sysventas.dto.comprobante.ItemComprobante;
import pe.edu.upeu.sysventas.model.Venta;
import pe.edu.upeu.sysventas.model.VentaDetalle;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.VentaRepository;
import pe.edu.upeu.sysventas.service.IVentaService;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VentaServiceImp extends CrudGenericoServiceImp<Venta, Long>
        implements IVentaService {

    private static final Logger logger = LoggerFactory.getLogger(VentaServiceImp.class);

    private final VentaRepository ventaRepository;
    private final DataSource dataSource;

    public VentaServiceImp(VentaRepository ventaRepository, DataSource dataSource) {
        this.ventaRepository = ventaRepository;
        this.dataSource = dataSource;
    }

    @Override
    protected ICrudGenericoRepository<Venta, Long> getRepo() {
        return ventaRepository;
    }

    @Override
    public File getFile(String filex) {
        File newFolder = new File("jasper");
        Path camino = Paths.get(newFolder.getAbsolutePath() + "/" + filex);
        logger.debug("Ruta Jasper: {}", camino.toAbsolutePath());
        return camino.toFile();
    }

    @Override
    public Comprobante generarComprobante(Venta venta){
        Comprobante b = new Comprobante();
        b.setRucEmisor("20123456789");
        b.setRazonSocial("Coolbox Juliaca");
        b.setDireccion("Jr. San Roman 123 - Juliaca");
        b.setUbigeo("Puno, San Roman, Juliaca");
        b.setTelefono("951782520");

        b.setTipoDocumento("BOLETA DE VENTA ELECTRÓNICA");
        b.setSerie("B001");
        // Número correlativo según el id de la venta (ej: venta 7 -> 00000007)
        b.setNumero(venta.getId() != null ? String.format("%08d", venta.getId()) : "00000001");
        b.setFechaEmision(venta.getFechaGener().toString());
        b.setMoneda("PEN");
        b.setHora(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        String cajero = (venta.getIdUsuario() != null && venta.getIdUsuario().getUsuario() != null)
                ? venta.getIdUsuario().getUsuario() : SessionManager.getInstance().getUserName();
        b.setCajero(cajero != null ? cajero : "-");

        b.setTipoDocCliente(venta.getDniruc().getTipoDocumento().name());
        b.setNroDocCliente(venta.getDniruc().getDniruc());
        b.setNombreCliente(venta.getDniruc().getNombres());
        b.setDireccionCliente(venta.getDniruc().getDireccion() != null ? venta.getDniruc().getDireccion() : "-");

        List<ItemComprobante> items = new ArrayList<>();

        for (VentaDetalle vd : venta.getDetalleVenta()) {
            // Se pasa el precio unitario (antes se pasaba el subtotal y se multiplicaba dos veces por la cantidad)
            ItemComprobante item = new ItemComprobante(vd.getIdProducto().getNombre(), vd.getCantidad(), BigDecimal.valueOf(vd.getPu()));
            item.setCodigo(vd.getIdProducto().getIdProducto() != null ? String.valueOf(vd.getIdProducto().getIdProducto()) : "-");
            // Promoción: si el producto tiene precio anterior mayor, se muestra el ahorro en la boleta
            Double antes = vd.getIdProducto().getPuold();
            if (antes != null && vd.getPu() != null && antes > vd.getPu()) {
                item.setPrecioAntes(BigDecimal.valueOf(antes).setScale(2, RoundingMode.HALF_UP));
                item.setDescuento(BigDecimal.valueOf((antes - vd.getPu()) * vd.getCantidad()).setScale(2, RoundingMode.HALF_UP));
            }
            items.add(item);
        }
        b.setItems(items);
        // Calcular totales
        BigDecimal total = items.stream()
                .map(ItemComprobante::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        b.setOpGravadas(total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP));
        b.setIgv(total.subtract(b.getOpGravadas()).setScale(2, RoundingMode.HALF_UP));
        b.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        b.setTotalDescuento(items.stream().map(ItemComprobante::getDescuento)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP));
        return b;
    }


}
