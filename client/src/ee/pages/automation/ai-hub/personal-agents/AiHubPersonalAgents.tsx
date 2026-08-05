import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiHubTasksSidebar from '@/ee/pages/automation/ai-hub/tasks/AiHubTasksSidebar';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {InfoIcon, PlusIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

import AiHubPersonalAgentsList from './AiHubPersonalAgentsList';
import {useAiHubPersonalAgentsQuery} from './hooks/useAiHubPersonalAgents';

/**
 * Full-page route for Personal Agents — `/automation/ai-hub/personal-agents`. Reuses the
 * AI Hub tasks sidebar (same one that anchors `/automation/ai-hub`) so the user
 * can hop between Personal Agents, Workflow Chats, the tasks list, and the active task
 * thread without losing the surrounding navigation. Page body is the {@link AiHubPersonalAgentsList} list
 * view; main header carries an inline title, the environment selector, and the New Agent CTA.
 *
 * <p>The "New Agent" button only renders in the header when at least one agent exists. When the list
 * is empty, the empty-state inside {@link AiHubPersonalAgentsList} owns the create CTA ("Create your first
 * agent") so the user has a single, prominent call to action rather than two stacked buttons.</p>
 *
 * <p>Owns the create-dialog open state because the trigger lives in two places (header CTA when agents
 * exist, empty-state CTA when none). Edit and delete dialogs stay local to the list rows since they're
 * row-triggered. {@code useAiHubPersonalAgentsQuery} is called here AND in the list — React Query dedupes by
 * query key so only one network request fires.</p>
 */
const AiHubPersonalAgents = () => {
    const navigate = useNavigate();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: agents} = useAiHubPersonalAgentsQuery(currentWorkspaceId ?? 0, currentEnvironmentId);
    const hasAgents = agents != null && agents.length > 0;

    // Both the header CTA and the empty-state CTA route to the dedicated form page rather than opening a
    // dialog. The previous dialog-based create flow had no room for the tools picker (so the picker was
    // edit-only); the standalone route fits the full agent definition + its tool template in one form.
    const goToNewAgent = () => navigate('/automation/ai-hub/personal-agents/new');

    return (
        <LayoutContainer
            header={
                <div className="flex w-full items-center gap-2 px-6 py-3">
                    {/*
                     * Header collapses back to a single row: title + info-icon on the left, env selector
                     * and (conditional) New Agent on the right. The description that previously rendered
                     * below the title now lives in a tooltip on the {@code InfoIcon} so the header stays
                     * compact while keeping the explanatory copy one hover away.
                     */}

                    <div className="flex min-w-0 flex-1 items-center gap-1.5">
                        <h2 className="truncate text-base font-medium">Personal Agents</h2>

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <InfoIcon
                                    aria-label="What are personal agents?"
                                    className="size-4 shrink-0 text-muted-foreground"
                                />
                            </TooltipTrigger>

                            <TooltipContent className="max-w-sm" side="bottom">
                                Define focused assistants once and reuse them across sessions. Each agent applies its
                                instructions on every turn of any task it spawns.
                            </TooltipContent>
                        </Tooltip>
                    </div>

                    <EnvironmentSelect />

                    {hasAgents && <Button icon={<PlusIcon />} label="New Agent" onClick={goToNewAgent} />}
                </div>
            }
            leftSidebarBody={<AiHubTasksSidebar />}
            leftSidebarHeader={<Header position="sidebar" title="AI Hub" />}
            leftSidebarWidth="64"
        >
            <div className="flex w-full flex-1 flex-col p-6">
                <AiHubPersonalAgentsList onOpenCreateDialog={goToNewAgent} />
            </div>
        </LayoutContainer>
    );
};

export default AiHubPersonalAgents;
