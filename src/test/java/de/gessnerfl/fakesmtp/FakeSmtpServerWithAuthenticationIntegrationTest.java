package de.gessnerfl.fakesmtp;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.server.EmailServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@ActiveProfiles({"endtoendtest_with_authentication", "default"})
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class FakeSmtpServerWithAuthenticationIntegrationTest {
    private static final String DEFAULT_FROM_ADDRESS = "sender@example.com";
    private static final String DEFAULT_TO_ADDRESS = "receiver@exmaple.com";

    @Autowired
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private EmailServer emailServer;

    @Test
    public void shouldSuccessfullyReceivePlainTextEmail(){
        var subject = "subject integration test with authentication";
        var content = "content plain text integration test with authentication";
        var testMailSender = new TestMailSender(fakeSmtpConfigurationProperties.getPort(), fakeSmtpConfigurationProperties.getAuthentication().getUsername(), fakeSmtpConfigurationProperties.getAuthentication().getPassword());
        testMailSender.sendPlainTextMail(DEFAULT_FROM_ADDRESS, DEFAULT_TO_ADDRESS, subject, content);

        List<Email> emails = emailRepository.findAll(Sort.by(Sort.Direction.DESC, "receivedOn"));

        assertEquals(subject, emails.get(0).getSubject());
        assertEquals(content, emails.get(0).getPlainContent().get().getData());
    }

    @Test(expected = MailAuthenticationException.class)
    public void shouldFailToReceiveEmailWhenLoginIsNotValid(){
        var subject = "subject integration test with authentication";
        var content = "content plain text integration test with authentication";
        var testMailSender = new TestMailSender(fakeSmtpConfigurationProperties.getPort(), fakeSmtpConfigurationProperties.getAuthentication().getUsername(), "invalid password");
        testMailSender.sendPlainTextMail(DEFAULT_FROM_ADDRESS, DEFAULT_TO_ADDRESS, subject, content);
    }

    @Test
    public void shouldReceiveMailWhenNoAuthenticationIsProvidedAsServerIsNotForcingAuthentication(){
        var subject = "subject integration test with authentication";
        var content = "content plain text integration test with authentication";
        var testMailSender = new TestMailSender(fakeSmtpConfigurationProperties.getPort());
        testMailSender.sendPlainTextMail(DEFAULT_FROM_ADDRESS, DEFAULT_TO_ADDRESS, subject, content);

        List<Email> emails = emailRepository.findAll(Sort.by(Sort.Direction.DESC, "receivedOn"));

        assertEquals(subject, emails.get(0).getSubject());
        assertEquals(content, emails.get(0).getPlainContent().get().getData());
    }

}
