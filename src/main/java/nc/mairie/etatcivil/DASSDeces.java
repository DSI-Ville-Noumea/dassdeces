/*
 * Created on 24 oct. 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nc.mairie.etatcivil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;



/**
 * @author boulu72
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DASSDeces {

	private File fileECC;
	private File fileECP;
	private File logFile;
	private String debutFilenameECC = "ECC_NOUMEA_";
	private String debutFilenameECP = "ECP_NOUMEA_";
	private static String serveurJDBC = "localhost";
	private static String hostname = "localhost";
	private String root=null;
	private String SMTPServer = null;
	
	private Properties properties = new Properties();

	public void init(String args []) {
		//On cherche le hostname
		try {
			hostname =  InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			hostname = "localhost";
		}

		//Si pas le serveur
		serveurJDBC = args.length == 1 ? args[0] : "LOCALHOST";
		
		//recup du root
		root = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
		if (root.toUpperCase().endsWith("JAR") || root.toUpperCase().endsWith("CLASS") ) {
			root=root.substring(0, root.lastIndexOf('/') +1);
		}
		
		//init du logFile
		logFile = new File(root+getNomClass()+".log");
		try{
			if (! logFile.exists()) logFile.createNewFile();
		} catch (Exception e) {
			System.out.println("Impossible d'initialiser le fichier de log : "+e.getMessage());
			System.exit(1);
		}
		
		//lecture des propriétés
		try {
			properties = new Properties();
			
			InputStream is = new FileInputStream(root+getNomClass()+".properties");
			properties.load(is);
			is.close();
			afficheMessage("Lecture des propriétés : "+properties);
	      
		} catch (Exception e) {
			afficheErreur("Impossible de lire le fichier properties : "+e.getMessage());
			System.exit(1);
		}
		
	}
	
	public void creerFile (File f, Connection con, String query) {

		
		
		PrintStream ps=null;
		//le fichier
		try {
			ps = new java.io.PrintStream(
				      new java.io.BufferedOutputStream(
				        new java.io.FileOutputStream(f)));
		} catch (Exception e) {
			afficheErreur("Impossible d'écrire dans "+f.getAbsolutePath());
		}
		
		try {
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(query);
			
			//on parcours pour ecrire
			while (rs.next()) {
				ps.println(rs.getString(1));
			}
				
			
		} catch (Exception e) {
			afficheErreur(e.getMessage());
		}
		
		
		ps.close();

	}
	
	public void creerFileECC(Connection con) {
		
		//Formatage du nom du fichier
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String nomfich=debutFilenameECC+sdf.format(new Date())+".txt";
		fileECC = new File(root+"Extraction/"+nomfich);

		creerFile(fileECC, con, "select * from mairie.DASSDecesECC");

		afficheMessage("Fin de reation du fichier "+fileECC.getName());
		
			}
	public void creerFileECP(Connection con) {
		
		//Formatage du nom du fichier
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		String nomfich=debutFilenameECP+sdf.format(new Date())+".txt";
		fileECP = new File(root+"Extraction/"+nomfich);

		creerFile(fileECP, con, "select * from mairie.DASSDecesECP");

		afficheMessage("Fin de creation du fichier "+fileECP.getName());
	}
	
	public void envoyerMail() {
		try {
			
			//Lecture de l'expediteur et du sujet
			String expediteur = properties.getProperty("expediteur");
			String sujet = properties.getProperty("sujet");
			
			SMTPServer = properties.getProperty("SMTPServer");

			Mail aMail = new Mail(SMTPServer, expediteur,  sujet);
			
			String dest=null;
			StringTokenizer st = null;

			//Ajout des destinataires TO
			dest = properties.getProperty("destinatairesTO");
			if (dest!=null) {
				st= new StringTokenizer(dest,",;");
				while (st.hasMoreElements()) {
					aMail.ajouteDestinataireTO(st.nextToken());
				}
			}

			//Ajout des destinataires CC
			dest = properties.getProperty("destinatairesCC");
			if (dest!=null) {
				st = new StringTokenizer(dest,",;");
				while (st.hasMoreElements()) {
					aMail.ajouteDestinataireCC(st.nextToken());
				}
			}
			
			//Ajout des destinataires BCC
			dest = properties.getProperty("destinatairesBCC");
			if (dest!=null) {
				st = new StringTokenizer(dest,",;");
				while (st.hasMoreElements()) {
					aMail.ajouteDestinataireBCC(st.nextToken());
				}
			}
			//aMail.ajouteDestinataireTO("boulu72@ville-noumea.nc");
			//aMail.ajouteDestinataireTO("f066457@gmail.com");
			
			//ajout du corps
			String msg  = properties.getProperty("message");
			if (dest == null) {
				dest="Transmission des données de mortalité de l'état civil du "+
					new SimpleDateFormat("dd/MM/yyyy").format(new Date());
			}
			aMail.ajouteMessage(msg);
			
			//ajout des fichiers
			aMail.ajouteFichier(fileECC.getCanonicalPath(),fileECC.getName());
			aMail.ajouteFichier(fileECP.getCanonicalPath(), fileECP.getName());
			
			aMail.envoyer();
			afficheMessage("Fin d'envoi du mail de "+expediteur);

		} catch (Exception e) {
			afficheErreur(e.getMessage());
		}
		
		
	}
	
	public void fermerConnection(Connection con){
		try {
			con.rollback();
		} catch (Exception e) {
			afficheMessage("Impossible de faire rollback sur la connexion");
		}
		try {
			con.close();
		} catch (Exception e) {
			afficheMessage("Impossible de fermer la connexion");
		}
			
	}
	
	public void run(String[] args) {

		//init des données
		init(args);

		//Recup d'une connexion
		Connection con = getUneConnexionJDBC(serveurJDBC);
		
		//Creation du fichier de droit commun
		creerFileECC(con);
		
		//Creation du fichier de droit particulier
		creerFileECP(con);
		
		//fermeture de la connexion
		fermerConnection(con);
		
		//Envoi du mail
		envoyerMail();
		
	}
	
	public static void main(String[] args) throws Exception {
		
		DASSDeces aDDASSDeces = new DASSDeces();
		
		//c'est parti
		aDDASSDeces.run(args);
		
	}
	
	
	public  java.sql.Connection getUneConnexionJDBC(String aServeur) {
		return getUneConnexionJDBC(aServeur, "***REMOVED***","***REMOVED***");
	}
	
	public java.sql.Connection getUneConnexionJDBC(String aServeur, String nom, String password) {
		if (hostname.toUpperCase().indexOf("NOUMEA") != -1 ) {
			return getUneConnexionJDBCNatif(aServeur, nom, password);
		} else
			return getUneConnexionJDBCToolBox(aServeur, nom, password);
	}
	
	private java.sql.Connection getUneConnexionJDBCNatif(String aServeur, String nom, String password) {
		afficheMessage("Connexion de "+nom+" avec le JDBC Natif");
	//	String drv="com.ibm.as400.access.AS400JDBCDriver";
		String drv="com.ibm.db2.jdbc.app.DB2Driver";
		
		java.sql.Connection con = null;
		try {
			
			Class.forName(drv);
			//con   = java.sql.DriverManager.getConnection("jdbc:as400://"+aServeur,nom,password);
			con   = java.sql.DriverManager.getConnection("jdbc:db2://"+aServeur,nom,password);
			con.setAutoCommit(true);
		} catch(Exception ex) {
			System.out.println("erreur driver : " + ex);
			return null;
		}
		return con;
	}
	private java.sql.Connection getUneConnexionJDBCToolBox(String aServeur, String nom, String password) {
		afficheMessage("Connexion de "+nom+" avec le JDBC ToolBox");
		String drv="com.ibm.as400.access.AS400JDBCDriver";
		java.sql.Connection con = null;
		try {
			Class.forName(drv);
			con   = java.sql.DriverManager.getConnection("jdbc:as400://"+aServeur,nom,password);
			con.setAutoCommit(false);
		} catch(Exception ex) {
			System.out.println("erreur driver : " + ex);
			return null;
		}
		return con;
	}
	
	/**
	 * Log d'un message
	 * @param message
	 */
	public void log(String message){
		try {
			FileWriter aWriter = new FileWriter(logFile.getCanonicalPath(),true);
		    aWriter.write(message + " " + System.getProperty("line.separator"));
			aWriter.flush();
		    aWriter.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Insérez la description de la méthode ici.
	 *  Date de création : (07/09/2006 13:29:54)
	 * @return java.util.Hashtable
	 */
	public void afficheErreur(String message) {
		String msg = new Date(System.currentTimeMillis())+" "+getNomClass()+" : "+message;
		System.err.println(msg);
		log(msg);
		System.exit(1);
	}

	/**
	 * Insérez la description de la méthode ici.
	 *  Date de création : (07/09/2006 13:29:54)
	 * @return java.util.Hashtable
	 */
	public void afficheMessage(String message) {
		String msg= new Date(System.currentTimeMillis())+" "+getNomClass()+" : "+message;
		System.err.println(msg);
		log(msg);
	}
	/*
	 * 
	 * @author boulu72
	 *
	 * Retourne le nom d la classe SANS le package
	 */
	public String getNomClass() {
		return getClass().getName().substring(getClass().getPackage().getName().length() +1);
	}
}
