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

package com.bytechef.hermes.definition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Ivica Cardic
 */
public class DisplayOptionTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    @SuppressWarnings("checkstyle:methodlengthcheck")
    public void testDisplayOption() throws JSONException, JsonProcessingException {
        DisplayOption displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.hideWhen("name").in(true, false)));

        jsonAssertEquals("""
                {"hideWhen":{"name":[true,false]}}
                """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.hideWhen("name").in(1, 2)));

        jsonAssertEquals("""
        {
            "hideWhen":{"name":[1,2]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.hideWhen("name").in(1L, 2L)));

        jsonAssertEquals("""
        {
            "hideWhen":{"name":[1,2]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.hideWhen("name").in(1F, 2F)));

        jsonAssertEquals("""
        {
            "hideWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.hideWhen("name").in(1D, 2D)));

        jsonAssertEquals("""
        {
            "hideWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.hideWhen("name").in("value1", "value2")));

        jsonAssertEquals("""
    {
        "hideWhen":{"name":["value1","value2"]}
    }
    """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.hideWhen("name1").eq(1)));

        jsonAssertEquals("""
        {
         "hideWhen":{"name1":[1]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(List.of(
                DefinitionDSL.hideWhen("name1").eq(1),
                DefinitionDSL.hideWhen("name2").eq(2)));

        jsonAssertEquals("""
    {
        "hideWhen":{"name1":[1],"name2":[2]}
    }
    """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.showWhen("name").in(true, false)));

        jsonAssertEquals("""
    {
        "showWhen":{"name":[true, false]}
    }
    """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.showWhen("name").in(1, 2)));

        jsonAssertEquals("""
        {
            "showWhen":{"name": [1,2]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.showWhen("name").in(1L, 2L)));

        jsonAssertEquals("""
        {
            "showWhen":{"name":[1,2]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.showWhen("name").in(1F, 2F)));

        jsonAssertEquals("""
        {
            "showWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.showWhen("name").in(1D, 2D)));

        jsonAssertEquals("""
        {
            "showWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.showWhen("name").in("value1", "value2")));

        jsonAssertEquals("""
    {
        "showWhen":{"name":["value1","value2"]}
    }
    """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(
                List.of(DefinitionDSL.showWhen("name1").eq(1)));

        jsonAssertEquals("""
        {
         "showWhen":{"name1":[1]}
        }
        """, displayOption);

        displayOption = DefinitionDSL.ModifiableDisplayOption.of(List.of(
                DefinitionDSL.showWhen("name1").eq(1),
                DefinitionDSL.showWhen("name2").eq(2)));

        jsonAssertEquals("""
    {
        "showWhen":{"name1":[1],"name2":[2]}
    }
    """, displayOption);
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
            throws JSONException, JsonProcessingException {
        JSONAssert.assertEquals(expectedString, objectMapper.writeValueAsString(jsonObject), true);
    }
}
