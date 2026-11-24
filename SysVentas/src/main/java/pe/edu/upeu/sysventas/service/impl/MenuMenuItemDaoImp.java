package pe.edu.upeu.sysventas.service.impl;

import pe.edu.upeu.sysventas.dto.MenuMenuItenTO;
import pe.edu.upeu.sysventas.service.IMenuMenuItemDao;

import java.util.*;

public class MenuMenuItemDaoImp implements IMenuMenuItemDao {
    @Override
    public List<MenuMenuItenTO> listaAccesos(String perfil, Properties idioma) {
        List<MenuMenuItenTO> lista = new ArrayList<>();

        lista.add(new MenuMenuItenTO("miprincipal", "/view/login.fxml",
                idioma.getProperty("menu.nombre.principal"),
                idioma.getProperty("menuitem.nombre.salir"), "Salir", "C"));

        lista.add(new MenuMenuItenTO("miproducto", "/view/main_producto.fxml",
                idioma.getProperty("menu.nombre.producto"), idioma.getProperty("menuitem.nombre.producto"), "Gestión Productos", "T"));

        lista.add(new MenuMenuItenTO("micliente", "/view/main_cliente.fxml",
                "Venta", "Reg. Cliente", "Gestionar Cliente", "T"));
        lista.add(new MenuMenuItenTO("miventa", "/view/main_venta.fxml",
                "Venta", "Reg. Venta", "Gestionar Ventas", "T"));
        lista.add(new MenuMenuItenTO("miproveedor", "/view/main_proveedor.fxml",
                "Compra", "Reg. Proveedor", "Gestionar Proveedores", "T"));
        lista.add(new MenuMenuItenTO("micompra", "/view/main_compra.fxml",
                "Compra", "Reg. Compra", "Gestionar Compras", "T"));
        lista.add(new MenuMenuItenTO("mireporte", "/view/main_reporte.fxml",
                "Reporte", "Reportes PDF", "Reportes", "T"));

        List<MenuMenuItenTO> accesoReal = new ArrayList<>();

        accesoReal.add(lista.get(0));

        switch (perfil) {
            case "Administrador":
                accesoReal.add(lista.get(1)); // miproducto
                accesoReal.add(lista.get(2)); // micliente
                accesoReal.add(lista.get(3)); // miventa (el administrador también vende)
                accesoReal.add(lista.get(4)); // miproveedor
                accesoReal.add(lista.get(5)); // micompra (Yoseth)
                accesoReal.add(lista.get(6)); // mireporte (Sesión 16)
                break;
            case "Root":
                accesoReal = lista;
                break;
            case "Reporte":
                accesoReal.add(lista.get(1)); // miproducto (consulta de productos y stock)
                accesoReal.add(lista.get(2)); // micliente
                accesoReal.add(lista.get(6)); // mireporte (Sesión 16)
                break;
            case "Proveedor":
                accesoReal.add(lista.get(4)); // miproveedor (CRUD de Yerson)
                break;
            default:
                // Perfil desconocido: solo ve la opción Salir (antes cerraba el programa con un error)
                break;
        }
        return accesoReal;

    }

    @Override
    public Map<String, String[]> accesosAutorizados(List<MenuMenuItenTO> accesos) {
        Map<String, String[]> menuConfig = new HashMap<>();
        for (MenuMenuItenTO menu : accesos) {
            menuConfig.put("mi" + menu.getIdNombreObj(),  new String[]{menu.getRutaFile(), menu.getNombreTab(),menu.getTipoTab() });
        }
        return menuConfig;
    }

    @Override
    public int[] contarMenuMunuItem(List<MenuMenuItenTO> data) {
        int menui = 0, menuitem = 0;
        String menuN = "";
        for (MenuMenuItenTO mmiItem : data) {
            if (!mmiItem.getMenunombre().equals(menuN)) {
                menuN = mmiItem.getMenunombre();
                menui++;
            }
            if (!mmiItem.getMenuitemnombre().equals("")) {
                menuitem++;
            }
        }
        return new int[]{menui, menuitem};
    }
}
