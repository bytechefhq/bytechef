import Button from '@/components/Button/Button';
import AgentDetailContent from '@/pages/automation/agents/AgentDetailContent';
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import AgentsLeftSidebarNav from '@/pages/automation/agents/components/AgentsLeftSidebarNav';
import AgentDetailHeader from '@/pages/automation/agents/components/detail/AgentDetailHeader';
import AgentTestChatPanel from '@/pages/automation/agents/components/detail/AgentTestChatPanel';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAiAgentQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {useParams} from 'react-router-dom';

/**
 * The Agent detail/builder page: chrome (header, sidebars, test-chat panel) around the reusable
 * {@link AgentDetailContent} card composition. Reads the {@code agentId} route param and fetches its
 * own copy of the agent (react-query dedupes this against {@link AgentDetailContent}'s own fetch of the
 * same id) so the header's title/description/unpublished-changes badge and the test-chat panel's
 * `draftWorkflowId` are available without threading them down through the shared card composition.
 */
const AgentDetail = () => {
    const {agentId} = useParams<{agentId: string}>();

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const [testPanelOpen, setTestPanelOpen] = useState(false);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);

    const {data} = useAiAgentQuery({id: agentId ?? ''}, {enabled: !!agentId});

    const agent = data?.aiAgent;

    // The two right-hand panels are mutually exclusive by STATE rather than by render gate: opening one
    // closes the other in the same commit, so the layout never briefly loses both and jumps.
    const handleToggleTestPanel = () => {
        setCopilotPanelOpen(false);
        setTestPanelOpen((open) => !open);
    };

    const openCopilot = () => {
        setContext({
            mode: MODE.ASK,
            parameters: {agentId: agent?.id},
            source: Source.AI_AGENT,
        });

        // The copilot renders over the right rail rather than inside it, so leaving the test panel open put
        // one panel on top of the other. Closing it here is the mirror of handleToggleTestPanel closing the
        // copilot, and both happen in the same commit as the panel that replaces them.
        setTestPanelOpen(false);

        setCopilotPanelOpen(true);
    };

    return (
        <LayoutContainer
            header={
                agent && (
                    <AgentDetailHeader
                        description={agent.description}
                        id={agent.id}
                        lastPublishedVersion={agent.lastPublishedVersion}
                        onAskCopilot={copilotEnabled ? openCopilot : undefined}
                        onToggleTestPanel={handleToggleTestPanel}
                        projectId={agent.projectId}
                        testPanelOpen={testPanelOpen}
                        title={agent.title}
                        visibility={agent.visibility}
                    />
                )
            }
            leftSidebarBody={<AgentsLeftSidebarNav currentAgentId={agentId} />}
            leftSidebarHeader={
                <Header
                    position="sidebar"
                    right={<AgentDialog triggerNode={<Button icon={<PlusIcon />} size="icon" variant="ghost" />} />}
                    title="Agents"
                />
            }
            leftSidebarWidth="64"
            rightSidebarAnimate
            rightSidebarBody={
                // Keyed by agent id so switching agents remounts the panel: its runtime provider owns local
                // message/conversation state per mount (see AgentTestChatRuntimeProvider), which must not
                // leak from one agent's draft conversation into another's after a route-param-only navigation.
                agent && (
                    <AgentTestChatPanel
                        key={agent.id}
                        onClose={() => setTestPanelOpen(false)}
                        workflowId={agent.draftWorkflowId}
                    />
                )
            }
            rightSidebarClass="border-l border-l-border/50"
            rightSidebarOpen={!!agent && testPanelOpen}
            // Matches the copilot panel's width: the two now sit side by side rather than swapping, and a
            // differing width made the page shift sideways on every toggle.
            rightSidebarWidth="450"
        >
            {agentId && <AgentDetailContent agentId={agentId} />}
        </LayoutContainer>
    );
};

export default AgentDetail;
