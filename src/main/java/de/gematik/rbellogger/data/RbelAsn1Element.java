package de.gematik.rbellogger.data;

import de.gematik.rbellogger.util.RbelException;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.ASN1Encodable;

@Data
public class RbelAsn1Element extends RbelElement {

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
        return asn1Element.toString();
    }

    public byte[] getEncoded() {
        try {
            return asn1Element.toASN1Primitive().getEncoded();
        } catch (IOException e) {
            throw new RbelException("Error while encoding ASN.1 element", e);
        }
    }

    public String getBase64Value() {
        try {
            return Base64.getEncoder().encodeToString(
                asn1Element.toASN1Primitive().getEncoded());
        } catch (IOException e) {
            throw new RbelException("Error while encoding ASN.1 element", e);
        }
    }
}
