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

package com.bytechef.platform.owner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.constant.OwnerType;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class OwnerTest {

    @Test
    void testConnectedUserBuildsAConnectedUserOwner() {
        Owner owner = Owner.connectedUser(1055L);

        assertEquals(OwnerType.CONNECTED_USER, owner.type());
        assertEquals(1055L, owner.id());
    }
}
