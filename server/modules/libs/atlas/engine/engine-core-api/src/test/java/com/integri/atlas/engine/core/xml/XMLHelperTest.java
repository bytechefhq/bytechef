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

package com.integri.atlas.engine.core.xml;

import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class XMLHelperTest {

    private static final XMLHelper xmlHelper = new XMLHelper();

    @Test
    public void testDeserialize() {
        Assertions
            .assertThat(
                xmlHelper.deserialize(
                    """
        <Flower id="45">
            <name>Poppy</name>
            <color>RED</color>
            <petals>9</petals>
            <Florists>
                 <Florist>
                     <name>Joe</name>
                 </Florist>
                 <Florist>
                     <name>Mark</name>
                 </Florist>
            </Florists>
        </Flower>
        """
                )
            )
            .isEqualTo(
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
            );

        Assertions
            .assertThat(
                xmlHelper.deserialize(
                    """
        <Flowers>
            <Flower id="45">
                <name>Poppy</name>
                <color>RED</color>
                <petals>9</petals>
                <Florists>
                     <Florist>
                         <name>Joe</name>
                     </Florist>
                     <Florist>
                         <name>Mark</name>
                     </Florist>
                </Florists>
            </Flower>
            <Flower id="46"><name>Rose</name><color>YELLOW</color><petals>5</petals></Flower>
        </Flowers>
        """,
                    List.class
                )
            )
            .isEqualTo(
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
            );
    }

    @Test
    public void testSerialize() {
        Assertions
            .assertThat(
                xmlHelper.deserialize(
                    xmlHelper.serialize(
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
                        "Flower"
                    )
                )
            )
            .isEqualTo(
                xmlHelper.deserialize(
                    """
                    <Flower id="45">
                        <name>Poppy</name>
                        <color>RED</color>
                        <petals>9</petals>
                        <Florists>
                             <Florist>
                                 <name>Joe</name>
                             </Florist>
                             <Florist>
                                 <name>Mark</name>
                             </Florist>
                        </Florists>
                    </Flower>
                    """
                )
            );

        Assertions
            .assertThat(
                xmlHelper.deserialize(
                    xmlHelper.serialize(
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
                        ),
                        "Flowers"
                    ),
                    List.class
                )
            )
            .isEqualTo(
                xmlHelper.deserialize(
                    """
                    <Flowers>
                        <Flower id="45">
                            <name>Poppy</name>
                            <color>RED</color>
                            <petals>9</petals>
                            <Florists>
                                 <Florist>
                                     <name>Joe</name>
                                 </Florist>
                                 <Florist>
                                     <name>Mark</name>
                                 </Florist>
                            </Florists>
                        </Flower>
                        <Flower id="46"><name>Rose</name><color>YELLOW</color><petals>5</petals></Flower>
                    </Flowers>
                    """,
                    List.class
                )
            );
    }
}
