import useAgents from '@/pages/automation/agents/hooks/useAgents';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useAiAgentDeploymentTagsQuery} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';

interface AgentDeploymentsLeftSidebarNavProps {
    currentAgentId?: string | null;
    currentTagId?: string | null;
}

/**
 * Filters the deployment list by agent or by tag, the way the Projects sidebar does. Both selections ride in
 * search params (`agentId` / `tagId`) — the agent detail page's Deployments link sets `agentId`, so arriving
 * from an agent lands pre-filtered. The two filters are mutually exclusive: picking one clears the other,
 * since a tag selects a SET of agents and an explicit agent selection would be a narrower, confusing overlay.
 */
const AgentDeploymentsLeftSidebarNav = ({currentAgentId, currentTagId}: AgentDeploymentsLeftSidebarNavProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {agents, agentsIsLoading} = useAgents();

    // The DEPLOYMENTS' own tags, not the owning agents'. This list drives the tag filter below, and a
    // deployment is filtered by the tags it carries — listing agent tags here offered filters that
    // matched nothing and hid deployment tags that do.
    const {data: agentTagsData} = useAiAgentDeploymentTagsQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: currentWorkspaceId != null}
    );

    const tags = agentTagsData?.aiAgentDeploymentTags ?? [];

    if (agentsIsLoading) {
        return <></>;
    }

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{current: !currentAgentId && !currentTagId, id: 'all-agents', name: 'All Agents'}}
                            toLink=""
                        />

                        {agents.map((agent) => (
                            <LeftSidebarNavItem
                                item={{
                                    current: agent.id === currentAgentId,
                                    id: agent.id,
                                    name: agent.title,
                                }}
                                key={agent.id}
                                toLink={`?agentId=${agent.id}`}
                            />
                        ))}
                    </>
                }
                title="Agents"
            />

            <LeftSidebarNav
                body={
                    tags.length ? (
                        tags.map((tag) => (
                            <LeftSidebarNavItem
                                icon={<TagIcon className="mr-1 size-4" />}
                                item={{
                                    current: tag.id === currentTagId,
                                    id: tag.id,
                                    name: tag.name,
                                }}
                                key={tag.id}
                                toLink={`?tagId=${tag.id}`}
                            />
                        ))
                    ) : (
                        <span className="px-3 text-xs">No defined tags.</span>
                    )
                }
                className="mb-0"
                title="Tags"
            />
        </>
    );
};

export default AgentDeploymentsLeftSidebarNav;
