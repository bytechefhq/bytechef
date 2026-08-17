import PageLoader from '@/components/PageLoader';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import AgentChannelsCard from '@/pages/automation/agents/components/detail/AgentChannelsCard';
import AgentInstructionsCard from '@/pages/automation/agents/components/detail/AgentInstructionsCard';
import AgentKnowledgeBaseCard from '@/pages/automation/agents/components/detail/AgentKnowledgeBaseCard';
import AgentModelCard from '@/pages/automation/agents/components/detail/AgentModelCard';
import AgentScheduleCard from '@/pages/automation/agents/components/detail/AgentScheduleCard';
import AgentSettingsCard from '@/pages/automation/agents/components/detail/AgentSettingsCard';
import AgentSkillsCard from '@/pages/automation/agents/components/detail/AgentSkillsCard';
import AgentSubAgentsCard from '@/pages/automation/agents/components/detail/AgentSubAgentsCard';
import AgentToolsCard from '@/pages/automation/agents/components/detail/AgentToolsCard';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useAiAgentQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';

export interface AgentDetailContentPropsI {
    agentId: string;
}

/**
 * The Agent builder section composition, split across two tabs: Agent holds the authoring surface
 * (instructions, model, channels, tools, skills, sub-agents, knowledge base) and Settings holds the per-agent
 * toggles — including the two HITL switches, which are per-agent policy rather than authoring — each editable
 * as its own {@code AiAgentChannel}/{@code AiAgentElement} row via the dedicated sections below. Extracted out of {@code AgentDetail.tsx} so the AI Hub resource-panel tab
 * ({@code AiHubAiAgentViewer.tsx}) can reuse the same composition without the routed page's chrome (header,
 * sidebars, test-chat panel) or its own copy of the `agentId` route param.
 *
 * <p>
 * Owns its own {@code useAiAgentQuery} fetch (keyed by the {@code agentId} prop) rather than taking the
 * fetched agent as a prop, so every consumer gets independent loading/error handling — the react-query
 * cache dedupes the network call against the page's own fetch of the same id (used for the header /
 * test-chat panel), so this is not an extra round trip.
 * </p>
 */
const AgentDetailContent = ({agentId}: AgentDetailContentPropsI) => {
    const queryClient = useQueryClient();

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const {data, error: agentError, isLoading: agentIsLoading} = useAiAgentQuery({id: agentId}, {enabled: !!agentId});

    const agent = data?.aiAgent;

    const channels = useMemo(() => (agent?.channels ?? []).filter((channel) => channel != null), [agent?.channels]);

    const elements = useMemo(() => (agent?.elements ?? []).filter((element) => element != null), [agent?.elements]);

    // Refresh this agent's detail view + the workspace list after a BUILD-mode copilot turn mutates the
    // agent (e.g. adding a channel or element), so the page reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.AI_AGENT, () => invalidateAgentQueries(queryClient));
    }, [queryClient, registerPostTurn]);

    return (
        <PageLoader errors={[agentError]} loading={agentIsLoading}>
            {agent && (
                // Keyed by agent id so navigating between agents remounts every section instead of reusing
                // them: several sections (AgentModelCard in particular) own local draft state that is never
                // resynced from props once mutated, so a warm cache-to-cache navigation without this key
                // would leak the previous agent's in-progress edits into the new agent's sections.
                //
                // `self-start` keeps this from being stretched by LayoutContainer's flex scroll container:
                // stretched, the sections overflow past its padding box and the bottom padding lands above
                // the overflow rather than after it.
                <Tabs
                    className="mx-auto w-full max-w-(--breakpoint-md) self-start p-4"
                    defaultValue="agent"
                    key={agent.id}
                >
                    <TabsList>
                        <TabsTrigger value="agent">Agent</TabsTrigger>

                        <TabsTrigger value="settings">Settings</TabsTrigger>
                    </TabsList>

                    <TabsContent className="flex flex-col gap-6" value="agent">
                        <AgentInstructionsCard agentId={agent.id} instructions={agent.instructions ?? null} />

                        <AgentModelCard agentId={agent.id} elements={elements} />

                        <AgentChannelsCard agentId={agent.id} channels={channels} />

                        <AgentToolsCard agentId={agent.id} elements={elements} />

                        <AgentSkillsCard agentId={agent.id} elements={elements} />

                        <AgentKnowledgeBaseCard agentId={agent.id} elements={elements} />

                        <AgentSubAgentsCard agentId={agent.id} elements={elements} />

                        <AgentScheduleCard agentId={agent.id} channels={channels} />
                    </TabsContent>

                    <TabsContent value="settings">
                        <AgentSettingsCard
                            agentId={agent.id}
                            channels={channels}
                            elements={elements}
                            settings={agent.settings}
                        />
                    </TabsContent>
                </Tabs>
            )}
        </PageLoader>
    );
};

export default AgentDetailContent;
