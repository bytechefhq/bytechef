/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.workspace.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Enables Spring Data JDBC repositories for the notification-workspace package, which holds
 * {@code NotificationWorkspaceRepository} - the targeted writer for {@code notification.workspace_id}.
 *
 * <p>
 * Named after that repository rather than a {@code WorkspaceNotification} entity: the {@code workspace_notification}
 * relation table was collapsed into a nullable column on {@code notification}, so no such entity exists.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.notification.workspace.repository")
public class NotificationWorkspaceJdbcRepositoryConfiguration {
}
