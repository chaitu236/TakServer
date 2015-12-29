/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author chaitu
 */
public class EMail {
    public static void send(String to, String sub, String msg) {
        Properties prop = System.getProperties();
        prop.setProperty("mail.smtp.host", "localhost");
        Session session = Session.getInstance(prop);
        
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress("no-reply@playtak.com"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(sub);
            message.setText(msg);
            
            Transport.send(message);
            System.out.println("Email sent to "+to);
        } catch (MessagingException ex) {
            Logger.getLogger(EMail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("Sending mail");
        EMail.send(args[0], "test", "testing java mail");
        System.out.println("Sent mail");
    }
}
