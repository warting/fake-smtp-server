package de.gessnerfl.fakesmtp;

import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import java.util.Objects;

public class TestMailSender {

    private final int port;
    private final String user;
    private final String password;

    public TestMailSender(int port) {
        this.port = port;
        this.user = null;
        this.password = null;
    }

    public TestMailSender(int port, String user, String password){
        Objects.requireNonNull(user, "User missing");
        Objects.requireNonNull(password, "Password missing");
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public void sendPlainTextMail(final String from, final String to, final String subject, final String text) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        getEmailSender().send(message);
    }

    public void sendHtmlEmail(final String from, final String to, final String subject, final String html){
        try {
            var sender = getEmailSender();

            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            sender.send(message);
        } catch (MessagingException e){
            throw new RuntimeException("Failed to create mail", e);
        }
    }

    public void sendMimeAlternativeEmail(final String from, final String to, final String subject, final String text, final String html, final AttachmentSource... attachmentSources) {
        try {
            var sender = getEmailSender();

            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            for(AttachmentSource attachmentSource : attachmentSources){
                helper.addAttachment(attachmentSource.name, attachmentSource.resource);
            }
            sender.send(message);
        } catch (MessagingException e){
            throw new RuntimeException("Failed to create mail", e);
        }
    }

    private JavaMailSender getEmailSender() {
        var mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(port);
        if(user != null && password != null){
            mailSender.setUsername(user);
            mailSender.setPassword(password);
        }

        var props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "false");

        return mailSender;
    }

    public static class AttachmentSource {
        public final String name;
        public final InputStreamSource resource;

        public AttachmentSource(String name, InputStreamSource resource) {
            this.name = name;
            this.resource = resource;
        }
    }
}
