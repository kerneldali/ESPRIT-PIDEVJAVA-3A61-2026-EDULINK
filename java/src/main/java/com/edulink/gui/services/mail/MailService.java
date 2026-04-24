package com.edulink.gui.services.mail;

import com.edulink.gui.models.event.Event;
import com.edulink.gui.models.reservation.Reservation;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.activation.DataHandler;
import java.io.IOException;
import java.util.Properties;

public class MailService {

    // ⚠️ Remplace par ton Gmail et ton App Password
    private static final String FROM_EMAIL = "zariatyassine1@gmail.com";
    private static final String APP_PASSWORD = "kfke xkdl tlss ehdt";

    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    public static boolean sendReservationConfirmation(String toEmail, Event event, Reservation reservation) {
        try {
            Session session = createSession();

            // Générer le QR Code
            String qrContent = "edulink://event?id=" + event.getId() + "&reservation=" + reservation.getId();
            byte[] qrBytes = QRCodeService.generateQRCodeBytes(qrContent, 300, 300);

            // Créer le mail
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("✅ Confirmation de réservation — " + event.getTitle());

            // Créer le corps du mail (HTML + QR Code)
            MimeMultipart multipart = new MimeMultipart("related");

            // Partie HTML
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(buildTemplate(event, reservation), "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // Partie QR Code (image inline)
            MimeBodyPart qrPart = new MimeBodyPart();
            ByteArrayDataSource ds = new ByteArrayDataSource(qrBytes, "image/png");
            qrPart.setDataHandler(new DataHandler(ds));
            qrPart.setHeader("Content-ID", "<qrcode>");
            qrPart.setDisposition(MimeBodyPart.INLINE);
            multipart.addBodyPart(qrPart);

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("✅ Mail envoyé à : " + toEmail);
            return true;

        } catch (MessagingException e) {
            System.out.println("❌ Erreur envoi mail : " + e.getMessage());
            return false;
        }
    }

    private static String buildTemplate(Event event, Reservation reservation) {
        String type = event.isOnline() ? "🌐 En ligne" : "📍 " + event.getLocation();
        String meetInfo = event.isOnline() && event.getMeetLink() != null
                ? "<p style='text-align:center;'><a href='" + event.getMeetLink() + "' style='background:#4CAF50;color:white;padding:10px 20px;border-radius:5px;text-decoration:none;'>🎥 Rejoindre le Meet</a></p>"
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                    <div style="max-width:600px; margin:auto; background:white; border-radius:10px; overflow:hidden; box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                        
                        <!-- Header -->
                        <div style="background:#2C3E50; padding:30px; text-align:center;">
                            <h1 style="color:white; margin:0;">🎓 EduLink</h1>
                            <p style="color:#BDC3C7; margin:5px 0 0;">Plateforme étudiante ESPRIT</p>
                        </div>
                        
                        <!-- Body -->
                        <div style="padding:30px;">
                            <h2 style="color:#2C3E50;">✅ Réservation confirmée !</h2>
                            <p style="color:#555;">Votre réservation pour l'événement suivant a bien été enregistrée :</p>
                            
                            <!-- Event Card -->
                            <div style="background:#f8f9fa; border-left:4px solid #3498DB; padding:15px; border-radius:5px; margin:20px 0;">
                                <h3 style="color:#2C3E50; margin:0 0 10px;">📅 """ + event.getTitle() + """
                                </h3>
                                <p style="margin:5px 0; color:#555;">📆 <strong>Début :</strong> """ + event.getDateStart() + """
                                </p>
                                <p style="margin:5px 0; color:#555;">🏁 <strong>Fin :</strong> """ + event.getDateEnd() + """
                                </p>
                                <p style="margin:5px 0; color:#555;">📍 <strong>Type :</strong> """ + type + """
                                </p>
                            </div>
                            
                            """ + meetInfo + """
                            
                            <!-- QR Code -->
                            <div style="text-align:center; margin:30px 0;">
                                <p style="color:#555; font-size:14px;">Présentez ce QR Code à l'entrée de l'événement</p>
                                <img src="cid:qrcode" width="200" height="200" style="border:5px solid #2C3E50; border-radius:10px;"/>
                                <p style="color:#999; font-size:12px;">Réservation #""" + reservation.getId() + """
                                </p>
                            </div>
                        </div>
                        
                        <!-- Footer -->
                        <div style="background:#2C3E50; padding:15px; text-align:center;">
                            <p style="color:#BDC3C7; margin:0; font-size:12px;">© 2026 EduLink — ESPRIT</p>
                        </div>
                        
                    </div>
                </body>
                </html>
                """;
    }
}