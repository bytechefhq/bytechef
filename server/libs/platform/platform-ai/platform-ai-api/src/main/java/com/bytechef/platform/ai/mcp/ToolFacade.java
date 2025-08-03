package com.bytechef.platform.ai.mcp;

import com.bytechef.platform.constant.Environment;

import java.util.List;
import java.util.Map;

/**
 * @author Matija Petanjek
 */
public interface ToolFacade {

    List<ToolDTO> getTools();

    public Object executeTool(
        String toolName, Map<String, Object> inputParameters, Environment environment);

}
