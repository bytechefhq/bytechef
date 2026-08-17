import {Input} from '@/components/Input/Input';
import AgentsLeftSidebarDropdownMenu from '@/pages/automation/agents/components/AgentsLeftSidebarDropdownMenu';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useAiAgentTagsQuery} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

interface AgentsLeftSidebarNavProps {
    currentAgentId?: string;
    currentTagId?: string | null;
    /**
     * List-page mode. The agents become tag-style filters rather than links to each detail page, and a Tags
     * group is added. The detail page keeps the plain navigation behaviour, where a tag filter would have
     * nothing to filter.
     */
    filterMode?: boolean;
}

/**
 * The workspace's agents, as the Agents pages' left sidebar — the same shape the Data Tables detail page
 * uses, so switching between agents never needs a trip back to the list.
 */
const AgentsLeftSidebarNav = ({currentAgentId, currentTagId, filterMode = false}: AgentsLeftSidebarNavProps) => {
    const [search, setSearch] = useState('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {agents, agentsIsLoading} = useAgents();

    const {data: agentTagsData} = useAiAgentTagsQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: filterMode && currentWorkspaceId != null}
    );

    const tags = agentTagsData?.aiAgentTags ?? [];

    // Only the detail page's navigation list is searchable, matching the data tables sidebar: on the list page
    // this sidebar is a filter, and the agents themselves are already on screen to search through.
    const filteredAgents = useMemo(() => {
        const query = search.trim().toLowerCase();

        if (filterMode || !query) {
            return agents;
        }

        return agents.filter((agent) => agent.title.toLowerCase().includes(query));
    }, [agents, filterMode, search]);

    if (agentsIsLoading) {
        return <></>;
    }

    return (
        <>
            {!filterMode && (
                <div className="space-y-2 px-3 pt-0.5 pb-3">
                    <Input
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Search agents..."
                        value={search}
                    />
                </div>
            )}

            <LeftSidebarNav
                body={
                    <>
                        {filterMode && (
                            <LeftSidebarNavItem
                                item={{current: !currentAgentId && !currentTagId, id: 'all-agents', name: 'All Agents'}}
                                toLink=""
                            />
                        )}

                        {filteredAgents.length ? (
                            filteredAgents.map((agent) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current: agent.id === currentAgentId,
                                        id: agent.id,
                                        name: agent.title,
                                    }}
                                    key={agent.id}
                                    toLink={filterMode ? `?agentId=${agent.id}` : `/automation/agents/${agent.id}`}
                                    trailing={
                                        <AgentsLeftSidebarDropdownMenu
                                            agent={{
                                                description: agent.description,
                                                id: agent.id,
                                                title: agent.title,
                                            }}
                                            current={agent.id === currentAgentId}
                                        />
                                    }
                                />
                            ))
                        ) : (
                            <span className="px-3 text-xs">{search ? 'No agents found.' : 'No agents yet.'}</span>
                        )}
                    </>
                }
                title={filterMode ? 'Agents' : undefined}
            />

            {filterMode && (
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
            )}
        </>
    );
};

export default AgentsLeftSidebarNav;
