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

package com.bytechef.component.active.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Property;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
public class ActiveCampaignComponentHandlerTest extends AbstractActiveCampaignComponentHandlerTest {

    @Test
    void testGetDefinitionAddsEachConnectionPropertyOnceAcrossHandlerInstances() {
        new ActiveCampaignComponentHandler();

        ComponentDefinition componentDefinition = new ActiveCampaignComponentHandler().getDefinition();

        ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
            .orElseThrow();

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

        assertThat(authorizations.getFirst()
            .getProperties())
                .extracting(Property::getName)
                .doesNotHaveDuplicates();
    }
}
