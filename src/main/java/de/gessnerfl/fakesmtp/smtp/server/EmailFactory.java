package de.gessnerfl.fakesmtp.smtp.server;

import com.sun.mail.util.BASE64DecoderStream;
import de.gessnerfl.fakesmtp.model.*;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmailFactory {
    public static final String UNDEFINED = "<undefined>";

    private final TimestampProvider timestampProvider;

    @Autowired
    public EmailFactory(TimestampProvider timestampProvider) {
        this.timestampProvider = timestampProvider;
    }

    public Email convert(RawData rawData) throws IOException {
        try {
            var mimeMessage = rawData.toMimeMessage();
            var subject = Objects.toString(mimeMessage.getSubject(), UNDEFINED);
            var contentType = ContentType.fromString(mimeMessage.getContentType());
            var messageContent = mimeMessage.getContent();
            var messageId = mimeMessage.getMessageID();

            switch (contentType) {
                case HTML, PLAIN, OCTET_STREAM:
                    return createPlainOrHtmlMail(rawData, subject, contentType, messageContent, messageId);
                case MULTIPART_ALTERNATIVE, MULTIPART_MIXED, MULTIPART_RELATED:
                    return createMultipartMail(rawData, subject, (Multipart) messageContent, messageId);
                default:
                    throw new IllegalStateException("Unsupported e-mail content type " + mimeMessage.getContentType());
            }
        } catch (MessagingException e) {
            return buildFallbackEmail(rawData);
        }
    }

    private Email createPlainOrHtmlMail(RawData rawData, String subject, ContentType contentType, Object messageContent, String messageId) {
        var email = createEmailFromRawData(rawData);
        email.setSubject(subject);
        createEmailContent(rawData, contentType, messageContent).ifPresent(email::addContent);
        email.setMessageId(messageId);
        return email;
    }

    private Email createMultipartMail(RawData rawData, String subject, Multipart multipart, String messageId) throws MessagingException, IOException {
        var email = createEmailFromRawData(rawData);
        email.setSubject(subject);
        email.setMessageId(messageId);

        appendMultipartBodyParts(email, rawData, multipart);

        return email;
    }

    private void appendMultipartBodyParts(Email email, RawData rawData, Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            final var part = multipart.getBodyPart(i);
            final var disposition = part.getDisposition();
            if (disposition == null || disposition.equalsIgnoreCase(Part.INLINE)) {
                appendMultipartContent(email, rawData, part);
            } else if (disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                var attachment = createAttachment(part);
                email.addAttachment(attachment);
            }
        }
    }

    private void appendMultipartContent(Email email, RawData rawData, BodyPart part) throws MessagingException, IOException {
        var partContentType = ContentType.fromString(part.getContentType());
        if (partContentType == ContentType.HTML || partContentType == ContentType.PLAIN) {
            final var partContent = part.getContent();
            createEmailContent(rawData, partContentType, partContent).ifPresent(email::addContent);
        } else if (partContentType == ContentType.MULTIPART_RELATED || partContentType == ContentType.MULTIPART_ALTERNATIVE) {
            final var content = (Multipart) part.getContent();
            appendMultipartBodyParts(email, rawData, content);
        } else if (partContentType == ContentType.IMAGE) {
            createInlineImage(part).ifPresent(email::addInlineImage);
        }
    }

    private Email buildFallbackEmail(RawData rawData) {
        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData(rawData.getContentAsString());

        var email = createEmailFromRawData(rawData);
        email.setSubject(UNDEFINED);
        email.addContent(content);
        return email;
    }

    private Email createEmailFromRawData(RawData rawData) {
        var email = new Email();
        email.setFromAddress(rawData.getFrom());
        email.setToAddress(rawData.getTo());
        email.setReceivedOn(timestampProvider.now());
        email.setRawData(rawData.getContentAsString());
        return email;
    }

    private Optional<EmailContent> createEmailContent(RawData rawData, ContentType contentType, Object messageContent) {
        var data = getMessageContentAsString(messageContent).map(this::normalizeContent).orElseGet(() -> normalizeContent(rawData.getContentAsString()));
        if (data == null) {
            return Optional.empty();
        }
        var content = new EmailContent();
        content.setContentType(contentType);
        content.setData(data);
        return Optional.of(content);
    }

    private Optional<InlineImage> createInlineImage(final BodyPart part) throws MessagingException, IOException {
        var contentType = part.getContentType();
        Object rawContent = part.getContent();
        Optional<String> data = getMessageContentAsString(rawContent);
        return extractContentId(part).flatMap(contentId ->
            data.map(d -> {
                var img = new InlineImage();
                img.setContentId(contentId);
                img.setContentType(contentType);
                img.setData(d);
                return img;
            }));
    }

    private Optional<String> getMessageContentAsString(Object rawContent) {
        var content = rawContent instanceof BASE64DecoderStream stream ? readBase64EncodedData(stream) : Objects.toString(rawContent, null);
        return Optional.ofNullable(content);
    }

    private String readBase64EncodedData(BASE64DecoderStream stream){
        try {
            var byteArray = IOUtils.toByteArray(stream);
            return Base64.getEncoder().encodeToString(byteArray);
        } catch (IOException e) {
            throw new EmailProcessingException("Failed to read message content", e);
        }
    }

    private Optional<String> extractContentId(BodyPart part) throws MessagingException {
        var headerValues = part.getHeader("Content-ID");
        if (headerValues != null && headerValues.length > 0) {
            var contentId = headerValues[0];
            return Optional.of(contentId.substring(1, contentId.length()-1));
        }
        return Optional.empty();
    }

    private EmailAttachment createAttachment(BodyPart part) throws MessagingException, IOException {
        var attachment = new EmailAttachment();
        attachment.setFilename(part.getFileName());
        attachment.setData(IOUtils.toByteArray(part.getInputStream()));
        return attachment;
    }

    private String normalizeContent(String input) {
        return input != null && !input.trim().isEmpty() ? input.trim() : null;
    }
}
