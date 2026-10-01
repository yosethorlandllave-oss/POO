package pe.edu.upeu.sysventas.repository;
import pe.edu.upeu.sysventas.model.Compra;
import java.util.List;
import java.util.Optional;
public class CompraRepository extends AbstractJpaRepository<Compra,Long> {
 private long sequence=1;
 @Override protected Long getId(Compra c){return c.getIdCompra();}
 @Override protected void setId(Compra c,Long id){c.setIdCompra(id);}
 @Override protected Long generateId(){return sequence++;}
 @Override public Compra save(Compra c){
  if(c==null)throw new IllegalArgumentException("La compra es obligatoria");
  if(c.getIdCompra()==null)c.setIdCompra(generateId()); else sequence=Math.max(sequence,c.getIdCompra()+1);
  c.calcularTotal(); data.add(c); return c;
 }
 @Override public Compra update(Compra c){
  if(c==null||c.getIdCompra()==null)throw new IllegalArgumentException("Compra e identificador son obligatorios");
  c.calcularTotal(); return super.update(c);
 }
 @Override public Optional<Compra> findById(Long id){return id==null?Optional.empty():super.findById(id);}
 @Override public void deleteById(Long id){if(id!=null)super.deleteById(id);}
 public List<Compra> buscarPorProveedor(String q){
  String f=q==null?"":q.trim().toLowerCase();
  return findAll().stream().filter(c->{
   if(f.isEmpty())return true; if(c.getProveedor()==null)return false;
   String n=c.getProveedor().getNombresRaso(), d=c.getProveedor().getDniruc();
   return (n!=null&&n.toLowerCase().contains(f))||(d!=null&&d.toLowerCase().contains(f));
  }).toList();
 }
}