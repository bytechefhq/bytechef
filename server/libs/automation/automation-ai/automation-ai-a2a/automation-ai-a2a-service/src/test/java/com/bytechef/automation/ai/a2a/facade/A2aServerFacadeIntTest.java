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

package com.bytechef.automation.ai.a2a.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = A2aIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@A2aIntTestConfigurationSharedMocks
class A2aServerFacadeIntTest {

    @Autowired
    private A2aServerFacade a2aServerFacade;

    @Autowired
    private A2aServerRepository a2aServerRepository;

    @Autowired
    private A2aServerService a2aServerService;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void afterEach() {
        a2aServerRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testUpdateA2aServerTagsCreatesNewTagsAndAssignsThem() {
        A2aServer a2aServer = a2aServerService.create(new A2aServer("agent", null, Environment.DEVELOPMENT));

        Tag salesTag = tagRepository.save(new Tag("sales"));

        List<Tag> tags = a2aServerFacade.updateA2aServerTags(
            a2aServer.getId(), List.of(new Tag(Validate.notNull(salesTag.getId(), "id"), "sales"), new Tag("support")));

        assertThat(tags).extracting(Tag::getName)
            .containsExactlyInAnyOrder("sales", "support");
        assertThat(tags).allSatisfy(tag -> assertThat(tag.getId()).isNotNull());
        assertThat(a2aServerRepository.findById(a2aServer.getId())
            .orElseThrow()
            .getTagIds()).containsExactlyInAnyOrderElementsOf(
                tags.stream()
                    .map(Tag::getId)
                    .toList());
    }

    @Test
    void testUpdateA2aServerTagsWithNoTagsClearsThem() {
        A2aServer a2aServer = a2aServerService.create(new A2aServer("agent", null, Environment.DEVELOPMENT));

        a2aServerFacade.updateA2aServerTags(a2aServer.getId(), List.of(new Tag("sales")));

        assertThat(a2aServerFacade.updateA2aServerTags(a2aServer.getId(), List.of())).isEmpty();
        assertThat(a2aServerRepository.findById(a2aServer.getId())
            .orElseThrow()
            .getTagIds()).isEmpty();
    }

    @Test
    void testGetA2aServerTagsReturnsTheTagsOfEachServerAndAllUsedTags() {
        A2aServer salesA2aServer = a2aServerService.create(new A2aServer("sales", null, Environment.DEVELOPMENT));
        A2aServer supportA2aServer = a2aServerService.create(
            new A2aServer("support", null, Environment.DEVELOPMENT));

        tagRepository.save(new Tag("unused"));

        a2aServerFacade.updateA2aServerTags(salesA2aServer.getId(), List.of(new Tag("sales")));
        a2aServerFacade.updateA2aServerTags(supportA2aServer.getId(), List.of(new Tag("support")));

        List<A2aServer> a2aServers = a2aServerService.getA2aServers();

        Map<A2aServer, List<Tag>> a2aServerTags = a2aServerFacade.getA2aServerTags(a2aServers);

        assertThat(a2aServers).allSatisfy(
            a2aServer -> assertThat(a2aServerTags).hasEntrySatisfying(
                a2aServer, tags -> assertThat(tags).extracting(Tag::getName)
                    .containsExactly(a2aServer.getName())));
        assertThat(a2aServerFacade.getA2aServerTags()).extracting(Tag::getName)
            .containsExactlyInAnyOrder("sales", "support");
    }
}
