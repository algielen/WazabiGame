package ovh.gorillahack.wazabi.chain;

import ovh.gorillahack.wazabi.domaine.Carte;
import ovh.gorillahack.wazabi.domaine.Joueur;
import ovh.gorillahack.wazabi.domaine.Partie.Sens;

public class GestionnaireCarteLaisserCarte extends GestionnaireCarte{
	public GestionnaireCarteLaisserCarte(GestionnaireCarte next){
		super(next);
	}
	
	@Override
	public boolean validerCarte(Carte c) {
		if(c.getCodeEffet()==6)
			return true;
		return false;
	}
	
	@Override
	public boolean utiliserCarte(Carte c) {
		return super.utiliserCarte(c);
	}
	
	@Override
	public boolean utiliserCarte(Carte c, Joueur j) {
		if(!validerCarte(c))
			return super.utiliserCarte(c, j);
		//TODO ne laisser qu'une carte � un adversaire
		return super.utiliserCarte(c, j);
	}
	
	@Override
	public boolean utiliserCarte(Carte c, Sens sens) {
		// TODO Auto-generated method stub
		return super.utiliserCarte(c, sens);
	}
}