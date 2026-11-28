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
import {useAgentExternalChangeStore} from '@/pages/automation/agents/stores/useAgentExternalChangeStore';
import refreshAgentAfterExternalChange from '@/pages/automation/agents/utils/refreshAgentAfterExternalChange';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useAiAgentQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {Fragment, useEffect, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

export interface AgentDetailContentPropsI {
    agentId: string;
    className?: string;
}

const AgentDetailContent = ({agentId, className}: AgentDetailContentPropsI) => {
    const externalChangeRevision = useAgentExternalChangeStore((state) => state.revision);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const {data, error: agentError, isLoading: agentIsLoading} = useAiAgentQuery({id: agentId}, {enabled: !!agentId});

    const queryClient = useQueryClient();

    const agent = data?.aiAgent;

    const channels = useMemo(() => (agent?.channels ?? []).filter((channel) => channel != null), [agent?.channels]);

    const elements = useMemo(() => (agent?.elements ?? []).filter((element) => element != null), [agent?.elements]);

    useEffect(() => {
        return registerPostTurn(Source.AI_AGENT, () => {
            void refreshAgentAfterExternalChange(queryClient);
        });
    }, [queryClient, registerPostTurn]);

    return (
        <PageLoader errors={[agentError]} loading={agentIsLoading}>
            {agent && (
                <Tabs
                    className={twMerge('mx-auto w-full max-w-(--breakpoint-md) self-start p-4', className)}
                    defaultValue="agent"
                    key={agent.id}
                >
                    <TabsList>
                        <TabsTrigger value="agent">Agent</TabsTrigger>

                        <TabsTrigger value="settings">Settings</TabsTrigger>
                    </TabsList>

                    <TabsContent className="flex flex-col gap-6" value="agent">
                        <AgentInstructionsCard
                            agentId={agent.id}
                            externalChangeRevision={externalChangeRevision}
                            instructions={agent.instructions ?? null}
                        />

                        <Fragment key={externalChangeRevision}>
                            <AgentModelCard agentId={agent.id} elements={elements} />

                            <AgentChannelsCard agentId={agent.id} channels={channels} />

                            <AgentToolsCard agentId={agent.id} elements={elements} />

                            <AgentSkillsCard agentId={agent.id} elements={elements} />

                            <AgentKnowledgeBaseCard agentId={agent.id} elements={elements} />

                            <AgentSubAgentsCard agentId={agent.id} elements={elements} />

                            <AgentScheduleCard agentId={agent.id} channels={channels} />
                        </Fragment>
                    </TabsContent>

                    <TabsContent value="settings">
                        <AgentSettingsCard
                            agentId={agent.id}
                            channels={channels}
                            elements={elements}
                            key={externalChangeRevision}
                            settings={agent.settings}
                        />
                    </TabsContent>
                </Tabs>
            )}
        </PageLoader>
    );
};

export default AgentDetailContent;
