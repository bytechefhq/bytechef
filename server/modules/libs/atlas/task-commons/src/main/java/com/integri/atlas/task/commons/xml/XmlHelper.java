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

package com.integri.atlas.task.commons.xml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
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
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author Ivica Cardic
 */
@Component
public class XmlHelper {

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    public Map<String, ?> read(String xml) {
        try {
            return XML_MAPPER.convertValue(XML_MAPPER.readTree(xml), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(String xml, Class<T> clazz) {
        try {
            return XML_MAPPER.readValue(xml, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(String xml, TypeReference<T> typeReference) {
        try {
            return XML_MAPPER.readValue(xml, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(InputStream inputStream, String path, Class<T> clazz) {
        return read(parse(path, documentBuilder -> parse(inputStream, documentBuilder)), clazz);
    }

    public <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference) {
        return read(parse(path, documentBuilder -> parse(inputStream, documentBuilder)), typeReference);
    }

    public <T> T read(String xml, String path, Class<T> clazz) {
        return read(parse(path, documentBuilder -> parse(xml, documentBuilder)), clazz);
    }

    public <T> T read(String xml, String path, TypeReference<T> typeReference) {
        return read(parse(path, (documentBuilder -> parse(xml, documentBuilder))), typeReference);
    }

    public Stream<Map<String, ?>> stream(InputStream inputStream) {
        try {
            return new XmlStreamReaderStream(inputStream, XML_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String write(Object object) {
        return write(object, "root");
    }

    public String write(Object object, String rootName) {
        try {
            return XML_MAPPER.writer().withRootName(rootName).writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Document parse(InputStream inputStream, DocumentBuilder documentBuilder) {
        try {
            return documentBuilder.parse(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Document parse(String xml, DocumentBuilder documentBuilder) {
        try {
            return documentBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String parse(String path, Function<DocumentBuilder, Document> parseFunction) {
        StringBuffer sb = new StringBuffer();
        try {
            DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            XPath xPath = X_PATH_FACTORY.newXPath();

            Document document = parseFunction.apply(documentBuilder);

            NodeList nodeList = (NodeList) xPath.compile(path).evaluate(document, XPathConstants.NODESET);

            sb.append("<root>");

            for (int i = 0; i < nodeList.getLength(); i++) {
                sb.append(nodeToString(nodeList.item(i)));
            }

            sb.append("</root>");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return sb.toString();
    }

    private String nodeToString(Node node) {
        try {
            StringWriter stringWriter = new StringWriter();
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();

            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(stringWriter));

            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
