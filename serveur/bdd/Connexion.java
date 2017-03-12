package serveur.bdd;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

public class Connexion {
  //On utilise ici le pattern Singleton pour s'assurer de n'avoir qu'une seule connexion à la BDD
  
  private static String url = "jdbc:mysql://localhost/mine_connection?useSSL=false";
  private static String user = "pierre-francois";
  private static String password;
  
  private static Connection connexion;
  
  
  private Connexion(){
	
	try {
		FileReader psswd = new FileReader("password.txt");
		Scanner sc = new Scanner(psswd);
		password = sc.nextLine();
		sc.close();
		psswd.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	
    System.out.println("Connexion à la base de données...");

    try{      
      connexion = DriverManager.getConnection(url,user,password);
      System.out.println("Connexion établie !");

    } catch (SQLException e){
      System.out.println("SQLException: " + e.getMessage());
      System.out.println("SQLState: " + e.getSQLState());
      System.out.println("VendorError: " + e.getErrorCode());
      System.err.println("Échec de la connexion !");

    }
  }
  
  public static synchronized Connection getConnection(){
    if (connexion == null) new Connexion();
    else System.out.println("Connexion récupérée !");
    return connexion;
  }
}
