/*
 * Copyright 2025 ByteChef
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

package com.bytechef.commons.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * XML utility class with secure parsing configuration to prevent XXE attacks.
 *
 * @author Ivica Cardic
 */
public class XmlUtils {

    private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
    private static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        DOCUMENT_BUILDER_FACTORY = createSecureDocumentBuilderFactory();
        TRANSFORMER_FACTORY = createSecureTransformerFactory();
    }

    private static XmlMapper xmlMapper;

    public static Map<String, ?> read(InputStream inputStream) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        return xmlMapper.readValue(
            inputStream, typeFactory.constructMapType(Map.class, String.class, Object.class));
    }

    public static <T> Map<String, T> read(InputStream inputStream, Class<T> valueType) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(inputStream, typeFactory.constructMapType(Map.class, String.class, valueType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static <T> Map<String, T> read(InputStream inputStream, Type type) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(inputStream, typeFactory.constructType(type));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static <T> Map<String, T> read(
        InputStream inputStream, TypeReference<T> typeReference) {

        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(
                inputStream,
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(typeReference)));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static Map<String, ?> read(String xml) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(xml, typeFactory.constructMapType(Map.class, String.class, Object.class));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static <T> Map<String, T> read(String xml, Class<T> valueType) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(xml, typeFactory.constructMapType(Map.class, String.class, valueType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static <T> Map<String, T> read(String xml, Type type) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(xml, typeFactory.constructType(type));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static <T> Map<String, T> read(String xml, TypeReference<T> typeReference) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(
                xml,
                typeFactory.constructMapType(
                    Map.class, typeFactory.constructType(String.class), typeFactory.constructType(typeReference)));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<?> readList(InputStream inputStream, String path) {
        return readList(inputStream, path, Object.class);
    }

    public static <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {

        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(
                parse(inputStream, path), typeFactory.constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static <T> List<T> readList(InputStream inputStream, String path, Type type) {
        TypeFactory typeFactory = xmlMapper.getTypeFactory();

        try {
            return xmlMapper.readValue(
                parse(inputStream, path),
                typeFactory.constructCollectionType(List.class, typeFactory.constructType(type)));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressFBWarnings("EI")
    public static void setXmlMapper(XmlMapper xmlMapper) {
        XmlUtils.xmlMapper = xmlMapper;
    }

    public static Stream<Map<String, ?>> stream(InputStream inputStream) {
        Validate.notNull(inputStream, "Non null stream reference expected");

        try {
            return new XmlStreamReaderStream(inputStream, xmlMapper);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static String write(Object object) {
        return write(object, "root");
    }

    public static String write(Object object, String rootName) {
        try {
            return xmlMapper.writer()
                .withRootName(rootName)
                .writeValueAsString(object);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Creates a secure DocumentBuilderFactory with XXE protections enabled. Disables DTD processing, external entities,
     * and external DTDs.
     */
    private static DocumentBuilderFactory createSecureDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            // Disable DTD processing entirely (strongest protection)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException e) {
            logger.warn("Could not disable DOCTYPE declaration, applying fallback XXE protections", e);

            // Fallback: disable external entities if DOCTYPE cannot be disabled
            try {
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException ex) {
                logger.warn("Could not configure all XXE protections", ex);
            }
        }

        // Set additional security attributes
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException e) {
            // Some parsers may not support these attributes
            logger.debug("Parser does not support ACCESS_EXTERNAL_DTD/SCHEMA attributes", e);
        }

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory;
    }

    /**
     * Creates a secure TransformerFactory with XXE protections enabled.
     */
    private static TransformerFactory createSecureTransformerFactory() {
        TransformerFactory factory = TransformerFactory.newInstance();

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException e) {
            // Some transformers may not support these attributes
            logger.debug("Transformer does not support ACCESS_EXTERNAL_DTD/STYLESHEET attributes", e);
        }

        return factory;
    }

    /**
     * Parses an XML input stream and extracts nodes matching the given XPath.
     *
     * <p>
     * <b>Security Note:</b> The XXE_DOCUMENT suppression is safe because the DocumentBuilderFactory is configured with
     * DTD and external entity processing disabled via {@link #createSecureDocumentBuilderFactory()}. The
     * XPATH_INJECTION suppression is appropriate because XPath expressions are internal implementation details, not
     * user input.
     */
    @SuppressFBWarnings({
        "XXE_DOCUMENT", "XPATH_INJECTION"
    })
    private static String parse(InputStream inputStream, String path) {
        StringBuffer sb = new StringBuffer();

        try {
            DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            XPath xPath = X_PATH_FACTORY.newXPath();

            Document document = documentBuilder.parse(inputStream);

            NodeList nodeList = (NodeList) xPath.compile(path)
                .evaluate(document, XPathConstants.NODESET);

            sb.append("<root>");

            for (int i = 0; i < nodeList.getLength(); i++) {
                sb.append(nodeToString(nodeList.item(i)));
            }

            sb.append("</root>");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
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
            throw new RuntimeException(exception);
        }
    }
}
