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
        DefinitionDSL.ModifiableDisplayOption.DisplayOptionCondition displayOptionCondition =
                DefinitionDSL.hide("name", List.of(true, false));

        jsonAssertEquals(
                """
                {"conditions":{"name":[true,false]}}
                """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.hide("name", List.of(1, 2));

        jsonAssertEquals(
                """
        {
            "conditions":{"name":[1,2]}
        }
        """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.hide("name", List.of(1L, 2L));

        jsonAssertEquals(
                """
        {
            "conditions":{"name":[1,2]}
        }
        """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.hide("name", List.of(1F, 2F));

        jsonAssertEquals(
                """
        {
            "conditions":{"name":[1.0,2.0]}
        }
        """,
                displayOptionCondition);

        displayOptionCondition = DefinitionDSL.hide("name", List.of(1D, 2D));

        jsonAssertEquals(
                """
        {
            "conditions":{"name":[1.0,2.0]}
        }
        """,
                displayOptionCondition);

        displayOptionCondition = DefinitionDSL.hide("name", List.of("value1", "value2"));

        jsonAssertEquals(
                """
    {
        "conditions":{"name":["value1","value2"]}
    }
    """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.hide("name1", List.of(1));

        jsonAssertEquals(
                """
        {
         "conditions":{"name1":[1]}
        }
        """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.hide("name1", List.of(1), "name2", List.of(2));

        jsonAssertEquals(
                """
    {
        "conditions":{"name1":[1],"name2":[2]}
    }
    """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name", List.of(true, false));

        jsonAssertEquals("""
    {
        "conditions":{"name":[true, false]}
    }
    """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name", List.of(1, 2));

        jsonAssertEquals(
                """
        {
            "conditions":{"name": [1,2]}
        }
        """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name", List.of(1L, 2L));

        jsonAssertEquals(
                """
        {
            "conditions":{"name":[1,2]}
        }
        """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name", List.of(1F, 2F));

        jsonAssertEquals(
                """
        {
            "conditions":{"name":[1.0,2.0]}
        }
        """,
                displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name", List.of(1D, 2D));

        jsonAssertEquals(
                """
        {
            "conditions":{"name":[1.0,2.0]}
        }
        """,
                displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name", List.of("value1", "value2"));

        jsonAssertEquals(
                """
    {
        "conditions":{"name":["value1","value2"]}
    }
    """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name1", List.of(1));

        jsonAssertEquals(
                """
        {
         "conditions":{"name1":[1]}
        }
        """, displayOptionCondition);

        displayOptionCondition = DefinitionDSL.show("name1", List.of(1), "name2", List.of(2));

        jsonAssertEquals(
                """
    {
        "conditions":{"name1":[1],"name2":[2]}
    }
    """, displayOptionCondition);
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
            throws JSONException, JsonProcessingException {
        JSONAssert.assertEquals(expectedString, objectMapper.writeValueAsString(jsonObject), true);
    }
}
