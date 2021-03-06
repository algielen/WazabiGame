package ovh.gorillahack.wazabi.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ovh.gorillahack.wazabi.domaine.Carte;
import ovh.gorillahack.wazabi.domaine.De;
import ovh.gorillahack.wazabi.domaine.Face.Valeur;
import ovh.gorillahack.wazabi.domaine.Joueur;
import ovh.gorillahack.wazabi.domaine.JoueurPartie;
import ovh.gorillahack.wazabi.domaine.Partie;
import ovh.gorillahack.wazabi.domaine.Partie.Sens;
import ovh.gorillahack.wazabi.domaine.Partie.Status;
import ovh.gorillahack.wazabi.util.CryptService;

/**
 * Session Bean implementation class JoueurDaoImpl
 */
@Stateless
@Local(Dao.class)
@LocalBean
public class JoueurDaoImpl extends DaoImpl<Joueur> {
	private static final long serialVersionUID = -6188714066284889331L;
	@PersistenceContext(unitName = "wazabi")
	private EntityManager entityManager;

	@EJB
	private PartieDaoImpl partieDaoImpl;

	@EJB
	private JoueurPartieDaoImpl joueurPartieDaoImpl;

	@EJB
	private CarteDaoImpl carteDaoImpl;
	@EJB
	private DeDaoImpl deDaoImpl;

	public JoueurDaoImpl() {
		super(Joueur.class);
	}

	/**
	 * Le joueur deja existant se connecte en rentrant les bonnes informations.
	 * @param pseudo
	 * @param motdepasse
	 * @return Le compte du joueur.
	 */
	public Joueur connecter(String pseudo, String motdepasse) {
		Joueur joueur = super.recherche("SELECT j FROM Joueur j " + "WHERE j.pseudo = ?1 AND j.mot_de_passe = ?2",
				pseudo, CryptService.hash(motdepasse));
		return joueur;
	}

	/**
	 * Inscrire un nouveau joueur, le pseudo doit etre unique.
	 * @param pseudo
	 * @param motdepasse
	 * @return Le compte du joueur nouvellement cree.
	 */
	public Joueur inscrire(String pseudo, String motdepasse) {
		// On v�rifie que le pseudo n'est pas d�j� pris
		Joueur joueurExistant = super.recherche("SELECT j FROM Joueur j WHERE j.pseudo = ?1", pseudo);
		if (joueurExistant != null) {
			return null;
		} else {
			Joueur joueur = new Joueur(pseudo, CryptService.hash(motdepasse));
			enregistrer(joueur);
			return joueur;
		}
	}

	/**
	 * Le joueur lance ses des.
	 * @param j
	 * @return
	 */
	public List<De> lancerDes(Joueur j) {
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		List<De> des = jp.getDes();
		for (int i = 0; i < des.size(); i++) {
			De de = des.get(i);
			deDaoImpl.lancerDe(de);
		}
		return des;
	}

	public List<De> voirDes(Joueur j) {
		return deDaoImpl.getDes(j);
	}

	public List<Carte> voirCartes(Joueur j) {
		return carteDaoImpl.getCartes(j);
	}

	/**
	 * Le joueur courant termine son tour.
	 * @return
	 */
	public Partie terminerTour() {
		Partie p = partieDaoImpl.getPartieCourante();
		JoueurPartie courant = p.getCourant();
		p = partieDaoImpl.recharger(p.getId_partie());
		
		if (courant.getDes() == null) {
		} // si plus de d�s : fin de partie
		else if (courant.getDes().isEmpty()) {
			p.setStatut(Status.PAS_COMMENCE);
			terminerPartie();
			p.setVainqueur(courant.getJoueur());
			p = partieDaoImpl.mettreAJour(p);
		// sinon on passe au prochain joueur	
		} else {
			JoueurPartie suivant = null;
			if (p.getSens() == Sens.HORAIRE) {
				// suivant dans le sens horaire
				suivant = joueurPartieDaoImpl.getJoueurSuivant(courant, p);
				// on passe encore au suivant si le suivant devait passer des tours
				while (suivant.getCompteur_sauts() > 0) {
					suivant.setCompteur_sauts(suivant.getCompteur_sauts() - 1);
					joueurPartieDaoImpl.mettreAJour(suivant);
					suivant = joueurPartieDaoImpl.getJoueurSuivant(suivant, p);
				}
				p = partieDaoImpl.setCourant(suivant, p);
			} else if (p.getSens() == Sens.ANTIHORAIRE) {
				// pr�c�dent dans le sens antihoraire
				suivant = joueurPartieDaoImpl.getJoueurPrecedent(courant, p);
				while (suivant.getCompteur_sauts() > 0) {
					suivant.setCompteur_sauts(suivant.getCompteur_sauts() - 1);
					joueurPartieDaoImpl.mettreAJour(suivant);
					suivant = joueurPartieDaoImpl.getJoueurPrecedent(suivant, p);
				}
				p = partieDaoImpl.setCourant(suivant, p);
			}
		}
		return p;
	}

	/**
	 * Le joueur se deconnecte, s'il n'a a plus assez de joueurs actifs alors
	 * la partie se termine.
	 * @param j
	 * @param nombreJoueursMin
	 * @return
	 */
	public Partie deconnecter(Joueur j, int nombreJoueursMin) {
		Partie p = partieDaoImpl.getPartieCourante();
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		joueurPartieDaoImpl.enleverJoueur(jp);
		List<JoueurPartie> temp = partieDaoImpl.getPartieCourante().getJoueursParties();
		List<JoueurPartie> joueurActif = new ArrayList<JoueurPartie>();
		
		// on v�rifie le nombre de joueurs restants, si < MIN : on arr�te la partie
		for (JoueurPartie jop : temp) {
			if (jop.estActif())
				joueurActif.add(jop);
		}
		if (joueurActif.size() == 1) {
			p.setStatut(Status.ANNULEE);
			terminerPartie();
			p.setVainqueur(joueurActif.get(0).getJoueur());
			p = partieDaoImpl.mettreAJour(p);
		}
		return p;
	}

	public List<Joueur> listerJoueurPartieCourante() {
		return super.liste("SELECT j FROM Joueur j WHERE EXISTS " + "(SELECT jp FROM JoueurPartie jp WHERE "
				+ "jp.partie = (SELECT MAX(p.id_partie) FROM Partie p)" + "AND jp.joueur = j.id_joueur)");
	}

	/**
	 * Le joueur pioche une carte.
	 * @param j
	 * @return
	 */
	public Carte piocherCarte(Joueur j) {
		Partie p = partieDaoImpl.getPartieCourante();
		// on prend une carte de la pioche
		Carte c = p.piocher();
		c = carteDaoImpl.recharger(c.getId_carte()); // Utile?
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		List<Carte> cartes = jp.getCartes();
		if (cartes == null)
			cartes = new ArrayList<Carte>();
		// on ajoute la carte au joueur
		cartes.add(c);
		jp.setCartes(cartes);
		joueurPartieDaoImpl.mettreAJour(jp);
		partieDaoImpl.mettreAJour(p); // Utile?
		return c;
	}

	/**
	 * Le joueur remet la carte dans la pioche.
	 * @param j
	 * @param carte
	 * @return
	 */
	public Carte remettreCarte(Joueur j, Carte carte) {
		carte = carteDaoImpl.recharger(carte.getId_carte());
		Partie p = partieDaoImpl.getPartieCourante();
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		jp.supprimerCarte(carte);
		p.ajouterCarteALaPioche(carte);
		return carte;
	}

	/**
	 * Le joueur courant vole une carte au hasard d'un joueur qu'il a choisi.
	 * @param carte
	 * @param j
	 * @return
	 */
	public boolean piocherCarteChezUnJoueur(Carte carte, Joueur j) {
		// recuperation du joueur dans la classe joueurPartie

		JoueurPartie joueurReceveur = joueurPartieDaoImpl
				.getJoueurDeLaPartieCourante(joueurPartieDaoImpl.getJoueurCourant().getJoueur());
		JoueurPartie joueurCible = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		// carte du joueur
		List<Carte> listeCarteJoueur = joueurCible.getCartes();
		if (listeCarteJoueur.isEmpty()){
			
			piocherCarte(joueurCible.getJoueur());
			return false;
		
		}else {
			// sinon on prend une carte au hasard
			Random random = new Random();
			
			Carte c = carteDaoImpl
					.recharger(listeCarteJoueur.get(random.nextInt(listeCarteJoueur.size())).getId_carte());
			// on l'enleve de chez le joueur
			joueurCible = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(joueurCible.getJoueur());
			joueurCible.supprimerCarte(c);

			// on la place chez le joueur en parametre
			joueurReceveur.ajouterCarte(c);

			return true;
		}
	}

	/**
	 * Le joueur courant retire toutes les cartes sauf 2 au hasard chez tous ses adversaires.
	 * @return
	 */
	public boolean laisserToutLesAdversairesAvecDeuxCartes() {
		JoueurPartie joueurReceveur = joueurPartieDaoImpl.getJoueurCourant();

		Partie partieCourante = partieDaoImpl.getPartieCourante();
		List<JoueurPartie> listeJoueur = partieCourante.getJoueursParties();
		for (JoueurPartie joueurCible : listeJoueur) {
			List<Carte> listeCarteCible = joueurCible.getCartes();
			// si c'est le joueur courant ou s'il n'a deja que 2 cartes 
			if (listeCarteCible.size() <= 2 || joueurCible.equals(joueurReceveur)) {
				continue;
			}
			//on retire une carte au hasard jusqu'� n'en avoir que 2
			while (listeCarteCible.size() != 2) {
				Carte c = listeCarteCible.get((int) (Math.random() * (listeCarteCible.size() - 1)));
				remettreCarte(joueurCible.getJoueur(), c);
			}
		}
		return true;
	}

	/**
	 * Le joueur cible se voit rajouter un tour � passer (l'effet se cumule).
	 * @param c
	 * @param joueurCible
	 * @return
	 */
	public boolean passerTour(Carte c, Joueur joueurCible) {
		JoueurPartie joueurReceveur = joueurPartieDaoImpl.getJoueurCourant();

		JoueurPartie joueurPartieCible = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(joueurCible);
		joueurPartieCible.ajouterSaut();
		joueurPartieDaoImpl.enregistrer(joueurReceveur);
		joueurPartieDaoImpl.enregistrer(joueurPartieCible);
		partieDaoImpl.enregistrer(partieDaoImpl.getPartieCourante());
		return true;
	}

	/**
	 * Le joueur cible perd toutes ses cartes sauf 1 choisie au hasard.
	 * @param c
	 * @param j
	 * @return
	 */
	public boolean laisserAdversaireAvecUneCartes(Carte c, Joueur j) {
		JoueurPartie joueurReceveur = joueurPartieDaoImpl.getJoueurCourant();
		// utilisation de la carte
		remettreCarte(joueurReceveur.getJoueur(), c);
		JoueurPartie joueurCible = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		List<Carte> listeCarteCible = joueurCible.getCartes();
		if (listeCarteCible.size() < 2)
			return false;
		Random random = new Random();
		int index = random.nextInt(listeCarteCible.size());
		Carte carte = listeCarteCible.get(index);
		listeCarteCible.clear();
		listeCarteCible.add(carte);
		joueurCible.setCartes(listeCarteCible);
		return true;
	}

	public Joueur getJoueur(String pseudo) {
		return super.recherche("SELECT j FROM Joueur j WHERE j.pseudo=?1", pseudo);
	}

	/**
	 * Nettoie les des et les cartes apres que la partie soit terminee.
	 */
	private void terminerPartie() {
		for (JoueurPartie jp : partieDaoImpl.getPartieCourante().getJoueursParties()) {
			int size = jp.getCartes().size();
			for (int i = 0; i < size; i++) {
				Carte c = jp.getCartes().get(0);
				remettreCarte(jp.getJoueur(), c);
			}
			
			size = jp.getDes().size();
			for(int i = 0; i<size;i++){
				List<De> des = jp.getDes();
				for(int j = 0; j<des.size();j++){
					De d = des.get(j);
					d.setValeur(Valeur.WAZABI);
					deDaoImpl.mettreAJour(d);
				}
			}
		}
	}
}
