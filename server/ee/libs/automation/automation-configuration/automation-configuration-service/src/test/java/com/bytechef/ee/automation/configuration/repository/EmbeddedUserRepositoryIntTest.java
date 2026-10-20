/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EeAutomationConfigurationIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class EmbeddedUserRepositoryIntTest {

    private static final int PRODUCTION = 2;

    @Autowired
    private EmbeddedUserRepository embeddedUserRepository;

    @Test
    void testAnImplicitRowAndAnExplicitRowCoexistAndAreFoundSeparately() {
        long userId = 9201L;

        embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.EDITOR));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        Optional<EmbeddedUser> implicitRow = embeddedUserRepository.findByUserIdAndEnvironmentIsNull(userId);
        Optional<EmbeddedUser> explicitRow = embeddedUserRepository.findByUserIdAndEnvironment(userId, PRODUCTION);
        List<EmbeddedUser> allRows = embeddedUserRepository.findAllByUserId(userId);

        assertThat(implicitRow).isPresent();
        assertThat(implicitRow.get()
            .getEmbeddedRole()).isEqualTo(WorkspaceRole.EDITOR);
        assertThat(explicitRow).isPresent();
        assertThat(explicitRow.get()
            .getEmbeddedRole()).isEqualTo(WorkspaceRole.VIEWER);
        assertThat(explicitRow.get()
            .getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(allRows).hasSize(2);
        assertThat(embeddedUserRepository.existsByUserId(userId)).isTrue();
        assertThat(embeddedUserRepository.existsByUserIdAndEnvironmentIsNotNull(userId)).isTrue();
    }

    @Test
    void testASecondImplicitRowForTheSameUserIsRejected() {
        long userId = 9202L;

        embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.EDITOR));

        assertThatThrownBy(() -> embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.VIEWER)))
            .satisfiesAnyOf(
                throwable -> assertThat(throwable).isInstanceOf(DuplicateKeyException.class),
                throwable -> assertThat(throwable).isInstanceOf(DbActionExecutionException.class)
                    .hasCauseInstanceOf(DuplicateKeyException.class));
    }

    @Test
    void testASecondRowForTheSameUserAndEnvironmentIsRejected() {
        long userId = 9203L;

        embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.EDITOR, Environment.STAGING));

        assertThatThrownBy(
            () -> embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.VIEWER, Environment.STAGING)))
                .satisfiesAnyOf(
                    throwable -> assertThat(throwable).isInstanceOf(DuplicateKeyException.class),
                    throwable -> assertThat(throwable).isInstanceOf(DbActionExecutionException.class)
                        .hasCauseInstanceOf(DuplicateKeyException.class));
    }

    @Test
    void testDeleteByUserIdRemovesEveryRow() {
        long userId = 9204L;

        embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        embeddedUserRepository.save(EmbeddedUser.forRole(userId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        embeddedUserRepository.deleteByUserId(userId);

        assertThat(embeddedUserRepository.findAllByUserId(userId)).isEmpty();
        assertThat(embeddedUserRepository.existsByUserId(userId)).isFalse();
    }

    @Test
    void testLockEmbeddedUsersLocksOnlyTheMembersOwnRows() {
        long userId = 9205L;

        EmbeddedUser developmentRow = embeddedUserRepository.save(
            EmbeddedUser.forRole(userId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        EmbeddedUser productionRow = embeddedUserRepository.save(
            EmbeddedUser.forRole(userId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        assertThat(embeddedUserRepository.lockEmbeddedUsers(userId))
            .containsExactlyInAnyOrder(developmentRow.getId(), productionRow.getId());
        assertThat(embeddedUserRepository.lockEmbeddedUsers(-1L)).isEmpty();
    }
}
