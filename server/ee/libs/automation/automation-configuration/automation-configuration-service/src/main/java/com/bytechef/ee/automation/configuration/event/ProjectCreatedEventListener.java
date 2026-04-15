/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import com.bytechef.automation.configuration.event.ProjectCreatedEvent;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Auto-assigns the project creator as project ADMIN after project creation so the creator retains access once
 * {@code @PreAuthorize} checks enforce project scopes. Goes straight to the repository because the project has no
 * admins yet, and the standard {@code addProjectUser} path is itself guarded by {@code PROJECT_MANAGE_USERS}.
 *
 * <p>
 * Failure policy: this listener propagates exceptions (it does not swallow them). If the creator-admin row cannot be
 * persisted, the enclosing project-creation transaction must roll back \u2014 committing the project without an admin
 * row silently orphans the project (nobody can manage it, edit settings, or delete it). Rollback surfaces the problem
 * as an error to the caller, who can retry, while silent success would leave the user staring at a project they cannot
 * access.
 *
 * <p>
 * <b>Load-bearing synchrony.</b> {@link EventListener} is synchronous, so the listener runs on the publishing thread
 * inside the project-creation transaction — exactly what the rollback guarantee requires. DO NOT switch this to
 * {@code @Async} or {@code @TransactionalEventListener(phase = AFTER_COMMIT)} without reconsidering the orphan-project
 * failure mode: either would decouple listener failure from the project-creation commit and silently reintroduce the
 * "created project, no admin" class of bug this listener exists to prevent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectCreatedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCreatedEventListener.class);

    private final ProjectUserRepository projectUserRepository;

    @SuppressFBWarnings("EI")
    public ProjectCreatedEventListener(ProjectUserRepository projectUserRepository) {
        this.projectUserRepository = projectUserRepository;
    }

    @EventListener
    public void onProjectCreated(ProjectCreatedEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug(
                "Assigning user={} as ADMIN of newly created project={}",
                event.creatorUserId(), event.projectId());
        }

        try {
            projectUserRepository.save(
                ProjectUser.forBuiltInRole(event.projectId(), event.creatorUserId(), ProjectRole.ADMIN));
        } catch (RuntimeException exception) {
            // Propagate so the surrounding @Transactional project-creation rolls back. The caller sees an error they
            // can act on (retry, fix, escalate) instead of a committed project with no admin \u2014 which would be
            // invisible to the user (missing access) and impossible to diagnose without DB inspection.
            logger.error(
                "Failed to assign user={} as ADMIN of newly created project={}. Rolling back project creation "
                    + "to avoid an orphan project that nobody can manage.",
                event.creatorUserId(), event.projectId(), exception);

            throw exception;
        }
    }
}
