/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectGitFacade {

    List<String> getRemoteBranches(long projectId);

    void pullProjectFromGit(long projectId);

    String pushProjectToGit(long projectId, String commitMessage);
}
