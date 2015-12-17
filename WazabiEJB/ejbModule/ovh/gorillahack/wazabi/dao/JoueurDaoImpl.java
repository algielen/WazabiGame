package ovh.gorillahack.wazabi.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ovh.gorillahack.wazabi.domaine.Carte;
import ovh.gorillahack.wazabi.domaine.De;
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

	public Joueur connecter(String pseudo, String motdepasse) {
		Joueur joueur = super.recherche("SELECT j FROM Joueur j " + "WHERE j.pseudo = ?1 AND j.mot_de_passe = ?2",
				pseudo, CryptService.hash(motdepasse));
		return joueur;
	}

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

	public Partie terminerTour() {
		Partie p = partieDaoImpl.getPartieCourante();
		JoueurPartie courant = p.getCourant();
		p = partieDaoImpl.recharger(p.getId_partie());
		if (courant.getDes() == null) {
		} else if (courant.getDes().isEmpty()) {
			p.setStatut(Status.PAS_COMMENCE);
			p.setVainqueur(courant.getJoueur());
			p = partieDaoImpl.mettreAJour(p);
		} else {
			JoueurPartie suivant = null;
			if (p.getSens() == Sens.HORAIRE) {
				suivant = joueurPartieDaoImpl.getJoueurSuivant(courant, p);
				while (suivant.getCompteur_sauts() > 0) {
					suivant.setCompteur_sauts(suivant.getCompteur_sauts() - 1);
					joueurPartieDaoImpl.mettreAJour(suivant);
					suivant = joueurPartieDaoImpl.getJoueurSuivant(suivant, p);
				}
				p = partieDaoImpl.setCourant(suivant, p);
			} else if (p.getSens() == Sens.ANTIHORAIRE) {
				suivant = joueurPartieDaoImpl.getJoueurPrecedent(courant, p);
				while (suivant.getCompteur_sauts() > 0) {
					suivant.setCompteur_sauts(suivant.getCompteur_sauts() - 1);
					joueurPartieDaoImpl.mettreAJour(suivant);
					suivant = joueurPartieDaoImpl.getJoueurPrecedent(suivant, p);
				}
				p = partieDaoImpl.setCourant(suivant, p);
			}
			System.out.println(p.getCourant());
		}
		return p;
	}

	public Partie deconnecter(Joueur j, int nombreJoueursMin) {
		Partie p = partieDaoImpl.getPartieCourante();
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		System.out.println("jp ---->" + jp);
		joueurPartieDaoImpl.enleverJoueur(jp);
		List<JoueurPartie> temp = partieDaoImpl.getPartieCourante().getJoueursParties();
		List<JoueurPartie> joueurActif = new ArrayList<JoueurPartie>();
		for (JoueurPartie jop : temp) {
			if (jop.estActif())
				joueurActif.add(jop);
		}
		if (joueurActif.size() == 1) {
			p.setStatut(Status.ANNULEE);
			p.setVainqueur(joueurActif.get(0).getJoueur());
			p = partieDaoImpl.mettreAJour(p);
		}
		return p;
	}

	public List<Joueur> listerJoueurPartieCourante() {
		return super.liste("SELECT j FROM Joueur j WHERE EXISTS " + "(SELECT jp FROM JoueurPartie jp WHERE "
				+ "jp.partie = (SELECT MAX(p.id_partie) FROM Partie p)" + "AND jp.joueur = j.id_joueur)");
	}

	public Carte piocherCarte(Joueur j) {
		Partie p = partieDaoImpl.getPartieCourante();
		Carte c = p.piocher();
		c = carteDaoImpl.recharger(c.getId_carte());
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		jp = joueurPartieDaoImpl.recharger(jp.getId_joueur_partie());
		System.out.print("NAME:" + p.getNom());
		System.out.print("Id_Carte:" + c.getId_carte());
		System.out.println("Id_jp:" + jp.getId_joueur_partie());
		jp.ajouterCarte(c);	
		jp = joueurPartieDaoImpl.mettreAJour(jp);
		//partieDaoImpl.mettreAJour(p);
		return c;
	}

	public Carte remettreCarte(Joueur j, Carte carte) {
		carte = carteDaoImpl.recharger(carte.getId_carte());
		Partie p = partieDaoImpl.getPartieCourante();
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(j);
		jp.supprimerCarte(carte);
		p.ajouterCarteALaPioche(carte);
		// jp = joueurPartieDaoImpl.mettreAJour(jp);
		// p = partieDaoImpl.mettreAJour(p);
		// carte = carteDaoImpl.mettreAJour(carte);
		return carte;
	}

	public Carte piocherCarteChezUnJoueur(Carte carte) {
		// TODO Auto-generated method stub

		// recuperation du joueur dans la classe joueurPartie

		JoueurPartie joueurReceveur = joueurPartieDaoImpl.getJoueurCourant();
		// utilisation de la carte du joueur receveur
		remettreCarte(joueurReceveur.getJoueur(), carte);
		Carte renv;
		Partie partieCourante = partieDaoImpl.getPartieCourante();
		List<JoueurPartie> listeJoueur = partieCourante.getJoueursParties();
		Collections.shuffle(listeJoueur);
		for (JoueurPartie joueurCible : listeJoueur) {
			// carte du joueur
			List<Carte> listeCarteJoueur = joueurCible.getCartes();
			if (listeCarteJoueur.isEmpty())
				continue;
			else {
				// sinon on prend une carte au hasard
				Carte c = listeCarteJoueur.get((int) (Math.random() * (listeCarteJoueur.size() - 1)));
				// on l'enleve de chez le joueur
				joueurCible.getCartes().remove(c);
				// on la place chez le joueur en parametre
				joueurReceveur.ajouterCarte(c);
				// on enregistre
				joueurPartieDaoImpl.enregistrer(joueurReceveur);
				joueurPartieDaoImpl.enregistrer(joueurCible);
				partieDaoImpl.enregistrer(partieCourante);
				return carte;
			}

		}
		// aucun joueur n'a de carte
		Carte c = piocherCarte(joueurReceveur.getJoueur());
		return c;
	}

	public boolean laisserAdversaireAvecDeuxCartes(Carte carte) {
		// TODO Piocher chez les autres joueurs tant que un n'a pas plus de deux
		// cartes
		JoueurPartie joueurReceveur = joueurPartieDaoImpl.getJoueurCourant();
		// utilisation de la carte du joueur receveur
		remettreCarte(joueurReceveur.getJoueur(), carte);
		Partie partieCourante = partieDaoImpl.getPartieCourante();
		List<JoueurPartie> listeJoueur = partieCourante.getJoueursParties();
		JoueurPartie joueurCible = listeJoueur.get((int) (Math.random() * (listeJoueur.size() - 1)));
		while (joueurCible.equals(joueurReceveur)) {
			joueurCible = listeJoueur.get((int) (Math.random() * (listeJoueur.size() - 1)));
		}
		List<Carte> listeCarteCible = joueurCible.getCartes();
		if (listeCarteCible.size() <= 2) {
			return false;
		}
		while (listeCarteCible.size() != 2) {
			Carte c = listeCarteCible.get((int) (Math.random() * (listeCarteCible.size() - 1)));
			remettreCarte(joueurCible.getJoueur(), c);
		}
		return true;

	}

	public boolean laisserToutLesAdversairesAvecDeuxCartes() {
		JoueurPartie joueurReceveur = joueurPartieDaoImpl.getJoueurCourant();

		Partie partieCourante = partieDaoImpl.getPartieCourante();
		List<JoueurPartie> listeJoueur = partieCourante.getJoueursParties();
		for (JoueurPartie joueurCible : listeJoueur) {
			List<Carte> listeCarteCible = joueurCible.getCartes();
			if (listeCarteCible.size() <= 2 || joueurCible.equals(joueurReceveur)) {
				continue;
			}

			while (listeCarteCible.size() != 2) {
				Carte c = listeCarteCible.get((int) (Math.random() * (listeCarteCible.size() - 1)));
				remettreCarte(joueurCible.getJoueur(), c);
			}
		}
		return true;
	}

	public boolean passerTour(Carte c, Joueur joueurCible) {
		// TODO Auto-generated method stub
		JoueurPartie joueurReceveur = joueurPartieDaoImpl.getJoueurCourant();
		// utilisation de la carte
		remettreCarte(joueurReceveur.getJoueur(), c);

		JoueurPartie joueurPartieCible = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(joueurCible);
		joueurPartieCible.setCompteur_sauts(joueurPartieCible.getCompteur_sauts() + 1);
		joueurPartieDaoImpl.enregistrer(joueurReceveur);
		joueurPartieDaoImpl.enregistrer(joueurPartieCible);
		partieDaoImpl.enregistrer(partieDaoImpl.getPartieCourante());
		return true;
	}
}
