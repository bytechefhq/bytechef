import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {AiAgent} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useQueryClient} from '@tanstack/react-query';
import {BotIcon, SparklesIcon} from 'lucide-react';
import {useEffect} from 'react';

import AgentDialog from './components/AgentDialog';
import AgentList from './components/agent-list/AgentList';
import useAgents from './hooks/useAgents';

const Agents = () => {
    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    const {agents, agentsError, agentsIsLoading} = useAgents();

    // No agent is selected on the list page, so the context carries no agentId — the copilot's ai_agent
    // tools list the workspace's agents themselves, which is what makes "create an agent that…" work here.
    const openCopilot = () => {
        setContext({
            mode: MODE.ASK,
            parameters: {},
            source: Source.AI_AGENT,
        });

        setCopilotPanelOpen(true);
    };

    // Refresh the workspace agent list after a BUILD-mode copilot turn creates or deletes an agent, so the
    // list reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.AI_AGENT, () => invalidateAgentQueries(queryClient));
    }, [queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        <div className="flex items-center gap-2">
                            {copilotEnabled && (
                                <Button
                                    aria-label="Ask Copilot"
                                    icon={<SparklesIcon className="size-4" />}
                                    onClick={openCopilot}
                                    size="icon"
                                    variant="ghost"
                                />
                            )}

                            {agents.length > 0 && <AgentDialog triggerNode={<Button label="New Agent" />} />}
                        </div>
                    }
                    title={agents.length > 0 ? 'Agents' : ''}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Agents" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[agentsError]} loading={agentsIsLoading}>
                {agents.length > 0 ? (
                    <AgentList agents={agents as AiAgent[]} />
                ) : (
                    <EmptyList
                        button={<AgentDialog triggerNode={<Button label="Create Agent" />} />}
                        icon={<BotIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new agent."
                        title="No Agents"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Agents;
