/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.util;

import de.gematik.rbellogger.converter.RbelConverter;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.RbelHostname;
import de.gematik.rbellogger.data.facet.RbelTcpIpMessageFacet;
import de.gematik.rbellogger.data.facet.RbelHostnameFacet;
import de.gematik.rbellogger.data.facet.RbelMessageTimingFacet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RbelFileWriterUtils {

    public final static String FILE_DIVIDER = "\n";
    private static final String RAW_MESSAGE_CONTENT = "rawMessageContent";
    private static final String SENDER_HOSTNAME = "senderHostname";
    private static final String RECEIVER_HOSTNAME = "receiverHostname";
    private static final String SEQUENCE_NUMBER = "sequenceNumber";
    private static final String MESSAGE_TIME = "timestamp";
    private static final String MESSAGE_UUID = "uuid";

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
                .orElse(""),
            MESSAGE_TIME, rbelElement.getFacet(RbelMessageTimingFacet.class)
                .map(RbelMessageTimingFacet::getTransmissionTime)
                .map(Object::toString)
                .orElse(""),
            MESSAGE_UUID, rbelElement.getUuid()
        ));
        return jsonObject + FILE_DIVIDER;
    }

    public static void convertFromRbelFile(String rbelFileContent, RbelConverter rbelConverter) {
        Arrays.stream(rbelFileContent.split(FILE_DIVIDER))
            .filter(StringUtils::isNotEmpty)
            .map(JSONObject::new)
            .forEach(content -> parseFileObject(rbelConverter, content));
    }

    private static void parseFileObject(RbelConverter rbelConverter, JSONObject messageObject) {
        try {
            final String msgUuid = messageObject.optString(MESSAGE_UUID);
            if (new ArrayList<>(rbelConverter.getMessageHistory()).stream()
                .anyMatch(msg -> msg.getUuid().equals(msgUuid))) {
                return;
            }
            rbelConverter.parseMessage(RbelElement.builder()
                    .rawContent(Base64.getDecoder().decode(messageObject.getString(RAW_MESSAGE_CONTENT)))
                    .uuid(msgUuid)
                    .parentNode(null)
                    .build(),
                RbelHostname.fromString(messageObject.getString(SENDER_HOSTNAME)).orElse(null),
                RbelHostname.fromString(messageObject.getString(RECEIVER_HOSTNAME)).orElse(null));
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
