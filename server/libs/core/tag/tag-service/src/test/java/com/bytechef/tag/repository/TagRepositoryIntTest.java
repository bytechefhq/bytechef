
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

package com.bytechef.tag.repository;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.tag.config.TagIntTestConfiguration;
import com.bytechef.tag.domain.Tag;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = TagIntTestConfiguration.class)
public class TagRepositoryIntTest {

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        tagRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testCreate() {
        Tag tag = tagRepository.save(new Tag("name"));

        assertThat(tag).isEqualTo(tagRepository.findById(tag.getId())
            .get());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Tag tag = tagRepository.save(new Tag("name"));

        Tag resultTag = OptionalUtils.get(tagRepository.findById(tag.getId()));

        assertThat(resultTag).isEqualTo(tag);

        tagRepository.deleteById(resultTag.getId());

        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testFindById() {
        Tag tag = tagRepository.save(new Tag("name"));

        assertThat(tagRepository.findById(tag.getId())).hasValue(tag);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Tag tag = tagRepository.save(new Tag("name"));

        assertThat(tagRepository.findById(tag.getId())).hasValue(tag);

        tag.setName("name2");

        tag = tagRepository.save(tag);

        assertThat(tagRepository.findById(tag.getId())).hasValue(tag);
    }
}
