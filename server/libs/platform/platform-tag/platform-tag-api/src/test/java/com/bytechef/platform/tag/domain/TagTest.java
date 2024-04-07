/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.tag.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TagTest {

    @Test
    void testEquals() {
        Tag tag1 = new Tag();

        tag1.setId(1L);

        Tag tag2 = new Tag();

        tag2.setId(tag1.getId());

        assertThat(tag1).isEqualTo(tag2);

        tag2.setId(2L);

        assertThat(tag1).isNotEqualTo(tag2);

        tag1.setId(null);

        assertThat(tag1).isNotEqualTo(tag2);
    }
}
