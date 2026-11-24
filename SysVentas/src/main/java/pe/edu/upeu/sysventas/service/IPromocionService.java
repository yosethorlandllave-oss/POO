package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.model.Promocion;

import java.util.List;

public interface IPromocionService extends ICrudGenericoService<Promocion, Long> {
    /** Promociones activas cuya fecha de hoy está dentro de su vigencia. */
    List<Promocion> listarVigentes();
}
