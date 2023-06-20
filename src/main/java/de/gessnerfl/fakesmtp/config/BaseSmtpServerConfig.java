package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.smtp.auth.BasicUsernamePasswordValidator;
import de.gessnerfl.fakesmtp.smtp.auth.EasyAuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.smtp.command.CommandHandler;
import de.gessnerfl.fakesmtp.smtp.server.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.util.List;

@Profile("default")
@Configuration
public class BaseSmtpServerConfig implements SmtpServerConfig {

    private final BuildProperties buildProperties;
    private final FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    private final List<MessageListener> messageListeners;
    private final BasicUsernamePasswordValidator basicUsernamePasswordValidator;
    private final CommandHandler commandHandler;
    private final Logger logger;

    @Autowired
    public BaseSmtpServerConfig(BuildProperties buildProperties,
                                FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties,
                                List<MessageListener> messageListeners,
                                BasicUsernamePasswordValidator basicUsernamePasswordValidator,
                                CommandHandler commandHandler,
                                Logger logger) {
        this.buildProperties = buildProperties;
        this.fakeSmtpConfigurationProperties = fakeSmtpConfigurationProperties;
        this.messageListeners = messageListeners;
        this.basicUsernamePasswordValidator = basicUsernamePasswordValidator;
        this.commandHandler = commandHandler;
        this.logger = logger;
    }

    @Override
    @Bean
    public SmtpServer smtpServer() {
        BaseSmtpServer smtpServer = createBaseSmtpServerFor(new MessageListenerAdapter(messageListeners), sessionIdFactory());
        smtpServer.setPort(fakeSmtpConfigurationProperties.getPort());
        smtpServer.setBindAddress(fakeSmtpConfigurationProperties.getBindAddress());
        if (fakeSmtpConfigurationProperties.getAuthentication() != null) {
            configureAuthentication(smtpServer, fakeSmtpConfigurationProperties.getAuthentication());
        }
        if (fakeSmtpConfigurationProperties.getMaxMessageSize() != null){
            smtpServer.setMaxMessageSizeInBytes(fakeSmtpConfigurationProperties.getMaxMessageSize().toBytes());
        }
        smtpServer.setRequireTLS(fakeSmtpConfigurationProperties.isRequireTLS());
        smtpServer.setEnableTLS(fakeSmtpConfigurationProperties.isRequireTLS());
        return smtpServer;
    }

    @Bean
    public SessionIdFactory sessionIdFactory(){
        return new TimeBasedSessionIdFactory();
    }

    BaseSmtpServer createBaseSmtpServerFor(MessageListenerAdapter messageListenerAdapter, SessionIdFactory sessionIdFactory){
        final var softwareName = "FakeSMTPServer " + buildProperties.getVersion();
        return new BaseSmtpServer(softwareName, messageListenerAdapter, commandHandler, sessionIdFactory);
    }

    private void configureAuthentication(BaseSmtpServer smtpServer, FakeSmtpConfigurationProperties.Authentication authentication) {
        if (!StringUtils.hasText(authentication.getUsername())) {
            logger.error("Username is missing; skip configuration of authentication");
        } else if (!StringUtils.hasText(authentication.getPassword())) {
            logger.error("Password is missing; skip configuration of authentication");
        } else {
            logger.info("Setup simple username and password authentication for SMTP server");
            smtpServer.setAuthenticationHandlerFactory(new EasyAuthenticationHandlerFactory(basicUsernamePasswordValidator));
            smtpServer.setRequireAuth(true);
        }
    }

}
