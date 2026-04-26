package com.edulink.gui.services.mail;

import com.edulink.gui.models.event.Event;
import com.edulink.gui.models.reservation.Reservation;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.activation.DataHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailService {

    private static final String FROM_EMAIL   = "zariatyassine1@gmail.com";
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

    // ─────────────────────────────────────────────────────
    // Event Reservation Confirmation
    // ─────────────────────────────────────────────────────
    public static boolean sendReservationConfirmation(String toEmail, Event event, Reservation reservation) {
        try {
            Session session = createSession();
            String qrContent = "edulink://event?id=" + event.getId() + "&reservation=" + reservation.getId();
            byte[] qrBytes = QRCodeService.generateQRCodeBytes(qrContent, 300, 300);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("✅ Confirmation de réservation — " + event.getTitle());

            MimeMultipart multipart = new MimeMultipart("related");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(buildReservationTemplate(event, reservation), "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

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

    // ─────────────────────────────────────────────────────
    // OTP — Password Reset
    // ─────────────────────────────────────────────────────
    public static boolean sendOtpEmail(String toEmail, String otp) {
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("🔐 EduLink — Password Reset Code");
            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;'>"
                + "<div style='max-width:480px;margin:auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,.1);'>"
                + "<div style='background:#1e293b;padding:24px;text-align:center;'><h1 style='color:#fff;margin:0;'>🎓 EduLink</h1></div>"
                + "<div style='padding:30px;text-align:center;'>"
                + "<h2 style='color:#1e293b;'>Password Reset Request</h2>"
                + "<p style='color:#555;'>Use the code below to reset your password. It expires in <strong>10 minutes</strong>.</p>"
                + "<div style='margin:24px auto;display:inline-block;background:#f1f5f9;border:2px dashed #6366f1;border-radius:12px;padding:20px 40px;'>"
                + "<span style='font-size:36px;font-weight:bold;letter-spacing:8px;color:#6366f1;'>" + otp + "</span></div>"
                + "<p style='color:#888;font-size:13px;'>If you did not request this, please ignore this email.</p>"
                + "</div>"
                + "<div style='background:#1e293b;padding:12px;text-align:center;'><p style='color:#94a3b8;margin:0;font-size:12px;'>© 2026 EduLink — ESPRIT</p></div>"
                + "</div></body></html>";
            message.setContent(html, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ OTP mail sent to: " + toEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("❌ OTP mail error: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────
    // Session Summary
    // ─────────────────────────────────────────────────────
    public static boolean sendSessionSummaryEmail(
            com.edulink.gui.models.assistance.HelpSession session, String summary) {
        try {
            Session mailSession = createSession();
            for (String toEmail : resolveEmails(session.getTutorId(), session.getStudentId())) {
                if (toEmail == null || toEmail.isBlank()) continue;
                Message message = new MimeMessage(mailSession);
                message.setFrom(new InternetAddress(FROM_EMAIL));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                message.setSubject("📚 EduLink — Tutoring Session Summary");
                String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                    + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;'>"
                    + "<div style='max-width:600px;margin:auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,.1);'>"
                    + "<div style='background:#1e293b;padding:24px;text-align:center;'><h1 style='color:#fff;margin:0;'>🎓 EduLink</h1>"
                    + "<p style='color:#94a3b8;margin:4px 0 0;'>Session Summary</p></div>"
                    + "<div style='padding:30px;'>"
                    + "<h2 style='color:#1e293b;'>Your tutoring session has ended</h2>"
                    + "<p style='color:#555;'><strong>Duration:</strong> " + session.getDurationMinutes() + " min</p>"
                    + "<p style='color:#555;'><strong>Messages:</strong> " + session.getMessageCount() + "</p>"
                    + "<div style='background:#f1f5f9;border-left:4px solid #6366f1;padding:16px;border-radius:6px;margin:20px 0;'>"
                    + "<h3 style='color:#1e293b;margin:0 0 8px;'>📝 AI Summary</h3>"
                    + "<p style='color:#374151;margin:0;'>" + summary + "</p></div>"
                    + "<p style='color:#555;'>Thank you for using EduLink! Your credits have been updated.</p>"
                    + "</div><div style='background:#1e293b;padding:12px;text-align:center;'><p style='color:#94a3b8;margin:0;font-size:12px;'>© 2026 EduLink — ESPRIT</p></div>"
                    + "</div></body></html>";
                message.setContent(html, "text/html; charset=utf-8");
                Transport.send(message);
            }
            return true;
        } catch (MessagingException e) {
            System.err.println("❌ Summary mail error: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────
    private static String[] resolveEmails(int tutorId, int studentId) {
        List<String> emails = new ArrayList<>();
        try (java.sql.Connection c = com.edulink.gui.util.MyConnection.getInstance().getCnx();
             java.sql.PreparedStatement ps = c.prepareStatement(
                 "SELECT email FROM user WHERE id IN (?,?)")) {
            ps.setInt(1, tutorId);
            ps.setInt(2, studentId);
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) emails.add(rs.getString("email"));
        } catch (Exception e) {
            System.err.println("[MailService] resolveEmails error: " + e.getMessage());
        }
        return emails.toArray(new String[0]);
    }

    private static String buildReservationTemplate(Event event, Reservation reservation) {
        String type     = event.isOnline() ? "🌐 En ligne" : "📍 " + event.getLocation();
        String meetInfo = (event.isOnline() && event.getMeetLink() != null)
            ? "<p style='text-align:center;'><a href='" + event.getMeetLink()
              + "' style='background:#4CAF50;color:white;padding:10px 20px;border-radius:5px;text-decoration:none;'>🎥 Rejoindre le Meet</a></p>"
            : "";
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
            + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;'>"
            + "<div style='max-width:600px;margin:auto;background:white;border-radius:10px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.1);'>"
            + "<div style='background:#2C3E50;padding:30px;text-align:center;'><h1 style='color:white;margin:0;'>🎓 EduLink</h1>"
            + "<p style='color:#BDC3C7;margin:5px 0 0;'>Plateforme étudiante ESPRIT</p></div>"
            + "<div style='padding:30px;'><h2 style='color:#2C3E50;'>✅ Réservation confirmée !</h2>"
            + "<p style='color:#555;'>Votre réservation pour l'événement suivant a bien été enregistrée :</p>"
            + "<div style='background:#f8f9fa;border-left:4px solid #3498DB;padding:15px;border-radius:5px;margin:20px 0;'>"
            + "<h3 style='color:#2C3E50;margin:0 0 10px;'>📅 " + event.getTitle() + "</h3>"
            + "<p>📆 <strong>Début :</strong> " + event.getDateStart() + "</p>"
            + "<p>🏁 <strong>Fin :</strong> " + event.getDateEnd() + "</p>"
            + "<p>📍 <strong>Type :</strong> " + type + "</p></div>"
            + meetInfo
            + "<div style='text-align:center;margin:30px 0;'>"
            + "<p>Présentez ce QR Code à l'entrée</p>"
            + "<img src='cid:qrcode' width='200' height='200' style='border:5px solid #2C3E50;border-radius:10px;'/>"
            + "<p style='color:#999;font-size:12px;'>Réservation #" + reservation.getId() + "</p></div></div>"
            + "<div style='background:#2C3E50;padding:15px;text-align:center;'><p style='color:#BDC3C7;margin:0;font-size:12px;'>© 2026 EduLink — ESPRIT</p></div>"
            + "</div></body></html>";
    }
}