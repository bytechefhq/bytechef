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

package com.bytechef.ai.mcp.tool.platform.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.tool.platform.util.ToolUtils.PropertyDecorator;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.BooleanProperty;
import com.bytechef.platform.component.domain.IntegerProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.StringProperty;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolUtilsTest {

    @Test
    void testListDisplayConditionsEmptyGet() {
        List<PropertyDecorator> properties = List.of();

        Map<String, List<String>> result = ToolUtils.getDisplayConditions(properties);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDisplayConditionsSimpleProperties() {
        StringProperty property1 = mock(StringProperty.class);

        when(property1.getName()).thenReturn("prop1");
        when(property1.getDisplayCondition()).thenReturn("condition1");

        StringProperty property2 = mock(StringProperty.class);

        when(property2.getName()).thenReturn("prop2");
        when(property2.getDisplayCondition()).thenReturn("condition2");

        StringProperty property3 = mock(StringProperty.class);

        when(property3.getName()).thenReturn("prop3");
        when(property3.getDisplayCondition()).thenReturn("condition1");

        List<PropertyDecorator> properties = List.of(
            new PropertyDecorator(property1), new PropertyDecorator(property2), new PropertyDecorator(property3));

        Map<String, List<String>> result = ToolUtils.getDisplayConditions(properties);

        assertEquals(2, result.size());
        assertEquals(List.of("prop1", "prop3"), result.get("condition1"));
        assertEquals(List.of("prop2"), result.get("condition2"));
    }

    @Test
    void testGetDisplayConditionsPropertiesWithoutDisplayCondition() {
        StringProperty property1 = mock(StringProperty.class);

        when(property1.getName()).thenReturn("prop1");
        when(property1.getDisplayCondition()).thenReturn(null);

        StringProperty property2 = mock(StringProperty.class);

        when(property2.getName()).thenReturn("prop2");
        when(property2.getDisplayCondition()).thenReturn("");

        StringProperty property3 = mock(StringProperty.class);

        when(property3.getName()).thenReturn("prop3");
        when(property3.getDisplayCondition()).thenReturn("  ");

        List<PropertyDecorator> properties = List.of(
            new PropertyDecorator(property1), new PropertyDecorator(property2), new PropertyDecorator(property3));

        Map<String, List<String>> result = ToolUtils.getDisplayConditions(properties);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetDisplayConditionsNestedObjectProperties() {
        StringProperty nestedProp1 = mock(StringProperty.class);

        when(nestedProp1.getName()).thenReturn("nestedProp1");
        when(nestedProp1.getDisplayCondition()).thenReturn("nestedCondition1");

        StringProperty nestedProp2 = mock(StringProperty.class);

        when(nestedProp2.getName()).thenReturn("nestedProp2");
        when(nestedProp2.getDisplayCondition()).thenReturn("nestedCondition2");

        ObjectProperty objectProperty = mock(ObjectProperty.class);

        when(objectProperty.getName()).thenReturn("parentObject");
        when(objectProperty.getDisplayCondition()).thenReturn("parentCondition");
        when(objectProperty.getProperties()).thenReturn((List) List.of(nestedProp1, nestedProp2));

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(objectProperty));

        Map<String, List<String>> result = ToolUtils.getDisplayConditions(properties);

        assertEquals(3, result.size());
        assertEquals(List.of("parentObject"), result.get("parentCondition"));
        assertEquals(List.of("parentObject.nestedProp1"), result.get("nestedCondition1"));
        assertEquals(List.of("parentObject.nestedProp2"), result.get("nestedCondition2"));
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetDisplayConditionsNestedArrayProperties() {
        StringProperty arrayItemProp = mock(StringProperty.class);

        when(arrayItemProp.getName()).thenReturn("arrayItem");
        when(arrayItemProp.getDisplayCondition()).thenReturn("arrayItemCondition");

        ArrayProperty arrayProperty = mock(ArrayProperty.class);

        when(arrayProperty.getName()).thenReturn("arrayProp");
        when(arrayProperty.getDisplayCondition()).thenReturn("arrayCondition");
        when(arrayProperty.getItems()).thenReturn((List) List.of(arrayItemProp));

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(arrayProperty));

        Map<String, List<String>> result = ToolUtils.getDisplayConditions(properties);

        assertEquals(2, result.size());
        assertEquals(List.of("arrayProp"), result.get("arrayCondition"));
        assertEquals(List.of("arrayProp.arrayItem"), result.get("arrayItemCondition"));
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetDisplayConditionsDeepNesting() {
        StringProperty deepNestedProp = mock(StringProperty.class);

        when(deepNestedProp.getName()).thenReturn("deepProp");
        when(deepNestedProp.getDisplayCondition()).thenReturn("deepCondition");

        ObjectProperty level2Object = mock(ObjectProperty.class);

        when(level2Object.getName()).thenReturn("level2");
        when(level2Object.getDisplayCondition()).thenReturn("level2Condition");
        when(level2Object.getProperties()).thenReturn((List) List.of(deepNestedProp));

        ArrayProperty level1Array = mock(ArrayProperty.class);

        when(level1Array.getName()).thenReturn("level1");
        when(level1Array.getDisplayCondition()).thenReturn("level1Condition");
        when(level1Array.getItems()).thenReturn((List) List.of(level2Object));

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(level1Array));

        Map<String, List<String>> result = ToolUtils.getDisplayConditions(properties);

        assertEquals(3, result.size());
        assertEquals(List.of("level1"), result.get("level1Condition"));
        assertEquals(List.of("level1.level2"), result.get("level2Condition"));
        assertEquals(List.of("level1.level2.deepProp"), result.get("deepCondition"));
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGetDisplayConditionsMixedConditions() {
        StringProperty prop1 = mock(StringProperty.class);

        when(prop1.getName()).thenReturn("prop1");
        when(prop1.getDisplayCondition()).thenReturn("sharedCondition");

        StringProperty nestedProp = mock(StringProperty.class);

        when(nestedProp.getName()).thenReturn("nested");
        when(nestedProp.getDisplayCondition()).thenReturn("sharedCondition");

        ObjectProperty objectProp = mock(ObjectProperty.class);

        when(objectProp.getName()).thenReturn("object");
        when(objectProp.getDisplayCondition()).thenReturn("uniqueCondition");
        when(objectProp.getProperties()).thenReturn((List) List.of(nestedProp));

        List<PropertyDecorator> properties = List.of(
            new PropertyDecorator(prop1),
            new PropertyDecorator(objectProp));

        Map<String, List<String>> result = ToolUtils.getDisplayConditions(properties);

        assertEquals(2, result.size());
        assertEquals(List.of("prop1", "object.nested"), result.get("sharedCondition"));
        assertEquals(List.of("object"), result.get("uniqueCondition"));
    }

    @Test
    void testGenerateObjectValueEmptyProperties() {
        List<PropertyDecorator> properties = List.of();

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        assertEquals("{ \"metadata\": \"\" }", result);
    }

    @Test
    void testGenerateObjectValueSingleStringProperty() {
        StringProperty stringProp = mock(StringProperty.class);

        when(stringProp.getName()).thenReturn("testString");
        when(stringProp.getRequired()).thenReturn(false);

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(stringProp));

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        assertEquals("{ \"metadata\": \"\", \"testString\": \"string\" }", result);
    }

    @Test
    void testGenerateObjectValueRequiredStringProperty() {
        StringProperty stringProp = mock(StringProperty.class);

        when(stringProp.getName()).thenReturn("requiredString");
        when(stringProp.getRequired()).thenReturn(true);

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(stringProp));

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        assertEquals("{ \"metadata\": \"\", \"requiredString\": \"string (required)\" }", result);
    }

    @Test
    void testGenerateObjectValueMultipleSimpleProperties() {
        StringProperty stringProp = mock(StringProperty.class);

        when(stringProp.getName()).thenReturn("name");
        when(stringProp.getRequired()).thenReturn(true);

        BooleanProperty boolProp = mock(BooleanProperty.class);

        when(boolProp.getName()).thenReturn("active");
        when(boolProp.getRequired()).thenReturn(false);

        IntegerProperty intProp = mock(IntegerProperty.class);

        when(intProp.getName()).thenReturn("count");
        when(intProp.getRequired()).thenReturn(true);

        List<PropertyDecorator> properties = List.of(
            new PropertyDecorator(stringProp), new PropertyDecorator(boolProp), new PropertyDecorator(intProp));

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        assertEquals(
            "{ \"metadata\": \"\", \"name\": \"string (required)\", \"active\": \"boolean\", \"count\": " +
                "\"integer (required)\" }",
            result);
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGenerateObjectValueNestedObject() {
        StringProperty nestedProp = mock(StringProperty.class);

        when(nestedProp.getName()).thenReturn("nestedField");
        when(nestedProp.getRequired()).thenReturn(true);

        ObjectProperty objectProp = mock(ObjectProperty.class);

        when(objectProp.getName()).thenReturn("nested");
        when(objectProp.getRequired()).thenReturn(false);
        when(objectProp.getProperties()).thenReturn((List) List.of(nestedProp));

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(objectProp));

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        assertEquals(
            "{ \"metadata\": \"\", \"nested\": { \"metadata\": \"\", \"nestedField\": \"string (required)\" } }",
            result);
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGenerateObjectValueArrayProperty() {
        StringProperty arrayItemProp = mock(StringProperty.class);

        when(arrayItemProp.getName()).thenReturn("item");
        when(arrayItemProp.getRequired()).thenReturn(true);

        ArrayProperty arrayProp = mock(ArrayProperty.class);

        when(arrayProp.getName()).thenReturn("items");
        when(arrayProp.getRequired()).thenReturn(false);
        when(arrayProp.getItems()).thenReturn((List) List.of(arrayItemProp));

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(arrayProp));

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        assertEquals("{ \"metadata\": \"\", \"items\": [ \"string (required)\" ] }", result);
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGenerateObjectValueConditionalBodyContent() {
        StringProperty bodyContentType = mock(StringProperty.class);

        when(bodyContentType.getName()).thenReturn("bodyContentType");
        when(bodyContentType.getRequired()).thenReturn(false);

        StringProperty extension = mock(StringProperty.class);

        when(extension.getName()).thenReturn("extension");
        when(extension.getRequired()).thenReturn(true);

        StringProperty mimeType = mock(StringProperty.class);

        when(mimeType.getName()).thenReturn("mimeType");
        when(mimeType.getRequired()).thenReturn(true);

        StringProperty name = mock(StringProperty.class);

        when(name.getName()).thenReturn("name");
        when(name.getRequired()).thenReturn(true);

        StringProperty url = mock(StringProperty.class);

        when(url.getName()).thenReturn("url");
        when(url.getRequired()).thenReturn(true);

        ObjectProperty bodyContentTrue = mock(ObjectProperty.class);

        when(bodyContentTrue.getName()).thenReturn("bodyContent");
        when(bodyContentTrue.getDisplayCondition()).thenReturn("bodyContentType == true");
        when(bodyContentTrue.getRequired()).thenReturn(false);
        when(bodyContentTrue.getProperties()).thenReturn((List) List.of(extension, mimeType, name, url));

        ObjectProperty bodyContentFalse = mock(ObjectProperty.class);

        when(bodyContentFalse.getName()).thenReturn("bodyContent");
        when(bodyContentFalse.getDisplayCondition()).thenReturn("bodyContentType == false");
        when(bodyContentFalse.getRequired()).thenReturn(false);
        when(bodyContentFalse.getProperties()).thenReturn(List.of());

        ObjectProperty body = mock(ObjectProperty.class);

        when(body.getName()).thenReturn("body");
        when(body.getRequired()).thenReturn(false);
        when(body.getProperties()).thenReturn((List) List.of(bodyContentType, bodyContentTrue, bodyContentFalse));

        List<PropertyDecorator> properties = List.of(new PropertyDecorator(body));

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        String expected =
            "{ \"metadata\": \"\", \"body\": { \"metadata\": \"\", \"bodyContentType\": \"string\", \"bodyContent\": " +
                "{ \"metadata\": \"@bodyContentType == true@\", \"extension\": \"string (required)\", \"mimeType\":" +
                " \"string (required)\", \"name\": \"string (required)\", \"url\": \"string (required)\" }, " +
                "\"bodyContent\": { \"metadata\": \"@bodyContentType == false@\" } } }";

        assertEquals(expected, result);

        Map<String, List<String>> displayConditions = ToolUtils.getDisplayConditions(
            List.of(new PropertyDecorator(body)));

        assertEquals(2, displayConditions.size());
        assertEquals(List.of("body.bodyContent"), displayConditions.get("bodyContentType == true"));
        assertEquals(List.of("body.bodyContent"), displayConditions.get("bodyContentType == false"));
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testGenerateObjectValueComplexNestedStructure() {
        StringProperty deepProperty = mock(StringProperty.class);

        when(deepProperty.getName()).thenReturn("deepProperty");
        when(deepProperty.getRequired()).thenReturn(true);

        ObjectProperty level2 = mock(ObjectProperty.class);

        when(level2.getName()).thenReturn("level2");
        when(level2.getRequired()).thenReturn(false);
        when(level2.getProperties()).thenReturn((List) List.of(deepProperty));

        ArrayProperty level1Array = mock(ArrayProperty.class);

        when(level1Array.getName()).thenReturn("level1Array");
        when(level1Array.getRequired()).thenReturn(true);
        when(level1Array.getItems()).thenReturn((List) List.of(level2));

        StringProperty siblingProp = mock(StringProperty.class);

        when(siblingProp.getName()).thenReturn("sibling");
        when(siblingProp.getRequired()).thenReturn(false);

        List<PropertyDecorator> properties = List.of(
            new PropertyDecorator(level1Array), new PropertyDecorator(siblingProp));

        String result = ToolUtils.generateObjectValue(properties, "", "\"");

        assertEquals(
            "{ \"metadata\": \"\", \"level1Array\": [ { \"metadata\": \"\", \"deepProperty\":" +
                " \"string (required)\" } ], \"sibling\": \"string\" }",
            result);
    }

}
