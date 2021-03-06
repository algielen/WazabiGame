package ovh.gorillahack.wazabi.chain;

import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import ovh.gorillahack.wazabi.domaine.Carte;
import ovh.gorillahack.wazabi.domaine.Joueur;
import ovh.gorillahack.wazabi.domaine.Partie.Sens;
import ovh.gorillahack.wazabi.exception.CardConstraintViolatedException;
import ovh.gorillahack.wazabi.usecases.GestionPartie;

public abstract class GestionnaireCarte {
	private GestionnaireCarte next;
	private List<Integer> effetJoueur = new ArrayList<Integer>();
	private List<Integer> effetSens = new ArrayList<Integer>();

	protected static GestionPartie gp;

	static {
		try {
			gp = (GestionPartie) new InitialContext()
					.lookup("ejb:Wazabi/WazabiEJB/GestionPartieImpl!ovh.gorillahack.wazabi.usecases.GestionPartie");
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	public GestionnaireCarte(GestionnaireCarte next) {
		this.next = next;
		effetJoueur.add(4);
		effetJoueur.add(5);
		effetJoueur.add(6);
		effetJoueur.add(9);

		effetSens.add(2);
	}

	public abstract boolean validerCarte(Carte c);

	public boolean utiliserCarte(Carte c) throws CardConstraintViolatedException {
		int ce = c.getCodeEffet();
		if (effetJoueur.contains(ce) || effetSens.contains(ce)) {
			return false;
		}
		gp.remettreCarte(gp.getJoueurCourant(), c);
		if (this.next != null)
			return next.utiliserCarte(c);
		return false;
	}

	public boolean utiliserCarte(Carte c, Sens sens) throws CardConstraintViolatedException {
		if (!effetSens.contains(c.getCodeEffet()))
			return false;
		gp.remettreCarte(gp.getJoueurCourant(), c);
		if (next != null)
			return next.utiliserCarte(c, sens);
		return false;
	}

	public boolean utiliserCarte(Carte c, Joueur j) throws CardConstraintViolatedException {
		if (!effetJoueur.contains(c.getCodeEffet()))
			return false;
		gp.remettreCarte(gp.getJoueurCourant(), c);
		if (next != null)
			return next.utiliserCarte(c, j);
		return false;
	}
}
