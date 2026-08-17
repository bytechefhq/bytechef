/**
 * Edition seam for opening an agent deployment's chat channel.
 *
 * CE has no AI Hub, so its chat channel opens the hosted chat page — a standalone URL anyone with the link can
 * use. EE opens a NEW AI Hub conversation against the same deployment instead, which is where an EE user
 * expects to talk to an agent and where the transcript is kept.
 *
 * The default returns null rather than a no-op handler, so the CE surface can tell "no EE implementation" from
 * "EE implementation that did nothing" and render its link. The EE bundle replaces it via
 * {@link registerAgentChatApi} from main.tsx's edition bootstrap, which runs before the first render — so hook
 * identity never changes between renders and the rules of hooks hold.
 */

export interface OpenAgentChatParamsI {
    /** The ProjectDeployment id — an agent deployment IS one. */
    projectDeploymentId: string;
    /** Conversation title, shown in the AI Hub task list. */
    title: string;
    /** The chat trigger's static webhook execution id, which is what a workflow chat is keyed on. */
    workflowExecutionId: string;
}

export interface AgentChatApiI {
    /**
     * @return a handler that opens the deployment's chat, or null when this edition has nowhere to open it —
     *         in which case the caller falls back to the hosted chat link.
     */
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
