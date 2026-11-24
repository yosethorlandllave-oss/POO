package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.model.Usuario;
import pe.edu.upeu.sysventas.repository.ICrudGenericoRepository;
import pe.edu.upeu.sysventas.repository.UsuarioRepository;
import pe.edu.upeu.sysventas.service.IUsuarioService;
import pe.edu.upeu.sysventas.utils.PasswordUtil;

import java.util.Optional;

public class UsuarioServiceImp extends CrudGenericoServiceImp<Usuario,Long> implements IUsuarioService {
    private final UsuarioRepository repo;
    public UsuarioServiceImp(UsuarioRepository repo) {
        this.repo = repo;
    }
    @Override
    protected ICrudGenericoRepository<Usuario, Long> getRepo() {
        return repo;
    }

    /**
     * Login: encripta la clave escrita y la compara con la guardada.
     * Solo deja entrar a usuarios con estado ACTIVO.
     */
    @Override
    public Optional<Usuario> loginUsuario(String usuario, String clave) {
        return repo.findByUsuarioAndClave(usuario, PasswordUtil.encriptar(clave))
                .filter(u -> "ACTIVO".equalsIgnoreCase(u.getEstado()));
    }

    /** Al registrar un usuario, su clave se guarda encriptada. */
    @Override
    public Usuario save(Usuario usuario) {
        encriptarClave(usuario);
        return super.save(usuario);
    }

    /** Al actualizar, si se cambió la clave, también se encripta. */
    @Override
    public Usuario update(Long id, Usuario usuario) {
        encriptarClave(usuario);
        return super.update(id, usuario);
    }

    private void encriptarClave(Usuario usuario) {
        if (usuario.getClave() != null && !PasswordUtil.estaEncriptada(usuario.getClave())) {
            usuario.setClave(PasswordUtil.encriptar(usuario.getClave()));
        }
    }
}
