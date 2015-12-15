package ovh.gorillahack.wazabi.dao;

import java.util.List;

import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ovh.gorillahack.wazabi.domaine.Carte;
import ovh.gorillahack.wazabi.domaine.De;
import ovh.gorillahack.wazabi.domaine.Joueur;

@Stateless
@Local(Dao.class)
@LocalBean
public class DeDaoImpl extends DaoImpl<De>{
	@PersistenceContext(unitName = "wazabi")
	private EntityManager entityManager;
	
    public DeDaoImpl() {
    	super(De.class);
    }
    
    public List<De> getDes(Joueur j){
    	return /*super.liste("SELECT d FROM De d WHERE EXISTS "
    			+ "(SELECT jp FROM JoueurPartie jp WHERE jp.joueur = ?1"
    			+ " AND jp.partie = (SELECT p FROM Partie p WHERE p.id_partie = (SELECT MAX(p.id_partie))))", j)*/ null;
    }
}