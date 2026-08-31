import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import AgentDeploymentListItem from '@/pages/automation/agent-deployments/components/AgentDeploymentListItem';
import AgentDeploymentsLeftSidebarNav from '@/pages/automation/agent-deployments/components/AgentDeploymentsLeftSidebarNav';
import useAgentDeployments from '@/pages/automation/agent-deployments/hooks/useAgentDeployments';
import AgentsFilterTitle from '@/pages/automation/agents/components/AgentsFilterTitle';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {AiAgent, useAiAgentDeploymentTagsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {BotMessageSquareIcon, SparklesIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

const AgentDeployments = () => {
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    const [searchParams] = useSearchParams();

    const agentIdFilter = searchParams.get('agentId');
    const tagIdFilter = searchParams.get('tagId');

    const {agentDeployments, agentDeploymentsError, agentDeploymentsIsLoading} = useAgentDeployments();
    const {agents, agentsError, agentsIsLoading} = useAgents();

    // Behind an agent picker per the brief: only published agents (lastPublishedVersion > 0) have a workflow a
    // ProjectDeployment can reference.
    const publishedAgents = useMemo(
        () => agents.filter((agent) => agent.lastPublishedVersion > 0) as AiAgent[],
        [agents]
    );

    // A deployment is filtered by its OWN tags. It used to resolve the tag through the owning agent's tags,
    // which no longer matches what the row shows or what the sidebar lists.
    const filteredDeployments = useMemo(() => {
        if (agentIdFilter) {
            return agentDeployments.filter((deployment) => deployment.agentId === agentIdFilter);
        }

        if (tagIdFilter) {
            return agentDeployments.filter((deployment) =>
                (deployment.tags ?? []).some((tag) => tag?.id === tagIdFilter)
            );
        }

        return agentDeployments;
    }, [agentDeployments, agentIdFilter, tagIdFilter]);

    // Fetched once for the whole list rather than per row, the way ApiCollectionList feeds its own items: each
    // row filters out what it already carries.
    const {data: deploymentTagsData} = useAiAgentDeploymentTagsQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: currentWorkspaceId != null}
    );

    const remainingTags = useMemo(
        () => (deploymentTagsData?.aiAgentDeploymentTags ?? []).map((tag) => ({id: Number(tag.id), name: tag.name})),
        [deploymentTagsData?.aiAgentDeploymentTags]
    );

    const filterAgentTitle = agentIdFilter ? agents.find((agent) => agent.id === agentIdFilter)?.title : undefined;

    // The sidebar filters deployments by agent OR by tag, exactly as the agents list does, so it reuses that
    // page's filter title rather than a second component that would drift from it.
    const filterTagName = tagIdFilter
        ? agentDeployments.flatMap((deployment) => deployment.tags ?? []).find((tag) => tag?.id === tagIdFilter)?.name
        : undefined;

    // The dialog picks the agent itself, so the page only needs the deployable set. An agent deployment is
    // a ProjectDeployment of the agent's hidden backing project, hence projectId + lastPublishedVersion.
    const agentOptions = useMemo(
        () =>
            publishedAgents.map((agent) => ({
                id: agent.id,
                lastPublishedVersion: agent.lastPublishedVersion,
                projectId: agent.projectId,
                title: agent.title,
            })),
        [publishedAgents]
    );

    const handleCreateDialogClose = () => {
        setShowCreateDialog(false);
    };

    // The dialog invalidates the REST projectDeployments keys it owns; this list is the aiAgentDeployments
    // GraphQL query, which those keys do not reach, so a new deployment would not appear until a reload.
    const handleCreateDialogSuccess = () => {
        queryClient.invalidateQueries({queryKey: ['aiAgentDeployments']});
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
                        <div className="flex items-center gap-1">
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
                                <Button label="New Deployment" onClick={() => setShowCreateDialog(true)} />
                            )}
                        </div>
                    }
                    title={
                        filteredDeployments.length > 0 ? (
                            <AgentsFilterTitle agentName={filterAgentTitle} tagName={filterTagName} />
                        ) : (
                            ''
                        )
                    }
                />
            }
            leftSidebarBody={
                <AgentDeploymentsLeftSidebarNav currentAgentId={agentIdFilter} currentTagId={tagIdFilter} />
            }
            leftSidebarHeader={<Header position="sidebar" title="Agent Deployments" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[agentDeploymentsError, agentsError]}
                loading={agentDeploymentsIsLoading || agentsIsLoading}
            >
                {filteredDeployments.length > 0 ? (
                    <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
                        {filteredDeployments.map((deployment) => (
                            <AgentDeploymentListItem
                                deployment={deployment}
                                key={deployment.id}
                                remainingTags={remainingTags}
                            />
                        ))}
                    </div>
                ) : (
                    <EmptyList
                        button={
                            publishedAgents.length > 0 ? (
                                <Button label="New Deployment" onClick={() => setShowCreateDialog(true)} />
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

            {showCreateDialog && (
                <ProjectDeploymentDialog
                    agentOptions={agentOptions}
                    onClose={handleCreateDialogClose}
                    onSuccess={handleCreateDialogSuccess}
                    projectDeployment={{environmentId: currentEnvironmentId} as ProjectDeployment}
                    redirectOnSubmit={false}
                />
            )}
        </LayoutContainer>
    );
};

export default AgentDeployments;
