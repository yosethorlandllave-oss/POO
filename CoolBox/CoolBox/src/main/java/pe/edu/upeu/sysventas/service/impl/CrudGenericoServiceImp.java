package pe.edu.upeu.sysventas.service.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import pe.edu.upeu.sysventas.exception.ModelNotFoundException;
import pe.edu.upeu.sysventas.exception.ValidacionException;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.service.ICrudGenericoService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CrudGenericoServiceImp<T, ID> implements ICrudGenericoService<T,ID> {

    // Una sola fábrica para toda la aplicación: construirla es costoso.
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    protected abstract ICrudGenericoRepository<T,ID> getRepo();

    /** Coloca en la entidad el id del registro que se está actualizando. */
    protected abstract void fijarId(T t, ID id);

    /**
     * Reglas propias de cada entidad antes de guardar (duplicados, relaciones...).
     * idEnEdicion es null al crear.
     */
    protected void antesDeGuardar(T t, ID idEnEdicion) {
    }

    /** Reglas propias de cada entidad antes de eliminar (registros en uso...). */
    protected void antesDeEliminar(ID id) {
    }

    @Override
    public T save(T t) {
        validar(t);
        antesDeGuardar(t, null);
        return getRepo().save(t);
    }

    @Override
    public T update(ID id, T t) {
        if(!getRepo().existsById(id)){
            throw new ModelNotFoundException("Id no existe: "+id);
        }
        // Sin esto, repo.update() usaría el id que traiga la entidad y podría
        // sobrescribir otro registro.
        fijarId(t, id);
        validar(t);
        antesDeGuardar(t, id);
        return getRepo().update(t);
    }

    @Override
    public List<T> findAll() {
        return getRepo().findAll();
    }

    @Override
    public T findById(ID id) {
        return getRepo().findById(id).orElseThrow(
                ()->new ModelNotFoundException("El id no existe:"+id));
    }

    @Override
    public void delete(ID id) {
        if(!getRepo().existsById(id)){
            throw new ModelNotFoundException("Id no existe: "+id);
        }
        antesDeEliminar(id);
        getRepo().deleteById(id);
    }

    /** Aplica las anotaciones de Bean Validation del modelo; lanza ValidacionException si fallan. */
    protected void validar(T t) {
        Map<String, String> errores = erroresDe(t);
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
    }

    /**
     * Errores de Bean Validation del objeto, por propiedad y en orden alfabético.
     * Lo usan también los formularios para marcar todos los campos a la vez.
     */
    public static <E> Map<String, String> erroresDe(E objeto) {
        List<ConstraintViolation<E>> violaciones = VALIDATOR.validate(objeto).stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .toList();
        Map<String, String> errores = new LinkedHashMap<>();
        for (ConstraintViolation<E> v : violaciones) {
            errores.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
        }
        return errores;
    }

    /** Compara nombres ignorando mayúsculas y espacios en los extremos. */
    protected static boolean mismoNombre(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    /** Quita espacios en los extremos; conserva null. */
    protected static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
