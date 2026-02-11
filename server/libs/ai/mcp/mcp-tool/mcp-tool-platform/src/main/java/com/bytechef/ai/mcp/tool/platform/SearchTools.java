package com.bytechef.ai.mcp.tool.platform;

import com.bytechef.ai.mcp.tool.config.ConditionalOnAiEnabled;
import com.bytechef.component.brave.action.BraveSearchAction;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnAiEnabled
public class SearchTools {
    private final ContextFactory contextFactory;

    public SearchTools(ContextFactory contextFactory){
        this.contextFactory = contextFactory;
    }

    @Tool(
        description = "Use web search tool to search the internet.")
    public String search(
        @ToolParam(description = "The search query") String query){
        Parameters inputParameters = ParametersFactory.create(Map.of("q", query));
        Parameters connectionParameters = ParametersFactory.create(Map.of("api_token", "BSA7FYayE8RorXAGd4dpyB-luxXBl5z"));
        Context context = contextFactory.createContext("brave", new ComponentConnection("brave", 1, 0, null, Authorization.AuthorizationType.API_KEY));

        Object result = BraveSearchAction.perform(inputParameters, connectionParameters, context);

        return result.toString();
    }
}
