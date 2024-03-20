/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.configuration.repository.git.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version ee
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record GitWorkflowRepositoryProperties(
    String branch, String password, String[] searchPaths, String url, String username) {
}
