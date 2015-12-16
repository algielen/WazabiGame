package ovh.gorillahack.wazabi.usecases;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import ovh.gorillahack.wazabi.dao.JoueurDaoImpl;
import ovh.gorillahack.wazabi.dao.JoueurPartieDaoImpl;
import ovh.gorillahack.wazabi.dao.PartieDaoImpl;
import ovh.gorillahack.wazabi.dao.XmlParserImpl;
import ovh.gorillahack.wazabi.domaine.Carte;
import ovh.gorillahack.wazabi.domaine.De;
import ovh.gorillahack.wazabi.domaine.Joueur;
import ovh.gorillahack.wazabi.domaine.JoueurPartie;
import ovh.gorillahack.wazabi.domaine.Partie;
import ovh.gorillahack.wazabi.domaine.Partie.Sens;
import ovh.gorillahack.wazabi.exception.CardNotFoundException;
import ovh.gorillahack.wazabi.exception.NoCurrentGameException;
import ovh.gorillahack.wazabi.exception.NotEnoughDiceException;
import ovh.gorillahack.wazabi.exception.PlayerNotFoundException;
import ovh.gorillahack.wazabi.exception.QueryException;
import ovh.gorillahack.wazabi.exception.ValidationException;
import ovh.gorillahack.wazabi.exception.XmlParsingException;
import ovh.gorillahack.wazabi.util.Utils;

/**
 * Session Bean implementation class GestionPartieImpl
 */
@Singleton
@Startup
@Remote(GestionPartie.class)
public class GestionPartieImpl implements GestionPartie {
	private Partie partieCourante;
	private int min_joueurs;
	private int max_joueurs;
	private int nbCartesParJoueurs;
	private int nbCartesTotal;
	private int nbDesParJoueur;
	private int nbDesTotal;
	private List<Carte> jeuDeCarte;
	private static int ordre_pioche = 0;

	@EJB
	private JoueurDaoImpl joueurDaoImpl;

	@EJB
	private JoueurPartieDaoImpl joueurPartieDaoImpl;

	@EJB
	private PartieDaoImpl partieDaoImpl;

	@EJB
	private XmlParserImpl xmlParserImpl;

	@PostConstruct
	public void postconstruct() {
		System.out.println("GestionPartieImpl created");
	}

	@PreDestroy
	public void predestroy() {
		System.out.println("GestionPartieImpl destroyed");
	}

	/**
	 * Default constructor.
	 */
	public GestionPartieImpl() {
		// TODO Lors de la selection du joueur courant, il faut prendre en
		// compte le champ "compteur_saut".
	}

	@Override
	public Joueur inscrire(String pseudo, String motdepasse, String motdepasseRepeat) throws ValidationException {
		if (!Utils.checkString(pseudo) || !Pattern.matches("([a-z]|[0-9]){1,20}", pseudo)) {
			throw new ValidationException("Format du pseudo invalide .");
		}

		if (!Utils.checkString(motdepasse) || !Pattern.matches("([a-z]|[0-9]){1,20}", motdepasse)) {
			throw new ValidationException("Format du mot de passe invalide.");
		}

		if (!motdepasse.equals(motdepasseRepeat) || !Pattern.matches("([a-z]|[0-9]){1,20}", motdepasseRepeat)) {
			throw new ValidationException("Les deux mots de passe ne sont pas similaires.");
		}

		return joueurDaoImpl.inscrire(pseudo, motdepasse);
	}

	@Override
	public List<Partie> afficherHistorique(Joueur j) throws PlayerNotFoundException {
		return partieDaoImpl.afficherHistorique(j);
	}

	@Override
	public Partie rejoindrePartie(Joueur j) throws PlayerNotFoundException, NoCurrentGameException {
		partieCourante = partieDaoImpl.rejoindrePartie(j);
		return partieCourante;
	}

	@Override
	public List<Joueur> listerJoueurPartieCourante() throws NoCurrentGameException {
		return partieDaoImpl.listerJoueurPartieCourante();
	}

	@Override
	public List<Joueur> getAdversaires(Joueur j) throws PlayerNotFoundException, NoCurrentGameException {
		List<Joueur> adversaires = listerJoueurPartieCourante();
		adversaires.remove(j);
		return adversaires;
	}

	@Override
	public void commencerPartie() throws NoCurrentGameException {
		partieCourante = partieDaoImpl.commencerPartie(nbCartesParJoueurs, nbDesParJoueur);
	}

	@Override
	public List<De> lancerDes(Joueur j) throws PlayerNotFoundException {
		return joueurDaoImpl.lancerDes(j);
	}

	@Override
	public List<De> voirDes(Joueur j) throws PlayerNotFoundException {
		return joueurDaoImpl.voirDes(j);
	}

	@Override
	public boolean piocherUneCarte(Joueur j) throws PlayerNotFoundException {
		return joueurDaoImpl.piocherCarte(j);
	}

	@Override
	public void terminerTour() throws NoCurrentGameException {
		partieCourante = joueurDaoImpl.terminerTour();
	}

	@Override
	public Joueur seConnecter(String pseudo, String mdp) throws ValidationException {
		if (!Utils.checkString(pseudo) || !Pattern.matches("([a-z]|[0-9]){1,20}", pseudo)) {
			throw new ValidationException("Format du pseudo incorrecte.");
		}

		if (!Utils.checkString(mdp) || !Pattern.matches("([a-z]|[0-9]){1,20}", mdp)) {
			throw new ValidationException("Format du mot de passe incorrecte.");
		}
		return joueurDaoImpl.connecter(pseudo, mdp);
	}

	@Override
	public Partie creerPartie(String nom) throws ValidationException, XmlParsingException {
		if (!Utils.checkString(nom) || !Pattern.matches("[A-Za-z0-9]{1,20}", nom))
			throw new ValidationException("Format de la partie invalide.");
		xmlParserImpl.chargerXML();
		try {
			partieCourante = partieDaoImpl.creerUnePartie(nom);
		} catch (NoCurrentGameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return partieCourante;
	}

	@Override
	public void deconnecter(Joueur j) throws PlayerNotFoundException {
		joueurDaoImpl.deconnecter(j, min_joueurs);
	}

	public Joueur getJoueurCourant() {
		return partieCourante.getCourant().getJoueur();
	}

	public int getMin_joueurs() {
		return min_joueurs;
	}

	public void setMin_joueurs(int min_joueurs) {
		this.min_joueurs = min_joueurs;
	}

	public int getMax_joueurs() {
		return max_joueurs;
	}

	public void setMax_joueurs(int max_joueurs) {
		this.max_joueurs = max_joueurs;
	}

	public int getNbCartesParJoueurs() {
		return nbCartesParJoueurs;
	}

	public void setNbCartesParJoueurs(int nbCartesParJoueurs) {
		this.nbCartesParJoueurs = nbCartesParJoueurs;
	}

	public int getNbCartesTotal() {
		return nbCartesTotal;
	}

	public void setNbCartesTotal(int nbCartesTotal) {
		this.nbCartesTotal = nbCartesTotal;
	}

	public int getNbDesParJoueur() {
		return nbDesParJoueur;
	}

	public void setNbDesParJoueur(int nbDesParJoueur) {
		this.nbDesParJoueur = nbDesParJoueur;
	}

	public int getNbDesTotal() {
		return nbDesTotal;
	}

	public void setNbDesTotal(int nbDesTotal) {
		this.nbDesTotal = nbDesTotal;
	}

	@Override
	public Partie getPartieCourante() throws NoCurrentGameException {
		return partieCourante;
	}

	@Override
	public List<Carte> getJeuDeCarte() throws NoCurrentGameException {
		return jeuDeCarte;
	}

	@Override
	public void setJeuDeCarte(List<Carte> liste) {
		this.jeuDeCarte = liste;

	}

	@Override
	public void donnerDes(Joueur j, int[] id_adversaires) throws NotEnoughDiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void utiliserCarte(int id_carte) throws CardNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public void utiliserCarte(int id_carte, Joueur j) throws CardNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public void utiliserCarte(int id_carte, Sens sens) throws CardNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Carte> voirCartes(Joueur j) throws PlayerNotFoundException {
		return joueurDaoImpl.voirCartes(j);
	}

	@Override
	public int getNombreDeToursAPasser(Joueur j) throws PlayerNotFoundException {
		return 0;
	}

	@Override
	public boolean retournerCarteDansLaPioche(Joueur joueur, Carte carte, Partie partie) {
		JoueurPartie jp = joueurPartieDaoImpl.getJoueurDeLaPartieCourante(joueur);
		boolean succes = jp.supprimerCarte(carte);
		succes = succes && ajouterCarteALaPioche(carte, partie);
		return succes;
	}

	@Override
	public boolean ajouterCarteALaPioche(Carte carte, Partie partie) {
		//try {
			//ordre_pioche = partieDaoImpl.getMaxOrdrePioche(partieCourante) + 1;
			carte.setOrdre_pioche(ordre_pioche++);
			partie.ajouterCarteALaPioche(carte);
			return true;
		/*} catch (QueryException e) {
			e.printStackTrace();
			return false;
		}*/

	}
}
