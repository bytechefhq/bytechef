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

package com.bytechef.platform.workflow.validator;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PropertyUtilsTest {

    private static final PropertyInfo FILE_PROPERTY_INFO = new PropertyInfo(
        "file", "FILE_ENTRY", null, false, false, null, List.of(
            new PropertyInfo("name", "STRING", null, false, false, null, null),
            new PropertyInfo("url", "STRING", null, false, false, null, null)));

    private static final PropertyInfo ITEM_PROPERTY_INFO = new PropertyInfo(
        null, "OBJECT", null, false, false, null, List.of(
            new PropertyInfo("id", "INTEGER", null, false, false, null, null)));

    private static final PropertyInfo OUTPUT_PROPERTY_INFO = new PropertyInfo(
        null, "OBJECT", null, false, false, null, List.of(
            new PropertyInfo("body", "OBJECT", null, false, false, null, null),
            new PropertyInfo("data", "OBJECT", null, false, false, null, List.of(FILE_PROPERTY_INFO)),
            new PropertyInfo("items", "ARRAY", null, false, false, null, List.of(ITEM_PROPERTY_INFO))));

    @Test
    void testGetPropertyReturnsTopLevelProperty() {
        PropertyInfo propertyInfo = PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "body");

        assertThat(propertyInfo).isNotNull();
        assertThat(propertyInfo.name()).isEqualTo("body");
        assertThat(propertyInfo.nestedProperties()).isNull();
    }

    @Test
    void testGetPropertyReturnsNestedProperty() {
        assertThat(PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "data.file")).isSameAs(FILE_PROPERTY_INFO);
    }

    @Test
    void testGetPropertyReturnsDeeplyNestedProperty() {
        PropertyInfo propertyInfo = PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "data.file.url");

        assertThat(propertyInfo).isNotNull();
        assertThat(propertyInfo.name()).isEqualTo("url");
        assertThat(propertyInfo.type()).isEqualTo("STRING");
    }

    @Test
    void testGetPropertyReturnsArrayItem() {
        assertThat(PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "items[0]")).isSameAs(ITEM_PROPERTY_INFO);
    }

    @Test
    void testGetPropertyReturnsArrayItemProperty() {
        PropertyInfo propertyInfo = PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "items[0].id");

        assertThat(propertyInfo).isNotNull();
        assertThat(propertyInfo.type()).isEqualTo("INTEGER");
    }

    @Test
    void testGetPropertyReturnsNullForMissingProperty() {
        assertThat(PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "missing")).isNull();
        assertThat(PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "data.missing")).isNull();
        assertThat(PropertyUtils.getProperty(OUTPUT_PROPERTY_INFO, "body.missing")).isNull();
    }

    @Test
    void testGetPropertyTypeMatchesResolvedProperty() {
        assertThat(PropertyUtils.getPropertyType(OUTPUT_PROPERTY_INFO, "data.file")).isEqualTo("FILE_ENTRY");
        assertThat(PropertyUtils.getPropertyType(OUTPUT_PROPERTY_INFO, "items[0].id")).isEqualTo("INTEGER");
        assertThat(PropertyUtils.getPropertyType(OUTPUT_PROPERTY_INFO, "missing")).isNull();
    }
}
