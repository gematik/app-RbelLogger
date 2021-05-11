/*
 * Copyright (c) 2021 gematik GmbH
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
import de.gematik.rbellogger.data.RbelXmlElement;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.dom4j.*;
import org.dom4j.tree.AbstractBranch;
import org.dom4j.tree.DefaultElement;

public class RbelXmlConverter implements RbelConverterPlugin {

    private static final String XML_TEXT_KEY = "text";

    @Override
    public boolean canConvertElement(final RbelElement rbel, final RbelConverter context) {
        try {
            DocumentHelper.parseText(rbel.getContent());
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public RbelElement convertElement(final RbelElement rbel, final RbelConverter context) {
        try {
            return buildXmlElementForNode(DocumentHelper.parseText(rbel.getContent()), context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RbelXmlElement buildXmlElementForNode(Branch branch, RbelConverter converter) {
        RbelXmlElement result = new RbelXmlElement(branch);
        for (Object child : branch.content()) {
            if (child instanceof Text) {
                result.put(XML_TEXT_KEY, converter.convertMessage(((Text) child).getText()));
            } else if (child instanceof AbstractBranch) {
                final String childXmlName = ((AbstractBranch) child).getName();
                result.put(childXmlName, buildXmlElementForNode((AbstractBranch) child, converter));
            } else if (child instanceof Namespace) {
                final String childXmlName = ((Namespace) child).getPrefix();
                result.put(childXmlName, converter.convertMessage(((Namespace) child).getText()));
            } else {
                throw new RuntimeException("Could not convert XML element of type " + child.getClass().getSimpleName());
            }
        }

        if (branch instanceof Element) {
            for (Object attribute : ((Element) branch).attributes()) {
                if (!(attribute instanceof Attribute)) {
                    throw new RuntimeException("Could not convert XML attribute of type " + attribute.getClass().getSimpleName());
                }
                result.put(((Attribute) attribute).getName(),
                    converter.convertMessage(((Attribute) attribute).getText()));
            }
        }

        return result;
    }
}
