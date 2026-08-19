import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import AgentsFilterTitle from '@/pages/automation/agents/components/AgentsFilterTitle';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import isScheduledAgent from '@/pages/automation/agents/utils/isScheduledAgent';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {AiAgent, useImportAiAgentMutation} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useQueryClient} from '@tanstack/react-query';
import {BotIcon, ChevronDownIcon, SparklesIcon, UploadIcon} from 'lucide-react';
import {useEffect, useMemo, useRef} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {toast} from 'sonner';

import AgentDialog from './components/AgentDialog';
import AgentsLeftSidebarNav from './components/AgentsLeftSidebarNav';
import AgentList from './components/agent-list/AgentList';
import useAgents from './hooks/useAgents';

const Agents = () => {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const [searchParams] = useSearchParams();

    const importAgentMutation = useImportAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to import the agent.');
        },
        onSuccess: (data) => {
            invalidateAgentQueries(queryClient);

            // Skills, sub-agents, knowledge bases and every connection are deliberately left behind by the import
            // (see AiAgentFacade.importAgent) — said once here rather than discovered later on a failed publish.
            toast.success('Agent imported. Re-attach its connections, skills, knowledge bases and sub-agents.');

            navigate(`/automation/agents/${data.importAiAgent.id}`);
        },
    });

    const handleImportFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];

        // Cleared before the await so picking the same file twice in a row still fires a change event.
        event.target.value = '';

        if (!file || currentWorkspaceId == null) {
            return;
        }

        importAgentMutation.mutate({json: await file.text(), workspaceId: String(currentWorkspaceId)});
    };

    const agentIdFilter = searchParams.get('agentId');
    const filter = searchParams.get('filter');
    const tagIdFilter = searchParams.get('tagId');

    const {agents, agentsError, agentsIsLoading} = useAgents();

    // A tag or the scheduled filter selects a SET of agents; an explicit agent selection is narrower and wins
    // outright rather than intersecting, so picking one clears the other. Each sidebar item links to a single
    // search param, so only a hand-written URL can set two at once — and then the first branch that matches
    // wins rather than the filters combining.
    const filteredAgents = useMemo(() => {
        if (agentIdFilter) {
            return agents.filter((agent) => agent.id === agentIdFilter);
        }

        if (filter === 'scheduled') {
            return agents.filter((agent) => isScheduledAgent(agent));
        }

        if (tagIdFilter) {
            return agents.filter((agent) => (agent.tags ?? []).some((tag) => tag?.id === tagIdFilter));
        }

        return agents;
    }, [agents, agentIdFilter, filter, tagIdFilter]);

    const filterAgentName = agentIdFilter ? agents.find((agent) => agent.id === agentIdFilter)?.title : undefined;

    // Resolved from the loaded agents rather than a separate tag query: a tag is only selectable in the
    // sidebar because some agent carries it, so it is always present here.
    const filterTagName = tagIdFilter
        ? agents.flatMap((agent) => agent.tags ?? []).find((tag) => tag?.id === tagIdFilter)?.name
        : undefined;

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

                            {agents.length > 0 && (
                                <ButtonGroup>
                                    <AgentDialog triggerNode={<Button label="New Agent" />} />

                                    {/* The import lives on the create button rather than in a row menu: it makes
                                        a NEW agent, so it belongs beside the other way of making one — the same
                                        place the projects page keeps Import Project. */}

                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button aria-label="More create options">
                                                <ChevronDownIcon />
                                            </Button>
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem
                                                disabled={importAgentMutation.isPending}
                                                onClick={() => fileInputRef.current?.click()}
                                            >
                                                <UploadIcon className="mr-2 size-4" /> Import Agent
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </ButtonGroup>
                            )}
                        </div>
                    }
                    title={
                        agents.length > 0 ? (
                            <AgentsFilterTitle
                                agentName={filterAgentName}
                                scheduled={filter === 'scheduled'}
                                tagName={filterTagName}
                            />
                        ) : (
                            ''
                        )
                    }
                />
            }
            leftSidebarBody={
                <AgentsLeftSidebarNav
                    currentAgentId={agentIdFilter ?? undefined}
                    currentFilter={filter}
                    currentTagId={tagIdFilter}
                    filterMode
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Agents" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[agentsError]} loading={agentsIsLoading}>
                {filteredAgents.length > 0 ? (
                    <AgentList agents={filteredAgents as AiAgent[]} />
                ) : (
                    // A filter that matches nothing is not the same as an empty workspace — offering "create
                    // an agent" there would be answering a question the user did not ask.
                    <EmptyList
                        button={
                            agents.length > 0 ? undefined : (
                                <AgentDialog triggerNode={<Button label="Create Agent" />} />
                            )
                        }
                        icon={<BotIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message={
                            agents.length > 0
                                ? 'No agents match the current filter.'
                                : 'Get started by creating a new agent.'
                        }
                        title="No Agents"
                    />
                )}
            </PageLoader>

            <input
                accept=".json"
                onChange={handleImportFileChange}
                ref={fileInputRef}
                style={{display: 'none'}}
                type="file"
            />
        </LayoutContainer>
    );
};

export default Agents;
