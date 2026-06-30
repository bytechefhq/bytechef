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

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ResourceOwnerTest {

    @Test
    void testUnknownIsEmpty() {
        ResourceOwner owner = ResourceOwner.unknown();

        assertThat(owner.workspaceId()).isEmpty();
        assertThat(owner.ownerUserId()).isEmpty();
    }

    @Test
    void testOfWorkspaceSetsOnlyWorkspace() {
        ResourceOwner owner = ResourceOwner.ofWorkspace(42L);

        assertThat(owner.workspaceId()).hasValue(42L);
        assertThat(owner.ownerUserId()).isEmpty();
    }

    @Test
    void testOfUserSetsOnlyOwner() {
        ResourceOwner owner = ResourceOwner.ofUser(7L);

        assertThat(owner.ownerUserId()).hasValue(7L);
        assertThat(owner.workspaceId()).isEmpty();
    }

    @Test
    void testOfBothSetsBoth() {
        ResourceOwner owner = ResourceOwner.of(OptionalLong.of(42L), OptionalLong.of(7L));

        assertThat(owner.workspaceId()).hasValue(42L);
        assertThat(owner.ownerUserId()).hasValue(7L);
    }
}
