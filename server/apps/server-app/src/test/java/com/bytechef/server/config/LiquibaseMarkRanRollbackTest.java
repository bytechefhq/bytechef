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

package com.bytechef.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Guards every Liquibase changelog on the server application's classpath against a combination that silently arms a
 * destructive rollback: a changeset carrying a {@code MARK_RAN} precondition together with a change whose inverse
 * Liquibase generates for it, and no explicit {@code <rollback>} of its own.
 *
 * <p>
 * {@code onFail="MARK_RAN"} exists so a changeset can be recorded as ran without applying anything, typically when the
 * column or table it would create is already there. The changeset therefore cannot know whether it is the one that
 * created the object. Meanwhile Liquibase, finding no declared rollback, auto-generates the inverse of the change,
 * {@code dropColumn} for an {@code addColumn}, {@code dropTable} for a {@code createTable}, and runs it on rollback.
 * Nothing in the XML connects the precondition to that generated inverse, so the hazard is invisible on inspection: a
 * rollback can drop an object, and the data in it, that this changeset never created. The fix is an explicit empty
 * {@code <rollback/>}. A leftover object is inert and re-applying the changeset is a no-op through the same
 * precondition, where the drop is unrecoverable.
 * </p>
 *
 * @author Ivica Cardic
 */
class LiquibaseMarkRanRollbackTest {

    private static final String CHANGELOG_PATTERN = "classpath*:config/liquibase/**/*.xml";

    /**
     * Change types for which Liquibase implements {@code createInverses()} and will therefore roll back on its own when
     * a changeset declares no rollback of its own. Anything absent here (an {@code <sql>}, a {@code dropColumn}, a
     * {@code loadData}) has no generated inverse, so a MARK_RAN changeset built only from those is not exposed.
     */
    private static final Set<String> AUTO_INVERTED_CHANGES = Set.of(
        "addColumn", "addDefaultValue", "addForeignKeyConstraint", "addLookupTable", "addNotNullConstraint",
        "addPrimaryKey", "addUniqueConstraint", "createIndex", "createProcedure", "createSequence", "createTable",
        "createView", "dropNotNullConstraint", "renameColumn", "renameSequence", "renameTable", "renameView");

    // The parser this test builds has DOCTYPE declarations disabled outright and secure processing on, so no
    // external entity can be resolved; SpotBugs cannot see that because the factory is configured in a helper.
    @SuppressFBWarnings("XXE_DOCUMENT")
    @Test
    void testNoMarkRanChangeSetLeavesItsInverseToLiquibase() throws Exception {
        List<Resource> resources = changelogResources();
        List<String> changelogNames = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        int changeSetCount = 0;

        for (Resource resource : resources) {
            String name = name(resource);

            changelogNames.add(name);

            try (InputStream inputStream = resource.getInputStream()) {
                Document document = documentBuilder().parse(inputStream);

                NodeList changeSets = document.getElementsByTagName("changeSet");

                changeSetCount += changeSets.getLength();

                violations.addAll(violations(document, name));
            }
        }

        // A scanner that matched no changelogs, or only the handful in whichever module happens to be built, would
        // stay green forever while the hazard spread. The repository carries a couple of hundred changelogs; these
        // bounds are far enough below that to survive normal churn and far enough above zero to catch a broken scan.
        assertThat(changelogNames)
            .as("the changelog scan for %s found implausibly few files; the scan is broken, not the changelogs",
                CHANGELOG_PATTERN)
            .hasSizeGreaterThan(150);
        assertThat(changeSetCount)
            .as("the changelog scan parsed implausibly few changesets; the parser is matching nothing")
            .isGreaterThan(200);
        assertThat(changelogNames)
            .as("the changelog scan reached no ee changelog directory; the EE modules are not on this classpath and"
                + " their changesets would go unchecked")
            .anyMatch(name -> name.contains("/ai/gateway/") || name.contains("/embedded/"));

        assertThat(violations)
            .as("changesets combining a MARK_RAN precondition with a change Liquibase inverts for them, and no"
                + " explicit rollback — each one can drop an object it never created; add an empty <rollback/>")
            .isEmpty();
    }

    @Test
    void testTheDetectorActuallyFires() throws Exception {
        // Without this, a detector that quietly matched nothing would be indistinguishable from a clean tree.
        assertThat(violations(parse(changeLog("""
                <changeSet id="exposed" author="test">
                    <preConditions onFail="MARK_RAN">
                        <not><columnExists tableName="project" columnName="visibility"/></not>
                    </preConditions>
                    <addColumn tableName="project">
                        <column name="visibility" type="INT"/>
                    </addColumn>
                </changeSet>
            """)), "exposed.xml"))
            .singleElement()
            .asString()
            .contains("exposed")
            .contains("addColumn");

        assertThat(violations(parse(changeLog("""
                <changeSet id="guarded" author="test">
                    <preConditions onFail="MARK_RAN">
                        <not><columnExists tableName="project" columnName="visibility"/></not>
                    </preConditions>
                    <addColumn tableName="project">
                        <column name="visibility" type="INT"/>
                    </addColumn>
                    <rollback/>
                </changeSet>
            """)), "guarded.xml"))
            .as("an explicit empty rollback turns the auto-generated inverse off and must clear the finding")
            .isEmpty();

        assertThat(violations(parse(changeLog("""
                <changeSet id="halt" author="test">
                    <preConditions onFail="HALT">
                        <not><columnExists tableName="project" columnName="visibility"/></not>
                    </preConditions>
                    <addColumn tableName="project">
                        <column name="visibility" type="INT"/>
                    </addColumn>
                </changeSet>
            """)), "halt.xml"))
            .as("a changeset that halts rather than marking itself ran did apply its change, so its inverse is sound")
            .isEmpty();

        assertThat(violations(parse(changeLog("""
                <changeSet id="noInverse" author="test">
                    <preConditions onFail="MARK_RAN">
                        <columnExists tableName="project" columnName="visibility"/>
                    </preConditions>
                    <dropColumn tableName="project" columnName="visibility"/>
                </changeSet>
            """)), "no-inverse.xml"))
            .as("Liquibase generates no inverse for a dropColumn, so nothing is armed")
            .isEmpty();
    }

    private static List<String> violations(Document document, String changelogName) {
        List<String> violations = new ArrayList<>();

        NodeList changeSets = document.getElementsByTagName("changeSet");

        for (int i = 0; i < changeSets.getLength(); i++) {
            Element changeSet = (Element) changeSets.item(i);

            if (!marksRanOnFailedPrecondition(changeSet) || hasExplicitRollback(changeSet)) {
                continue;
            }

            List<String> autoInvertedChanges = autoInvertedChanges(changeSet);

            if (autoInvertedChanges.isEmpty()) {
                continue;
            }

            violations.add(
                "%s: changeset '%s' has a MARK_RAN precondition and %s, whose inverse Liquibase generates and runs on"
                    .formatted(changelogName, changeSet.getAttribute("id"), autoInvertedChanges)
                    + " rollback even though the precondition may have skipped the change; declare an explicit"
                    + " <rollback/>");
        }

        return violations;
    }

    private static boolean marksRanOnFailedPrecondition(Element changeSet) {
        NodeList preConditions = changeSet.getElementsByTagName("preConditions");

        for (int i = 0; i < preConditions.getLength(); i++) {
            Element preCondition = (Element) preConditions.item(i);

            if ("MARK_RAN".equals(preCondition.getAttribute("onFail"))) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasExplicitRollback(Element changeSet) {
        for (Element child : children(changeSet)) {
            if ("rollback".equals(child.getTagName())) {
                return true;
            }
        }

        return false;
    }

    private static List<String> autoInvertedChanges(Element changeSet) {
        List<String> changes = new ArrayList<>();

        for (Element child : children(changeSet)) {
            String tagName = child.getTagName();

            if (AUTO_INVERTED_CHANGES.contains(tagName) && !changes.contains(tagName)) {
                changes.add(tagName);
            }
        }

        return changes;
    }

    private static List<Element> children(Element element) {
        List<Element> children = new ArrayList<>();

        NodeList nodes = element.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node instanceof Element childElement) {
                children.add(childElement);
            }
        }

        return children;
    }

    private static List<Resource> changelogResources() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        return List.of(resolver.getResources(CHANGELOG_PATTERN));
    }

    private static String name(Resource resource) throws IOException {
        String description = resource.getURL()
            .toString();

        int index = description.indexOf("config/liquibase/");

        return index < 0 ? description : description.substring(index);
    }

    private static String changeLog(String changeSets) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<databaseChangeLog xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\">"
            + changeSets
            + "</databaseChangeLog>";
    }

    // See the note on the scanning test above: this parser resolves no external entity.
    @SuppressFBWarnings("XXE_DOCUMENT")
    private static Document parse(String xml) throws IOException, SAXException, ParserConfigurationException {
        return documentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setXIncludeAware(false);

        return documentBuilderFactory.newDocumentBuilder();
    }
}
