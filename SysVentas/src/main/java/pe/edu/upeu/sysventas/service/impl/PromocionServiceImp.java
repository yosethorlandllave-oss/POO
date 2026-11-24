package pe.edu.upeu.sysventas.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.model.Promocion;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.PromocionRepository;
import pe.edu.upeu.sysventas.service.IPromocionService;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public class PromocionServiceImp extends CrudGenericoServiceImp<Promocion, Long> implements IPromocionService {

    private final PromocionRepository promocionRepository;

    @Override
    protected ICrudGenericoRepository<Promocion, Long> getRepo() {
        return promocionRepository;
    }

    @Override
    public List<Promocion> listarVigentes() {
        List<Promocion> vigentes = promocionRepository.findAll().stream()
                .filter(Promocion::isVigente)
                .sorted(Comparator.comparing(Promocion::getIdPromocion))
                .toList();
        if (!vigentes.isEmpty()) return vigentes;
        // Si hoy no hay ninguna vigente, se muestran todas las activas
        return promocionRepository.findAll().stream()
                .filter(p -> "ACTIVO".equalsIgnoreCase(p.getEstado()))
                .sorted(Comparator.comparing(Promocion::getIdPromocion))
                .toList();
    }
}
