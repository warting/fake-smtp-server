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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@ActiveProfiles({"endtoendtest", "default"})
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class FakeSmtpServerIntegrationTest {
    private static final String DEFAULT_FROM_ADDRESS = "sender@example.com";
    private static final String DEFAULT_TO_ADDRESS = "receiver@exmaple.com";

    @Autowired
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private EmailServer emailServer;

    private TestMailSender testMailSender;

    @Before
    public void init() throws InterruptedException {
        testMailSender = new TestMailSender(fakeSmtpConfigurationProperties.getPort());
    }

    @Test
    public void shouldSuccessfullyReceivePlainTextEmail(){
        var subject = "subject integration test";
        var content = "content plain text integration test";
        testMailSender.sendPlainTextMail(DEFAULT_FROM_ADDRESS, DEFAULT_TO_ADDRESS, subject, content);

        List<Email> emails = emailRepository.findAll(Sort.by(Sort.Direction.DESC, "receivedOn"));

        assertEquals(subject, emails.get(0).getSubject());
        assertEquals(content, emails.get(0).getPlainContent().get().getData());
    }

}
