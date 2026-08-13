import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import AgentDeploymentListItem from '@/pages/automation/agent-deployments/components/AgentDeploymentListItem';
import useAgentDeployments from '@/pages/automation/agent-deployments/hooks/useAgentDeployments';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {AiAgent} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {BotMessageSquareIcon, SparklesIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

interface NewAgentDeploymentControlProps {
    onDeployClick: () => void;
    onSelectedAgentIdChange: (agentId: string) => void;
    publishedAgents: AiAgent[];
    selectedAgentId?: string;
}

const NewAgentDeploymentControl = ({
    onDeployClick,
    onSelectedAgentIdChange,
    publishedAgents,
    selectedAgentId,
}: NewAgentDeploymentControlProps) => (
    <div className="flex items-center gap-2">
        <Select onValueChange={onSelectedAgentIdChange} value={selectedAgentId}>
            <SelectTrigger aria-label="Select an agent to deploy" className="w-56">
                <SelectValue placeholder="Select an agent" />
            </SelectTrigger>

            <SelectContent>
                {publishedAgents.map((agent) => (
                    <SelectItem key={agent.id} value={agent.id}>
                        {agent.title}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>

        <Button disabled={!selectedAgentId} label="New Deployment" onClick={onDeployClick} />
    </div>
);

const AgentDeployments = () => {
    const [selectedAgentId, setSelectedAgentId] = useState<string | undefined>();
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    const [searchParams] = useSearchParams();

    const agentIdFilter = searchParams.get('agentId');

    const {agentDeployments, agentDeploymentsError, agentDeploymentsIsLoading} = useAgentDeployments();
    const {agents, agentsError, agentsIsLoading} = useAgents();

    // Behind an agent picker per the brief: only published agents (lastPublishedVersion > 0) have a workflow a
    // ProjectDeployment can reference.
    const publishedAgents = useMemo(
        () => agents.filter((agent) => agent.lastPublishedVersion > 0) as AiAgent[],
        [agents]
    );

    const filteredDeployments = useMemo(
        () =>
            agentIdFilter
                ? agentDeployments.filter((deployment) => deployment.agentId === agentIdFilter)
                : agentDeployments,
        [agentDeployments, agentIdFilter]
    );

    const filterAgentTitle = agentIdFilter ? agents.find((agent) => agent.id === agentIdFilter)?.title : undefined;

    const selectedAgent = publishedAgents.find((agent) => agent.id === selectedAgentId);

    const handleCreateDialogClose = () => {
        setShowCreateDialog(false);
        setSelectedAgentId(undefined);
    };

    // The agentId filter is the only page state worth carrying: it tells the copilot which agent's
    // deployments the user is currently looking at. Everything else it discovers via listProjectDeployments.
    const openCopilot = () => {
        setContext({
            mode: MODE.ASK,
            parameters: {agentId: agentIdFilter ?? undefined},
            source: Source.DEPLOYMENT,
        });

        setCopilotPanelOpen(true);
    };

    // Refresh the deployment list after a BUILD-mode copilot turn deploys, toggles, rolls back or deletes a
    // deployment, so the page reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.DEPLOYMENT, () => {
            queryClient.invalidateQueries({queryKey: ['aiAgentDeployments']});
            queryClient.invalidateQueries({queryKey: ['projectDeployments']});
        });
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

                            {filteredDeployments.length > 0 && publishedAgents.length > 0 && (
                                <NewAgentDeploymentControl
                                    onDeployClick={() => setShowCreateDialog(true)}
                                    onSelectedAgentIdChange={setSelectedAgentId}
                                    publishedAgents={publishedAgents}
                                    selectedAgentId={selectedAgentId}
                                />
                            )}
                        </div>
                    }
                    title={
                        filteredDeployments.length > 0
                            ? filterAgentTitle
                                ? `Agent Deployments — ${filterAgentTitle}`
                                : 'Agent Deployments'
                            : ''
                    }
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Agent Deployments" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[agentDeploymentsError, agentsError]}
                loading={agentDeploymentsIsLoading || agentsIsLoading}
            >
                {filteredDeployments.length > 0 ? (
                    <div className="w-full px-4 py-4 3xl:mx-auto 3xl:w-4/5">
                        {filteredDeployments.map((deployment) => (
                            <AgentDeploymentListItem deployment={deployment} key={deployment.id} />
                        ))}
                    </div>
                ) : (
                    <EmptyList
                        button={
                            publishedAgents.length > 0 ? (
                                <NewAgentDeploymentControl
                                    onDeployClick={() => setShowCreateDialog(true)}
                                    onSelectedAgentIdChange={setSelectedAgentId}
                                    publishedAgents={publishedAgents}
                                    selectedAgentId={selectedAgentId}
                                />
                            ) : undefined
                        }
                        icon={<BotMessageSquareIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message={
                            publishedAgents.length > 0
                                ? 'Get started by deploying a published agent.'
                                : 'Publish an agent first, then deploy it here.'
                        }
                        title="No Agent Deployments"
                    />
                )}
            </PageLoader>

            {showCreateDialog && selectedAgent && (
                <ProjectDeploymentDialog
                    onClose={handleCreateDialogClose}
                    projectDeployment={
                        {
                            environmentId: currentEnvironmentId,
                            projectId: +selectedAgent.projectId,
                            projectVersion: selectedAgent.lastPublishedVersion,
                        } as ProjectDeployment
                    }
                    redirectOnSubmit={false}
                />
            )}
        </LayoutContainer>
    );
};

export default AgentDeployments;
