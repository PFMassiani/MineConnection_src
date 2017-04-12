package share.interaction;

import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import exception.*;
//import serveur.utilisateur.Etudiant;
import share.utilisateur.Etudiant;
import share.utilisateur.Utilisateur;
import share.communication.Action;
import share.communication.Communication;
import share.communication.ConnexionServeur;
import share.communication.TypeBackupable;

public class Evenement extends share.interaction.Interaction {

  //------------------------------------------------------------------------------------------------------------------------
  // ATTRIBUTS ---------------------------------------------------------------------------------------------------------
  // -----------------------------------------------------------------------------------------------------------------------
  
  private static final long serialVersionUID = -1977854013851011478L;
  
  private Date date;
  private int debutH, debutM, duree;
  
  private Set<Etudiant> principale,attente;
  
  //------------------------------------------------------------------------------------------------------------------------
  // CONSTRUCTEURS ---------------------------------------------------------------------------------------------------------
  // -----------------------------------------------------------------------------------------------------------------------
  
  public Evenement(int id,String nom, String description, int places, Date date, int debutH, int debutM, int duree, share.utilisateur.Utilisateur createur) {
    super(id,nom,description,places,createur);

    this.debutH = debutH + ((int) debutM / 60);
    this.debutM = debutM % 60;
    int nbJours = (int) (debutH / 24);
    debutH %= 24;
    this.date = date;
    this.date.setTime(this.date.getTime() + nbJours * 24 * 60 * 60 * 10000);
    this.duree = duree;
    if (IDENTIFIANT == -1) throw new InvalidIDException("");
    push();
  }
  
  //------------------------------------------------------------------------------------------------------------------------
  // ACCESSEURS / SETTERS ---------------------------------------------------------------------------------------------------------
  // -----------------------------------------------------------------------------------------------------------------------
  
  public Date getDebut(){
	pull();
    return date;
  }
  public int getDuree(){
	  pull();
	  return duree;
  }
  public String getDureeHM(){
	  pull();
    int h = (int) (duree / 60);
    int m = (int) duree - 60 * h;
    String s = "";
    if (h != 0) s += h + "h ";
    s += m + "min";
    return s;
  }
  public long getFin(){
	  pull();
    return date.getTime() + ( ( debutH * 60 ) + debutM ) * 60 * 1000 ;
  }
  //TODO
//  public static Set<Integer> ids(){
//	pull();
//    return ids;
//  }
  public Set<Etudiant> principale(){
	pull();
    return principale;
  }
  public Set<Etudiant> inscrits(){
	pull();
	Set<Etudiant> inscrits = new HashSet<>(principale);
	inscrits.addAll(attente);
    return inscrits;
  }
  public Set<Etudiant> attente(){
	  pull();
	  return attente;
  }
  
  @Override
  public String toString(){
//   return "Événement n°" + IDENTIFIANT + " : " + nom + 
//          ". Début : " + date + " à " + debutH + "h " + debutM + "min. " +
//          "Durée : " + getDureeHM() + ". Créé par " + createur; 
    return nom + " ( " + date + " : " + debutH + " ) ";
  }
  
  @Override
  public boolean equals(Object o){
	  pull();
    if (!(o instanceof Evenement)) return false;
    Evenement evt = (Evenement) o;
    return evt.getID() == IDENTIFIANT;
  }
  
  //------------------------------------------------------------------------------------------------------------------------
  // MÉTIER ---------------------------------------------------------------------------------------------------------
  // -----------------------------------------------------------------------------------------------------------------------
  
  // TODO Recherche par nom
  
  public boolean estCompatibleAvec(Evenement evt){
	  pull();
    Evenement premier, deuxieme;
    if (date.before(evt.date)){
      premier = this;
      deuxieme = evt;
    }
    else {
      premier = evt;
      deuxieme = this;
    }
    return premier.getFin() <= deuxieme.date.getTime();
  }
  
  public boolean estAvant(Evenement evt){
    return date.getTime() <= evt.date.getTime();
  }
  
  public boolean participe(Etudiant e){
	  pull();
    return principale().contains(e);
  }
  
  public boolean estInscrit(Etudiant e){
	  pull();
    return inscrits().contains(e);
  }
  
  public boolean setPlaces(int places){
	  pull();
    this.places = places;
    placesRestantes = places - principale.size();
    boolean reussi = push();
    if (placesRestantes < 0) placesRestantes = 0;
    return reussi;
  }
  
  public boolean ajouterPrincipale(Etudiant e){
	  pull();

	  boolean etaitSurAttente =  attente.remove(e);
	  principale.add(e);
	  placesRestantes--;
	  boolean reussi = push();
	  if (!reussi) {
		  principale.remove(e);
		  if(etaitSurAttente) attente.add(e);
		  placesRestantes ++;
	  }
	  return reussi;
  }
  
  public boolean ajouterAttente(Etudiant e){
	  pull();
	  boolean etaitSurPrincipale =  principale.remove(e);
	  attente.add(e);
	  if (etaitSurPrincipale) placesRestantes ++;
	  boolean reussi = push();
	  if (!reussi) {
		  attente.remove(e);
		  if(etaitSurPrincipale) principale.add(e);
		  placesRestantes --;
	  }
	  return reussi;
  }
  
  public boolean ajouter(Etudiant e){
	  pull();
	  boolean reussi = false;
    if (!(reussi = ajouterPrincipale(e))) reussi = ajouterAttente(e);
    return reussi;
  }
  
  public boolean supprimerInscrit(Etudiant e){
	  pull();
	  if(!principale.remove(e)) attente.remove(e);
	  return push();
  }
  
  public static Evenement getRandomEvent() {
	  // TODO
	  return null;
  }

  public void afficher() {
	  // TODO Affichage d'un événement
	  System.out.println("AFFICHAGE ÉVÉNEMENT");
  }
  
  @Override
  protected boolean push() {
	  try{
		  Communication com = new Communication(TypeBackupable.EVENEMENT, Action.SAUVEGARDER,this);
		  ConnexionServeur.getOOS().writeObject(com);
		  return ConnexionServeur.getIOS().readBoolean();
	  } catch (InvalidParameterException | IOException ex) {
		  System.out.println(ex.getMessage());
	  }
	  return false;
  }
  
  protected void pull() {
	  try{
		  Communication com = new Communication(TypeBackupable.EVENEMENT, Action.CHARGER,this);
		  ConnexionServeur.getOOS().writeObject(com);
		  Object o =  ConnexionServeur.getIOS().readObject();
		  if (!(o instanceof Evenement)) throw new InvalidCommunicationException("La communication n'a pas renvoyé un Evenement");
		  Evenement e = (Evenement) o;
		  if (IDENTIFIANT != e.getID()) throw new InvalidIDException("L'identifiant de l'événement chargé ne correspond pas à celui de l'événement courant");
		  
		  nom = e.nom;
		  description = e.description;
		  places = e.places;
		  placesRestantes = e.placesRestantes;
		  createur = e.createur;
		  date = e.date;
		  debutH = e.debutH;
		  debutM = e.debutM;
		  duree = e.duree;
		  principale = e.principale;
		  attente = e.attente();
		  
	  } catch (InvalidParameterException | IOException | InvalidCommunicationException | InvalidIDException | ClassNotFoundException ex) {
		  System.out.println(ex.getMessage());
	  }
  }

  public int getDebutH() {
	  pull();
	  return debutH;
  }
  public int getDebutM() {
	  pull();
	  return debutM;
  }
  public Date getDate() {
	  pull();
	  return date;
  }
  public static Evenement chercher(int id) {
	  try {
		  Communication com = new Communication(TypeBackupable.EVENEMENT, Action.CHARGER,id);
		  ConnexionServeur.getOOS().writeObject(com);
		  Object o =  ConnexionServeur.getIOS().readObject();
		  if (!(o instanceof Evenement)) throw new InvalidCommunicationException("La communication n'a pas renvoyé un Evenement");
		  return (Evenement) o;
	  } catch (InvalidCommunicationException | IOException | ClassNotFoundException | InvalidParameterException ex) {
		  System.out.println(ex.getMessage());
	  }
	  
	  return null;
  }

public String getNomClient() {
return nom;
}

public String getDescriptionClient() {
	return description;
}

public int getPlacesClient() {
	return places;
}
public int getPlacesRestantesClient() {
	return placesRestantes;
}

public Date getDateClient() {
	return date;
}

public int getDureeClient() {
	return duree;
}
public int getDebutHClient() {
	return debutH;
}
public int getDebutMClient() {
	return debutM;
}
public Utilisateur getCreateurClient() {
	return createur;
}
public Set<Etudiant> inscritsClient(){
	Set<Etudiant> inscrits = new HashSet<>(principale);
	inscrits.addAll(attente);
    return inscrits;
}
public Set<Etudiant> principaleClient(){
	return principale;
}
public static Evenement nouveau(String nom, String description, int places, Date date, int debutH, int debutM, int duree, share.utilisateur.Utilisateur createur) {
	Evenement evt = null;
	
	try {
		Communication com = new Communication(TypeBackupable.EVENEMENT, Action.NOUVEAU);
		ConnexionServeur.getOOS().writeObject(com);
		Object o = ConnexionServeur.getIOS().readObject();
		if (!(o instanceof Evenement)) throw new InvalidCommunicationException("La communication n'a pas renvoyé un Evenement");
		evt = (Evenement) o;
	} catch (InvalidCommunicationException | IOException | ClassNotFoundException | InvalidParameterException ex) {
		  System.out.println(ex.getMessage());
	}
	return new Evenement(evt.getID(),nom,description,places,date,debutH,debutM,duree,createur);
}
}