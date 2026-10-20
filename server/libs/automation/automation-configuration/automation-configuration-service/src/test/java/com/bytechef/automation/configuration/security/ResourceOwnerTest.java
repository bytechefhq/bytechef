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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ResourceOwnerTest {

    @Test
    void testAnEmbeddedOwnerWithAWorkspaceIsRejected() {
        assertThatThrownBy(() -> new ResourceOwner(OptionalLong.of(42L), OptionalLong.empty(), true))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAnEmbeddedProjectOwnerThatIsNotEmbeddedIsRejected() {
        assertThatThrownBy(() -> new ResourceOwner(OptionalLong.empty(), OptionalLong.empty(), false, true))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAnEmbeddedProjectOwnerIsEmbeddedAndKnown() {
        ResourceOwner resourceOwner = ResourceOwner.ofEmbeddedProject();

        assertThat(resourceOwner.embedded()).isTrue();
        assertThat(resourceOwner.embeddedProject()).isTrue();
        assertThat(resourceOwner.isUnknown()).isFalse();
        assertThat(ResourceOwner.ofEmbedded()
            .embeddedProject()).isFalse();
    }

    @Test
    void testEmbeddedFactoriesAreNotUnknown() {
        assertThat(ResourceOwner.ofEmbedded()
            .isUnknown()).isFalse();
        assertThat(ResourceOwner.ofEmbedded(7L)
            .ownerUserId()).hasValue(7L);
        assertThat(ResourceOwner.unknown()
            .isUnknown()).isTrue();
    }
}
