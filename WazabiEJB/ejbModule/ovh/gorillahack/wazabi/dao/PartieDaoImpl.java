package ovh.gorillahack.wazabi.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ovh.gorillahack.wazabi.domaine.Joueur;
import ovh.gorillahack.wazabi.domaine.JoueurPartie;
import ovh.gorillahack.wazabi.domaine.Partie;
import ovh.gorillahack.wazabi.domaine.Partie.Sens;
import ovh.gorillahack.wazabi.domaine.Partie.Status;

@SuppressWarnings("serial")
@Stateless
@Local(Dao.class)
@LocalBean
public class PartieDaoImpl extends DaoImpl<Partie>{
	protected static int ordre=0;
	@EJB
	private JoueurPartieDaoImpl joueurPartieDao;
	@EJB
	private CarteDaoImpl carteDaoImpl;
	@EJB
	private DeDaoImpl deDaoImpl;
	
	public PartieDaoImpl() {
		super(Partie.class);
	}

	@PersistenceContext(unitName = "wazabi")
	private EntityManager entityManager;

	public Partie rejoindrePartie(Joueur j){
		JoueurPartie jp = new JoueurPartie(ordre++, 0, deDaoImpl.getDes(j), carteDaoImpl.getCartes(j));
		Partie p = getPartieCourante();
		//Si pas de partie ou si la partie poss�de le status PAS_COMMENCEE
		if(p==null||p.getStatut()==Partie.Status.PAS_COMMENCE){
			p = super.enregistrer(new Partie("TOTO", new Date(), Sens.HORAIRE, j, null, null, Status.EN_ATTENTE));
		}
		jp.setPartie(p);
		jp.setJoueur(j);
		joueurPartieDao.enregistrer(jp);
		return super.enregistrer(p);
	}	

	public List<Partie> afficherHistorique(Joueur j){
		return super.liste("SELECT p FROM Partie p WHERE EXISTS("
				+ "SELECT jp FROM JoueurPartie jp WHERE jp.joueur = ?1 AND jp.partie = p.id_partie)", j);
	}
	
	public Partie getPartieCourante(){
		return super.recherche("SELECT p FROM Partie p WHERE p.id_partie = (SELECT MAX(p.id_partie) FROM Partie p)");
	}
}