import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {InfoIcon} from 'lucide-react';

import AiHubTasksSidebar from './AiHubTasksSidebar';
import WorkflowChatsList from './WorkflowChatsList';

/**
 * Full-page route for Workflow Chats — `/automation/ai-hub/workflow-chats`. Reuses the
 * AI Hub tasks sidebar so the user keeps navigation continuity with the rest of CC.
 * Page body is the {@link WorkflowChatsList} list view; main header carries an inline title and the
 * environment selector.
 */
const WorkflowChats = () => (
    <LayoutContainer
        header={
            <div className="flex w-full items-center gap-2 px-6 py-3">
                {/*
                 * Mirrors the {@link AiHubPersonalAgents} page layout: title + InfoIcon-anchored tooltip on
                 * the left, env selector on the right. The tooltip explains what a workflow chat is so
                 * the user understands why these rows route through the workflow trigger instead of the
                 * LLM agent — a distinction that's not obvious from the row labels alone.
                 */}

                <div className="flex min-w-0 flex-1 items-center gap-1.5">
                    <h2 className="truncate text-base font-medium">Workflow Chats</h2>

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <InfoIcon
                                aria-label="What are workflow chats?"
                                className="size-4 shrink-0 text-muted-foreground"
                            />
                        </TooltipTrigger>

                        <TooltipContent className="max-w-sm" side="bottom">
                            Each row is a workflow with a chat trigger. Clicking one starts a fresh task that sends
                            every message to the workflow's webhook instead of an LLM. Use it to drive automations
                            interactively — the workflow decides how to respond.
                        </TooltipContent>
                    </Tooltip>
                </div>

                <EnvironmentSelect />
            </div>
        }
        leftSidebarBody={<AiHubTasksSidebar />}
        leftSidebarHeader={<Header position="sidebar" title="AI Hub" />}
        leftSidebarWidth="64"
    >
        <div className="flex w-full flex-1 flex-col gap-4 p-6">
            <WorkflowChatsList />
        </div>
    </LayoutContainer>
);

export default WorkflowChats;
