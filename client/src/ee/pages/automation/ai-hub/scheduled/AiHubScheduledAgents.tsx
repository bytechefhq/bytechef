import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import AiHubChatsSidebar from '@/ee/pages/automation/ai-hub/chats/AiHubChatsSidebar';
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import AgentList from '@/pages/automation/agents/components/agent-list/AgentList';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import isScheduledAgent from '@/pages/automation/agents/utils/isScheduledAgent';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {AiAgent} from '@/shared/middleware/graphql';
import {CalendarClockIcon} from 'lucide-react';
import {useMemo} from 'react';

/** Where AgentDialog returns to after creating an agent from this page. */
const SCHEDULED_PATH = '/automation/ai-hub/scheduled';

/**
 * Scheduled Tasks — every agent the workspace runs on a clock rather than in a chat.
 *
 * The rows ARE the Agents list's rows, filtered to agents owning a `schedule` channel: a scheduled agent is
 * an ordinary agent, so its version, deploy control and row menu should read and behave identically wherever
 * it is listed. A parallel row shape here drifted from that list every time one of them gained a field.
 * Scheduling itself lives on the agent (an `agent_channel` row), so this page owns no editor and adds no
 * query — `aiAgents` already returns each channel's parameters, and AgentListItem renders the cadence.
 *
 * The one write it owns is creating, delegated to the same AgentDialog with the schedule fieldset required:
 * an agent created from "New Task" that carried no schedule would not appear in the list it was created from.
 */
const AiHubScheduledAgents = () => {
    const {agents, agentsError, agentsIsLoading} = useAgents();

    const scheduledAgents = useMemo(() => agents.filter((agent) => isScheduledAgent(agent)), [agents]);

    // "New Task" rather than "New Scheduled Agent": from here the user is scheduling a piece of work, and the
    // agent it creates to carry that work is the mechanism, not the thing being asked for.
    const newTaskDialog = (
        <AgentDialog createdRedirectPath={SCHEDULED_PATH} scheduleRequired triggerNode={<Button label="New Task" />} />
    );

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description="Run agents on a schedule or whenever you need them."
                    position="main"
                    right={scheduledAgents.length > 0 && newTaskDialog}
                    title="Scheduled Tasks"
                />
            }
            // The hub's own sidebar, not a page-specific one: Scheduled Tasks is a destination INSIDE the AI
            // Hub, and dropping the chat list on arrival would strand the user a click away from every chat.
            // `leftSidebarOpen` is deliberately unset so LayoutContainer owns the open state and the header's
            // toggle works here as on every other page with a sidebar.
            leftSidebarBody={<AiHubChatsSidebar />}
            leftSidebarHeader={<Header position="sidebar" title="AI Hub" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[agentsError]} loading={agentsIsLoading}>
                {scheduledAgents.length > 0 ? (
                    <AgentList agents={scheduledAgents as AiAgent[]} />
                ) : (
                    <EmptyList
                        button={newTaskDialog}
                        icon={<CalendarClockIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="A scheduled task runs an agent on its own, on a schedule you set."
                        title="No scheduled tasks"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default AiHubScheduledAgents;
