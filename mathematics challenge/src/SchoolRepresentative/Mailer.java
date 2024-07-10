package SchoolRepresentative;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class Mailer {
    public static void send(String to, String tomessage) {
        String from = "infomath668@gmail.com";
        String password = "wlas aryu akfs thzp"; // Your app-specific password

        String subject = "STATUS";
        String messageText = tomessage;

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(messageText);

            Transport.send(message);

            System.out.println("Message sent successfully");

        } catch (MessagingException e) {
            System.out.println("Message not sent");
            throw new RuntimeException(e);
        }
    }
}
