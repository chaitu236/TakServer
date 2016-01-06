/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import com.sun.mail.smtp.SMTPTransport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author chaitu
 */
public class EMail {
    public static String host;
    public static String user;
    public static String password;
    
    public static void send(String to, String sub, String msg) {
        Properties prop = System.getProperties();
        prop.put("mail.smtps.host", host);
        prop.put("mail.smtps.auth", "true");
        Session session = Session.getInstance(prop);
        
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(user));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(sub);
            message.setText(msg);
            
            //Transport.send(message);
            SMTPTransport t = (SMTPTransport)session.getTransport("smtps");
            t.connect(host, user, password);
            t.sendMessage(message, message.getAllRecipients());
            t.close();
            
            System.out.println("Email sent to "+to);
        } catch (MessagingException ex) {
            Logger.getLogger(EMail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        Settings.parse();
        System.out.println("Sending mail");
        System.out.print("To:");
        
        try {
            EMail.send(new BufferedReader(new InputStreamReader(System.in)).readLine(), "test", "testing java mail");
            System.out.println("Sent mail");
        } catch (IOException ex) {
            Logger.getLogger(EMail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
