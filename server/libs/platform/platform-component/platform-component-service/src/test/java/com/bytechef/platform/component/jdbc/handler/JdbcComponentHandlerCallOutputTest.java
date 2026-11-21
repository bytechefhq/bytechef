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

package com.bytechef.platform.component.jdbc.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Igor Beslic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class JdbcComponentHandlerCallOutputTest {

    @Test
    public void testCallOutputDerivesSchemaFromOutputParameters() {
        OutputResponse outputResponse = JdbcComponentHandlerImpl.callOutput(
            ParametersFactory.create(
                Map.of(
                    JdbcConstants.OUTPUT_PARAMETERS, List.of(
                        Map.of("name", "balance", "type", "FLOAT", "mode", "OUT"),
                        Map.of("name", "count", "type", "INTEGER", "mode", "OUT"),
                        Map.of("name", "note", "type", "VARCHAR", "mode", "IN_OUT"),
                        Map.of("name", "createdAt", "type", "TIMESTAMP", "mode", "OUT")))),
            ParametersFactory.create(Map.of()), mock(ActionContext.class));

        ObjectProperty outputSchema = (ObjectProperty) outputResponse.getOutputSchema();

        assertThat(outputSchema.getProperties())
            .extracting(Property::getName, Property::getType)
            .containsExactly(
                tuple("balance", Property.Type.NUMBER),
                tuple("count", Property.Type.INTEGER),
                tuple("note", Property.Type.STRING),
                tuple("createdAt", Property.Type.DATE_TIME));
    }

    @Test
    public void testCallOutputWithoutOutputParametersIsEmptyObject() {
        OutputResponse outputResponse = JdbcComponentHandlerImpl.callOutput(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), mock(ActionContext.class));

        ObjectProperty outputSchema = (ObjectProperty) outputResponse.getOutputSchema();

        assertThat(outputSchema.getProperties()).isEmpty();
    }
}
