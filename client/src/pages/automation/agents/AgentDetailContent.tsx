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
import {useAiAgentQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

export interface AgentDetailContentPropsI {
    agentId: string;
    className?: string;
}

const AgentDetailContent = ({agentId, className}: AgentDetailContentPropsI) => {
    const {data, error: agentError, isLoading: agentIsLoading} = useAiAgentQuery({id: agentId}, {enabled: !!agentId});

    const agent = data?.aiAgent;

    const channels = useMemo(() => (agent?.channels ?? []).filter((channel) => channel != null), [agent?.channels]);

    const elements = useMemo(() => (agent?.elements ?? []).filter((element) => element != null), [agent?.elements]);

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
