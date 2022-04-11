/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine.core.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JSONHelperTest {

    private JSONHelper jsonHelper = new JSONHelper(new ObjectMapper());

    @Test
    public void testDeserialize() {}

    @Test
    public void testSerialize() {
        Assertions
            .assertThat(
                jsonHelper.deserialize(
                    jsonHelper.serialize(
                        Map.of(
                            "name",
                            "Poppy",
                            "color",
                            "RED",
                            "petals",
                            "9",
                            "id",
                            "45",
                            "Florists",
                            Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                        )
                    ),
                    Map.class
                )
            )
            .isEqualTo(
                jsonHelper.deserialize(
                    """
                    {"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"}
                    """,
                    Map.class
                )
            );

        Assertions
            .assertThat(
                jsonHelper.deserialize(
                    jsonHelper.serialize(
                        List.of(
                            Map.of(
                                "name",
                                "Poppy",
                                "color",
                                "RED",
                                "petals",
                                "9",
                                "id",
                                "45",
                                "Florists",
                                Map.of("Florist", List.of(Map.of("name", "Joe"), Map.of("name", "Mark")))
                            ),
                            Map.of("name", "Rose", "color", "YELLOW", "petals", "5", "id", "46")
                        )
                    ),
                    List.class
                )
            )
            .isEqualTo(
                jsonHelper.deserialize(
                    """
                    [{"Florists":{"Florist":[{"name":"Joe"}, {"name":"Mark"}]}, "color":"RED", "id":"45", "name":"Poppy", "petals":"9"},{"color":"YELLOW", "id":"46", "name":"Rose", "petals":"5"}]
                    """,
                    List.class
                )
            );
    }
}
