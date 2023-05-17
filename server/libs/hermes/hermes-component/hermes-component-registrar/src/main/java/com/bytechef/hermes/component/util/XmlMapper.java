
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.hermes.component.util;

import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
@Component
public class XmlMapper implements XmlUtils.XmlMapper {

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private static final com.fasterxml.jackson.dataformat.xml.XmlMapper xmlMapper =
        new com.fasterxml.jackson.dataformat.xml.XmlMapper();

    @Override
    public Map<String, ?> read(InputStream inputStream) {
        try {
            return xmlMapper.readValue(inputStream, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to read xml value", exception);
        }
    }

    @Override
    public Map<String, ?> read(String xml) {
        try {
            return xmlMapper.convertValue(xmlMapper.readTree(xml), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to convert xml value", exception);
        }
    }

    @Override
    public <T> T read(InputStream inputStream, String path) {
        try {
            return xmlMapper.readValue(
                parse(path, documentBuilder -> parse(inputStream, documentBuilder)), new TypeReference<T>() {});
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to read xml", exception);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> readList(InputStream inputStream) {
        try {
            return xmlMapper.readValue(inputStream, List.class);
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to read xml value", exception);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> readList(String xml) {
        try {
            return xmlMapper.readValue(xml, List.class);
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to read xml value", exception);
        }
    }

    @Override
    public Stream<Map<String, ?>> stream(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Non null stream reference expected");

        try {
            return new XmlStreamReaderStream(inputStream, xmlMapper);
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to stream xml", exception);
        }
    }

    @Override
    public String write(Object object) {
        return write(object, "root");
    }

    @Override
    public String write(Object object, String rootName) {
        try {
            return xmlMapper.writer()
                .withRootName(rootName)
                .writeValueAsString(object);
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to write xml", exception);
        }
    }

    private static Document parse(InputStream inputStream, DocumentBuilder documentBuilder) {
        try {
            return documentBuilder.parse(inputStream);
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to parse xml", exception);
        }
    }

    private static String parse(String path, Function<DocumentBuilder, Document> parseFunction) {
        StringBuffer sb = new StringBuffer();

        try {
            DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            XPath xPath = X_PATH_FACTORY.newXPath();

            Document document = parseFunction.apply(documentBuilder);

            NodeList nodeList = (NodeList) xPath.compile(path)
                .evaluate(document, XPathConstants.NODESET);

            sb.append("<root>");

            for (int i = 0; i < nodeList.getLength(); i++) {
                sb.append(nodeToString(nodeList.item(i)));
            }

            sb.append("</root>");
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to parse xml", exception);
        }

        return sb.toString();
    }

    private static String nodeToString(Node node) {
        try {
            StringWriter stringWriter = new StringWriter();
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();

            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(stringWriter));

            return stringWriter.toString();
        } catch (Exception exception) {
            throw new ComponentExecutionException("Unable to transform xml node", exception);
        }
    }
}
