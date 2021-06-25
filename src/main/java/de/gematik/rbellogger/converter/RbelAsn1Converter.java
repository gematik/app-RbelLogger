package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.data.elements.*;
import de.gematik.rbellogger.util.RbelException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Base64.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.*;
import org.bouncycastle.util.Iterable;

@Slf4j
public class RbelAsn1Converter implements RbelConverterPlugin {

    @Override
    public boolean canConvertElement(RbelElement rbel, RbelConverter context) {
        return true;
    }

    @Override
    public RbelElement convertElement(RbelElement rbel, RbelConverter context) {
        return convertElementToAsn1Optional(rbel, context)
            .orElse(null);
    }

    private Optional<RbelAsn1Element> convertElementToAsn1Optional(RbelElement rbel, RbelConverter context) {
        return Optional.ofNullable(rbel)
            .filter(RbelBinaryElement.class::isInstance)
            .map(RbelBinaryElement.class::cast)
            .map(RbelBinaryElement::getRawData)
            .flatMap(data -> tryToParseAsn1Structure(data, context))
            .or(() -> safeConvertBase64Using(rbel.getContent(), Base64.getDecoder(), context))
            .or(() -> safeConvertBase64Using(rbel.getContent(), Base64.getUrlDecoder(), context))
            .or(() -> safeConvertBase64Using(rbel.getContent(), Base64.getMimeDecoder(), context));
    }

    private Optional<RbelAsn1Element> safeConvertBase64Using(String input, Decoder decoder, RbelConverter context) {
        try {
            return Optional.ofNullable(decoder.decode(input))
                .flatMap(data -> tryToParseAsn1Structure(data, context));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<RbelAsn1Element> tryToParseAsn1Structure(byte[] data, RbelConverter converter) {
        try (ASN1InputStream input = new ASN1InputStream(data)) {
            ASN1Primitive primitive;
            while ((primitive = input.readObject()) != null) {
                ASN1Sequence asn1 = ASN1Sequence.getInstance(primitive);
                final RbelAsn1Element asn1Element = encaseAsn1Element(asn1, converter);
                if (input.available() != 0) {
                    log.warn(
                        "Found a ASN.1-Stream with more then a single element. The rest of the element will be skipped");
                    asn1Element.setUnparsedBytes(input.readAllBytes());
                }
                return Optional.of(asn1Element);
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private RbelAsn1Element encaseAsn1Element(ASN1Encodable asn1, RbelConverter converter) {
        if ((asn1 instanceof ASN1Sequence)
            || (asn1 instanceof ASN1Set)) {
            List<RbelElement> rbelSequence = new ArrayList<>();
            for (ASN1Encodable encodable : ((Iterable<ASN1Encodable>) asn1)) {
                rbelSequence.add(encaseAsn1Element(encodable, converter));
            }
            return new RbelAsn1Element(asn1, new RbelListElement(rbelSequence));
        } else if (asn1 instanceof ASN1TaggedObject) {
            return new RbelAsn1Element(asn1, new RbelMapElement(Map.of(
                "tag", new RbelIntegerElement(((ASN1TaggedObject) asn1).getTagNo()),
                "content", encaseAsn1Element(((ASN1TaggedObject) asn1).getObject(), converter))));
        } else if (asn1 instanceof ASN1Integer) {
            return new RbelAsn1Element(asn1, new RbelIntegerElement(((ASN1Integer) asn1).getValue()));
        } else if (asn1 instanceof ASN1ObjectIdentifier) {
            return new RbelAsn1Element(asn1, converter.convertElement(((ASN1ObjectIdentifier) asn1).getId()));
        } else if (asn1 instanceof ASN1OctetString) {
            return new RbelAsn1Element(asn1, converter.convertElement(((ASN1OctetString) asn1).getOctets()));
        } else if (asn1 instanceof ASN1BitString) {
            return new RbelAsn1Element(asn1, converter.convertElement(((ASN1BitString) asn1).getOctets()));
        } else if (asn1 instanceof ASN1String) {
            return new RbelAsn1Element(asn1, converter.convertElement(((ASN1String) asn1).getString()));
        } else if (asn1 instanceof ASN1Boolean) {
            return new RbelAsn1Element(asn1, new RbelBooleanElement(((ASN1Boolean) asn1).isTrue()));
        } else if (asn1 instanceof ASN1Null) {
            return new RbelAsn1Element(asn1, new RbelStringElement("Real ASN.1 null"));
        } else if (asn1 instanceof ASN1UTCTime) {
            try {
                return new RbelAsn1Element(asn1, new RbelDateTimeElement(ZonedDateTime.ofInstant(
                    ((ASN1UTCTime) asn1).getAdjustedDate().toInstant(), ZoneId.of("UTC")
                )));
            } catch (ParseException e) {
                throw new RbelException("Error during time-conversion of " + asn1, e);
            }
        } else {
            log.warn("Unable to parse {}, using fallback Null...", asn1.getClass().getSimpleName());
            return new RbelAsn1Element(asn1, new RbelStringElement("fuck"));
        }
    }
}
