package server;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    private static final String USERNAME = "YOUR_EMAIL@gmail.com";
    private static final String PASSWORD = "YOUR_GMAIL_APP_PASSWORD";

    public static void sendRecoveryEmail(String recipientEmail, String code) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
            new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, PASSWORD);
                }
            });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(USERNAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("🔑 BPark Code Recovery");
        message.setText("Hello,\n\nYour parking code is: " + code + "\n\nGood luck!\nBPark Team");

        Transport.send(message);
        System.out.println("✅ Email sent to " + recipientEmail);
    }

    public static void sendLateWarning(String recipientEmail, String warningText) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
            new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, PASSWORD);
                }
            });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(USERNAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("🚨 Late Pickup Warning - BPark");
        message.setText("Dear subscriber,\n\n" + warningText + "\n\nBPark System");

        Transport.send(message);
        System.out.println("📩 Warning email sent to: " + recipientEmail);
    }


}
