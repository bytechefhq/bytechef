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

package com.bytechef.automation.configuration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectTest {

    @Test
    void testSetTagsWithEmptyListClearsExistingTags() {
        Project project = new Project();

        project.setTags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        assertThat(project.getTagIds()).hasSize(2);

        project.setTags(List.of());

        assertThat(project.getTagIds()).isEmpty();
    }

    @Test
    void testSetTagsWithNullClearsExistingTags() {
        Project project = new Project();

        project.setTags(List.of(new Tag(1L, "tag1")));

        assertThat(project.getTagIds()).hasSize(1);

        project.setTags(null);

        assertThat(project.getTagIds()).isEmpty();
    }
}
