package com.example.nextalkapp;

import android.os.AsyncTask;
import android.util.Log;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JavaMailAPI extends AsyncTask<Void, Void, Void> {

    // THAY ĐỔI THÔNG TIN TẠI ĐÂY
    private static final String EMAIL = "nextalkproj@gmail.com"; // Email gửi
    private static final String PASSWORD = "taor chjz qoff rmjs"; // MẬT KHẨU ỨNG DỤNG 16 SỐ (KHÔNG PHẢI MẬT KHẨU ĐĂNG NHẬP)

    private String recipient;
    private String subject;
    private String content;

    public JavaMailAPI(String recipient, String subject, String content) {
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL, PASSWORD);
            }
        });

        try {
            MimeMessage mm = new MimeMessage(session);
            mm.setFrom(new InternetAddress(EMAIL));
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            mm.setSubject(subject);
            mm.setText(content);
            Transport.send(mm);
            Log.d("JavaMailAPI", "Email sent successfully to " + recipient);
        } catch (MessagingException e) {
            Log.e("JavaMailAPI", "Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}