/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectGitFacade {

    void pullProjectFromGit(long projectId);

    void pushProjectToGit(long projectId, String commitMessage);
}
