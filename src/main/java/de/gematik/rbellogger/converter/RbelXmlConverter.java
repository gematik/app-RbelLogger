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

package de.gematik.rbellogger.converter;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.facet.RbelRootFacet;
import de.gematik.rbellogger.data.facet.RbelXmlFacet;
import de.gematik.rbellogger.util.RbelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.AbstractBranch;
import org.dom4j.tree.DefaultComment;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

@Slf4j
public class RbelXmlConverter implements RbelConverterPlugin {

    private static final String XML_TEXT_KEY = "text";

    @Override
    public void consumeElement(final RbelElement rbel, final RbelConverter context) {
        final String content = rbel.getRawStringContent();
        if (content.contains("<") && content.contains(">")) {
            try {
                buildXmlElementForNode(parseXml(content.trim(), rbel), rbel, context);
                rbel.addFacet(new RbelRootFacet(rbel.getFacetOrFail(RbelXmlFacet.class)));
            } catch (DocumentException e) {
                log.trace("Exception while trying to parse XML. Skipping", e);
            }
        }
    }

    private Branch parseXml(String text, RbelElement parentElement) throws DocumentException {
        SAXReader reader = new SAXReader();//NOSONAR
        reader.setMergeAdjacentText(true);

        InputSource source = new InputSource(new StringReader(text));

        source.setEncoding(parentElement.getElementCharset().name());
        // see https://www.ietf.org/rfc/rfc3023 8.5 and 8.20: We always use the http-encoding.

        return reader.read(source);
    }

    private void buildXmlElementForNode(Branch branch, RbelElement parentElement, RbelConverter converter) {
        final List<Entry<String, RbelElement>> childElements = new ArrayList<>();
        parentElement.addFacet(RbelXmlFacet.builder()
            .sourceElement(branch)
            .childElements(childElements)
            .build());
        for (Object child : branch.content()) {
            if (child instanceof Text) {
                childElements
                    .add(Pair.of(XML_TEXT_KEY, converter.convertElement(((Text) child).getText(), parentElement)));
            } else if (child instanceof AbstractBranch) {
                final RbelElement element = new RbelElement(
                    ((AbstractBranch) child).asXML().getBytes(parentElement.getElementCharset()),
                    parentElement);
                buildXmlElementForNode((AbstractBranch) child, element, converter);
                childElements.add(Pair.of(((AbstractBranch) child).getName(), element));
            } else if (child instanceof Namespace) {
                final String childXmlName = ((Namespace) child).getPrefix();
                childElements
                    .add(Pair.of(childXmlName, converter.convertElement(((Namespace) child).getText(), parentElement)));
            } else if (child instanceof DefaultComment) {
                // do nothing
            } else {
                throw new RbelException("Could not convert XML element of type " + child.getClass().getSimpleName());
            }
        }

        if (childElements.stream()
            .map(Entry::getKey)
            .noneMatch(key -> key.equals(XML_TEXT_KEY))) {
            childElements.add(Pair.of(XML_TEXT_KEY, new RbelElement(new byte[]{}, parentElement)));
        }

        if (branch instanceof Element) {
            for (Object attribute : ((Element) branch).attributes()) {
                if (!(attribute instanceof Attribute)) {
                    throw new RbelException(
                        "Could not convert XML attribute of type " + attribute.getClass().getSimpleName());
                }
                childElements.add(Pair.of(
                    ((Attribute) attribute).getName(),
                    converter.convertElement(((Attribute) attribute).getText(), parentElement)));
            }
        }
    }
}
