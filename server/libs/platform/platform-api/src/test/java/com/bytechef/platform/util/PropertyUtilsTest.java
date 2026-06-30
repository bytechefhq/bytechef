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

package com.bytechef.platform.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Property;
import com.bytechef.definition.BaseProperty;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the dotted-path traversal used by the AI Hub lookup-options precondition gate.
 *
 * @author Ivica Cardic
 */
public class PropertyUtilsTest {

    @Test
    public void testReturnsTopLevelPropertyMatchByName() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "topLevel");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo("topLevel");
    }

    @Test
    public void testResolvesDottedPathThroughObjectProperty() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "parent.child");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo("child");
    }

    @Test
    public void testResolvesExplicitArrayDescentWithBracketSuffix() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "items[].id");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo("id");
    }

    @Test
    public void testResolvesImplicitArrayDescentWhenArrayHasSingleObjectItem() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "items.id");

        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo("id");
    }

    @Test
    public void testReturnsNullForUnknownIntermediateSegment() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "parent.missing.child");

        assertThat(resolved).isNull();
    }

    @Test
    public void testReturnsNullForUnknownFinalSegment() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "parent.missing");

        assertThat(resolved).isNull();
    }

    @Test
    public void testReturnsNullForUnknownTopLevelSegment() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "nope");

        assertThat(resolved).isNull();
    }

    @Test
    public void testReturnsNullForNullPath() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), null);

        assertThat(resolved).isNull();
    }

    @Test
    public void testReturnsNullForBlankPath() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "   ");

        assertThat(resolved).isNull();
    }

    @Test
    public void testReturnsNullWhenDescendingIntoLeafProperty() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "topLevel.child");

        assertThat(resolved).isNull();
    }

    @Test
    public void testReturnsArrayItemTypeWhenPathEndsInBracketSuffix() {
        BaseProperty resolved = PropertyUtils.findPropertyByPath(buildProperties(), "items[]");

        assertThat(resolved).isNotNull();
        // path ends in `[]` → caller wants the item type itself, which is an anonymous object (name `null`)
        assertThat(resolved).isInstanceOf(Property.ObjectProperty.class);
    }

    private static List<Property> buildProperties() {
        return List.of(
            string("topLevel"),
            object("parent")
                .properties(
                    string("child"),
                    integer("count")),
            array("items")
                .items(
                    object()
                        .properties(
                            string("id"),
                            string("sheetId"))));
    }
}
