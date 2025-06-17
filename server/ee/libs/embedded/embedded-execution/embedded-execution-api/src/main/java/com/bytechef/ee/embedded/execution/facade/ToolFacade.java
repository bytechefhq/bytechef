/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.ee.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolFacade {

    List<ToolDTO> getTools();

    Object executeTool(
        String externalUserId, String toolName, Map<String, Object> inputParameters, @Nullable Long instanceId,
        Environment environment);

    Map<String, List<ToolDTO>> getTools(
        String externalUserId, List<String> categoryNames, List<String> componentNames,
        List<String> clusterElementNames, Environment environment);
}
