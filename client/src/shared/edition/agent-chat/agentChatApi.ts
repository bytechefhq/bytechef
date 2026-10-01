export interface OpenAgentChatParamsI {
    projectDeploymentId: string;
    title: string;
    workflowExecutionId: string;
}

export interface AgentChatApiI {
    useOpenAgentChat: () => ((params: OpenAgentChatParamsI) => void) | null;
}

const noopAgentChatApi: AgentChatApiI = {
    useOpenAgentChat: () => null,
};

let agentChatApi: AgentChatApiI = noopAgentChatApi;

export function registerAgentChatApi(api: AgentChatApiI) {
    agentChatApi = api;
}

export function getAgentChatApi(): AgentChatApiI {
    return agentChatApi;
}
