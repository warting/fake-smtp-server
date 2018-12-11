package de.gessnerfl.fakesmtp;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.mail.MessagingException;

public class TestDataCreator {

    private static final int NUMBER_OF_TEST_EMAILS = 5;
    private static final String DEFAULT_FROM_ADDRESS = "sender@example.com";
    private static final String DEFAULT_TO_ADDRESS = "receiver@exmaple.com";


    private TestMailSender sender;

    public static void main(String[] args) {
        try {
            var testDataCreator = new TestDataCreator(5025);
            for (var i = 0; i < NUMBER_OF_TEST_EMAILS; i++) {
                testDataCreator.createEmail(i);
                testDataCreator.createHtmlEmail(i);
                testDataCreator.createMimeAlternativeEmail(i);
            }
        }catch (MessagingException e){
            LoggerFactory.getLogger(TestMailSender.class).error("Failed to send mail", e);
            System.exit(-1);
        }
    }

    private TestDataCreator(int port){
        this.sender = TestMailSender.createSpring(port);
    }

    private void createEmail(int i) throws MessagingException {
        var subject = "Test-Plain-Mail " + i;
        var text = "This is the test mail number " + i;
        sender.sendPlainTextMail(DEFAULT_FROM_ADDRESS, DEFAULT_TO_ADDRESS, subject, text);
    }

    private void createHtmlEmail(int i) throws MessagingException {
        var subject = "Test-Html-Mail " + i;
        var html = "<html><head></head><body>This is the test mail number " + i + "</body>";
        sender.sendHtmlEmail(DEFAULT_FROM_ADDRESS, DEFAULT_TO_ADDRESS, subject, html);
    }

    private void createMimeAlternativeEmail(int i) throws MessagingException {
        var subject = "Test-Html-Mail " + i;
        var text = "This is the test mail number" + i;
        var html = "<html><head></head><body>This is the test mail number " + i + "</body>";
        var att1 = new TestMailSender.AttachmentSource("app-icon.png", new ClassPathResource("/static/gfx/app-icon.png"));
        var att2 = new TestMailSender.AttachmentSource("customizing.css", new ClassPathResource("/static/customizing.css"));
        sender.sendMimeAlternativeEmail(DEFAULT_FROM_ADDRESS, DEFAULT_TO_ADDRESS, subject, text, html, att1, att2);
    }

}
