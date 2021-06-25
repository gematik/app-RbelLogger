package de.gematik.rbellogger.data.elements;

import de.gematik.rbellogger.util.GenericPrettyPrinter;
import de.gematik.rbellogger.util.RbelException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.util.Iterable;
import wiremock.com.google.common.collect.Streams;

@Data
public class RbelAsn1Element extends RbelElement {

    private static final GenericPrettyPrinter<ASN1Encodable> ASN1_PRETTY_PRINTER = new GenericPrettyPrinter<>(
        asn1 -> (asn1 instanceof ASN1Sequence) || (asn1 instanceof ASN1Set),
        Object::toString,
        asn1 -> Streams.stream((Iterable<ASN1Encodable>) asn1)
    );

    private final ASN1Encodable asn1Element;
    private final RbelElement nestedElement;
    private byte[] unparsedBytes;

    @Override
    public List<RbelElement> getChildNodes() {
        return List.of(nestedElement);
    }

    @Override
    public List<Entry<String, RbelElement>> getChildElements() {
        if (nestedElement == null) {
            return Collections.emptyList();
        } else if (nestedElement instanceof RbelAsn1Element) {
            return List.of(Pair.of("content", nestedElement));
        } else if (nestedElement.getChildElements().isEmpty()) {
            return List.of(Pair.of("content", nestedElement));
        } else {
            return nestedElement.getChildElements();
        }
    }

    @Override
    public boolean isNestedBoundary() {
        if (nestedElement != null && nestedElement.getChildElements().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String getContent() {
        return ASN1_PRETTY_PRINTER.prettyPrint(asn1Element);
    }

    public byte[] getEncoded() {
        try {
            return asn1Element.toASN1Primitive().getEncoded();
        } catch (IOException e) {
            throw new RbelException("Error while encoding ASN.1 element", e);
        }
    }
}
