/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectGitFacade {

    void pullProjectFromGit(long projectId);

    String pushProjectToGit(long projectId, @NonNull String commitMessage);
}
