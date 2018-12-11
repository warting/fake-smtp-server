package de.gessnerfl.fakesmtp;

import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Objects;
import java.util.Properties;

public class TestMailSender {

    private final MailSender mailSender;

    public static TestMailSender createSpring(int port) {
        return new TestMailSender(SpringJavaMailSender.createFor(port));
    }

    public static TestMailSender createSpring(int port, String user, String password) {
        Objects.requireNonNull(user, "User missing");
        Objects.requireNonNull(password, "Password missing");
        return new TestMailSender(NativeJavaMailSender.createFor(port, user, password));
    }

    public static TestMailSender createNative(int port) {
        return new TestMailSender(SpringJavaMailSender.createFor(port));
    }

    public static TestMailSender createNative(int port, String user, String password) {
        Objects.requireNonNull(user, "User missing");
        Objects.requireNonNull(password, "Password missing");
        return new TestMailSender(NativeJavaMailSender.createFor(port, user, password));
    }

    private TestMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPlainTextMail(final String from, final String to, final String subject, final String text) throws MessagingException {
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false);

        mailSender.send(message);
    }

    public void sendHtmlEmail(final String from, final String to, final String subject, final String html) throws MessagingException {
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);
    }

    public void sendMimeAlternativeEmail(final String from, final String to, final String subject, final String text, final String html, final AttachmentSource... attachmentSources) throws MessagingException {
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, html);
        for (AttachmentSource attachmentSource : attachmentSources) {
            helper.addAttachment(attachmentSource.name, attachmentSource.resource);
        }
        mailSender.send(message);
    }

    public static class AttachmentSource {
        public final String name;
        public final InputStreamSource resource;

        public AttachmentSource(String name, InputStreamSource resource) {
            this.name = name;
            this.resource = resource;
        }
    }

    private interface MailSender {
        void send(MimeMessage message) throws MessagingException;

        MimeMessage createMimeMessage() throws MessagingException;
    }

    private static class SpringJavaMailSender implements MailSender {
        private final JavaMailSender mailSender;

        static SpringJavaMailSender createFor(int port) {
            return createFor(port, null, null);
        }

        static SpringJavaMailSender createFor(int port, String user, String password) {
            var mailSender = new JavaMailSenderImpl();
            mailSender.setHost("localhost");
            mailSender.setPort(port);

            var properties = mailSender.getJavaMailProperties();
            properties.put("mail.transport.protocol", "smtp");
            properties.put("mail.debug", "false");
            if (user != null && password != null) {
                mailSender.setUsername(user);
                mailSender.setPassword(password);
                properties.put("mail.smtp.auth", "true");
            } else {
                properties.put("mail.smtp.auth", "false");
            }

            mailSender.setJavaMailProperties(properties);
            return new SpringJavaMailSender(mailSender);
        }

        private SpringJavaMailSender(JavaMailSender mailSender) {
            this.mailSender = mailSender;
        }

        @Override
        public void send(MimeMessage message) {
            mailSender.send(message);
        }

        @Override
        public MimeMessage createMimeMessage() {
            return mailSender.createMimeMessage();
        }
    }

    private static class NativeJavaMailSender implements MailSender {
        static NativeJavaMailSender createFor(int port) {
            Properties properties = createBasicProperties(port);
            properties.put("mail.smtp.auth", "false");

            var session = Session.getInstance(properties);
            return new NativeJavaMailSender(session);
        }

        static NativeJavaMailSender createFor(int port, String user, String password) {
            Properties properties = createBasicProperties(port);
            properties.put("mail.smtp.auth", "true");

            var authenticator = new MailAuthenticator(user, password);
            var session = Session.getInstance(properties, authenticator);

            return new NativeJavaMailSender(session);
        }

        private static Properties createBasicProperties(int port) {
            var properties = new Properties();
            properties.put("mail.smtp.host", "localhost");
            properties.put("mail.smtp.port", port);
            properties.put("mail.debug", "false");

            //properties.put("mail.smtp.starttls.enable","true");

            //properties.put("mail.smtp.socketFactory.port", port);
            //properties.put("mail.smtp.socketFactory.class",  "javax.net.ssl.SSLSocketFactory");
            return properties;
        }

        private final Session session;

        private NativeJavaMailSender(Session session) {
            this.session = session;
        }

        @Override
        public void send(MimeMessage message) throws MessagingException {
            Transport.send(message);
        }

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(session);
        }
    }

    private static class MailAuthenticator extends Authenticator {
        private final String user;
        private final String password;

        private MailAuthenticator(String user, String password) {
            this.user = user;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password);
        }
    }
}
