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

package com.bytechef.platform.component.visibility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentVisibilityProviderTest {

    @Test
    void testDefaultOperationVisibilityFallsBackToComponentVisibility() {
        ComponentVisibilityProvider visibilityProvider = componentName -> !"disabled".equals(componentName);

        assertThat(visibilityProvider.isActionVisible("disabled", "anyAction")).isFalse();
        assertThat(visibilityProvider.isActionVisible("enabled", "anyAction")).isTrue();
        assertThat(visibilityProvider.isTriggerVisible("disabled", "anyTrigger")).isFalse();
    }
}
