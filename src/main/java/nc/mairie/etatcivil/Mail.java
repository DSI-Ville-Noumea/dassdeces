/*
 * Created on 24 oct. 2007
 *
 * TODO éTo change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package nc.mairie.etatcivil;

import java.util.Properties;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * @author boulu72
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Mail {

	public static void main(String [] args) {
		try {
			test(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public static String SMTPserveur = "smtp.site-mairie.noumea.nc";
		
	private MimeMessage message;
	private Multipart multipart = new MimeMultipart();
	
	/**
	 * @param server server 
	 * @param replyTo replyTo
	 * @param theSujet theSujet 
	 * @throws Exception Exception 
	 * 
	 */
	public Mail(String server, String user, String password, String from, String replyTo, String theSujet) throws Exception{
		super();
		if (server == null) server = SMTPserveur;
		Properties props = new Properties();
		
		//Alim du serveur host
		props.put("mail.smtp.host", server);
		//Authentification
		props.put("mail.smtp.auth", true);
		// fill props with any information
		Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, password);
			}
		});

		//Le mime message
		message = new MimeMessage(session);
		message.setContent(multipart);

		message.setFrom(new InternetAddress(from));
		message.setReplyTo(new InternetAddress[]{new InternetAddress(replyTo)});
		message.setSubject(theSujet);
	}
	
	public void ajouteMessage(String msg)throws Exception{
//		 Create the message part 
		BodyPart messageBodyPart = new MimeBodyPart();
//		 Fill the message
		messageBodyPart.setText(msg);
		multipart.addBodyPart(messageBodyPart);
	}

	public void ajouteFichier(String filename, String displayFilename)throws Exception{
//		 Part two is attachment
		BodyPart messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(filename);
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(displayFilename);
		multipart.addBodyPart(messageBodyPart);	
	}
	
	public void ajouteDestinataireTO(String to) throws Exception {
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
	}
	
	public void ajouteDestinataireCC(String to) throws Exception {
		message.addRecipient(Message.RecipientType.CC, new InternetAddress(to));
	}
	
	public void ajouteDestinataireBCC(String to) throws Exception {
		message.addRecipient(Message.RecipientType.BCC, new InternetAddress(to));
	}
	
	public void envoyer() throws Exception {
		Transport.send(message);
	}

	public static void test(String [] args) throws Exception {
		
		//Args : Dequi Sujet texte Aqui1 Aqui2,...
		
		if (args ==null || args.length != 4) {
			System.out.println ("Appeler avec : \n   Dequi \"Sujet\" Aqui1;Aqui2;Aqui3 \"Message\"");
			System.exit(0);
		}
						 
		//Mail aMail = new Mail(Mail.SMTPserveur, authUser, authPassword, from, "toto@toto.com",  "Mon sujet");
		Mail aMail = new Mail(Mail.SMTPserveur, args[0],  args[1],  args[2],  args[3], args[4]);
		
		//Ajout des destinataires
		//aMail.ajouteDestinataireTO("boulu72@ville-noumea.nc");
		//aMail.ajouteDestinataireTO("richard.deplanque@ville-noumea.nc");
		StringTokenizer st = new StringTokenizer(args[2],";");
		while (st.hasMoreTokens()) {
			aMail.ajouteDestinataireTO(st.nextToken());
		}
		
		//ajout du corps
		//aMail.ajouteMessage("Alors ? Trouveras-tu qui est toto@toto.com ? ;-)");
		aMail.ajouteMessage(args[3]);

		
		//ajout des fichiers
		//aMail.ajouteFichier("/Mortalite/toto.txt","toto.txt");
		//aMail.ajouteFichier("/Mortalite/ImageDistillerServlet.txt","popol.txt");
		
		aMail.envoyer();	
		
	}
	
}
