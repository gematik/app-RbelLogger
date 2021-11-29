package de.gematik.rbellogger.util;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.RbelTcpIpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHostnameFacet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RbelFileWriterUtils {

    public final static String FILE_DIVIDER = "\n";
    private static final String RAW_MESSAGE_CONTENT = "rawMessageContent";
    private static final String SENDER_HOSTNAME = "senderHostname";
    private static final String RECEIVER_HOSTNAME = "receiverHostname";
    private static final String SEQUENCE_NUMBER = "sequenceNumber";

    public static String convertToRbelFileString(RbelElement rbelElement) {
        final JSONObject jsonObject = new JSONObject(Map.of(
            RAW_MESSAGE_CONTENT, Base64.getEncoder().encodeToString(rbelElement.getRawContent()),
            SENDER_HOSTNAME, rbelElement.getFacet(RbelTcpIpMessageFacet.class)
                .map(RbelTcpIpMessageFacet::getSender)
                .filter(Objects::nonNull)
                .flatMap(element -> element.getFacet(RbelHostnameFacet.class))
                .map(RbelHostnameFacet::toString)
                .orElse(""),
            RECEIVER_HOSTNAME, rbelElement.getFacet(RbelTcpIpMessageFacet.class)
                .map(RbelTcpIpMessageFacet::getReceiver)
                .filter(Objects::nonNull)
                .flatMap(element -> element.getFacet(RbelHostnameFacet.class))
                .map(RbelHostnameFacet::toString)
                .orElse(""),
            SEQUENCE_NUMBER, rbelElement.getFacet(RbelTcpIpMessageFacet.class)
                .map(RbelTcpIpMessageFacet::getSequenceNumber)
                .map(Object::toString)
                .orElse("")
        ));
        return jsonObject + FILE_DIVIDER;
    }

    public static void convertFromRbelFile(String rbelFileContent, RbelConverter rbelConverter) {
        Arrays.stream(rbelFileContent.split(FILE_DIVIDER))
            .map(JSONObject::new)
            .forEach(content -> parseFileObject(rbelConverter, content));
    }

    private static RbelElement parseFileObject(RbelConverter rbelConverter, JSONObject messageObject) {
        try {
            return rbelConverter.parseMessage(Base64.getDecoder().decode(messageObject.getString(RAW_MESSAGE_CONTENT)),
                RbelHostname.fromString(messageObject.getString(SENDER_HOSTNAME)),
                RbelHostname.fromString(messageObject.getString(RECEIVER_HOSTNAME)));
        } catch (Exception e) {
            throw new RbelFileReadingException("Error while converting from object '" + messageObject.toString() + "'", e);
        }
    }

    private static class RbelFileReadingException extends RuntimeException {
        public RbelFileReadingException(String s, Exception e) {
            super(s, e);
        }
    }
}
