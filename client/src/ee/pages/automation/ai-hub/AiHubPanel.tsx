import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiHubChatComposer from '@/ee/pages/automation/ai-hub/composer/AiHubChatComposer';
import AiHubThread from '@/ee/pages/automation/ai-hub/messages/AiHubThread';
import {useAiHubPersonalAgentQuery} from '@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useAiHubTasksQuery} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {aiHubTasksStore, useAiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {useAiDefaultModelQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PanelRightOpenIcon, WrenchIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const AiHubPanel = () => {
    const {generateTaskId, resetMessages} = useAiHubStore(
        useShallow((state) => ({
            generateTaskId: state.generateTaskId,
            resetMessages: state.resetMessages,
        }))
    );

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentTaskId = useAiHubTasksStore((state) => state.currentTaskId);

    const {data: defaultModelData} = useAiDefaultModelQuery({environment: String(currentEnvironmentId)});
    // Per-task LLM picker selection. Reads/writes the per-task map in the tasks store; both null = no
    // override (server uses workspace default; personal-agent tasks still get their per-agent override
    // applied server-side ahead of the workspace default).
    const taskLlmSelection = useAiHubTasksStore((state) =>
        currentTaskId != null ? state.taskLlmSelections[currentTaskId] : undefined
    );
    const setTaskLlmSelection = useAiHubTasksStore((state) => state.setTaskLlmSelection);

    const {rightPanelOpen, setRightPanelOpen} = useAiHubTabsStore(
        useShallow((state) => ({
            rightPanelOpen: state.rightPanelOpen,
            setRightPanelOpen: state.setRightPanelOpen,
        }))
    );

    const {setShowToolCalls, showToolCalls} = useAiHubSettingsStore(
        useShallow((state) => ({
            setShowToolCalls: state.setShowToolCalls,
            showToolCalls: state.showToolCalls,
        }))
    );

    // The task list query is already loaded by the sidebar; reading from the same query key here
    // is essentially a cache lookup. We don't fetch a single task by id because we need to react
    // to title-generation updates that the list query already invalidates.
    const {data: tasks} = useAiHubTasksQuery(currentWorkspaceId, currentEnvironmentId, 'ACTIVE');

    const currentTask = tasks?.find((task) => task.id === currentTaskId);
    // "New Task" placeholder until the auto-title generator (kicked off by runPostTurnTelemetry
    // around message-count >= 6) writes a real title back. Using this label everywhere the title falls
    // back lets the user see "this is a fresh chat" instead of the more terminal-sounding "Untitled".
    const taskTitle = currentTask?.title || 'New Task';
    // Workflow-chat tasks get a small badge under the title. The badge anchors the routing
    // distinction visibly when the panel header is the only place the task identity surfaces.
    const isWorkflowChat = currentTask?.kind === 'WORKFLOW_CHAT';
    const isPersonalAgentTask = currentTask?.kind === 'PERSONAL_AGENT';

    // For PERSONAL_AGENT-kind tasks, fetch the bound agent so the ModelPicker can surface "Use agent
    // default" as the revert label (instead of "Use workspace default") and preselect the agent's
    // pinned (llmProvider, llmModel) in the trigger when the user hasn't overridden per-conversation.
    // The query is gated on the kind + agent id presence so it's a true no-op for STANDARD / WORKFLOW_CHAT
    // tasks (TanStack Query short-circuits at the enabled=false fence).
    const {data: personalAgent} = useAiHubPersonalAgentQuery(
        currentTask?.aiHubPersonalAgentId ?? undefined,
        currentWorkspaceId ?? 0,
        isPersonalAgentTask && currentTask?.aiHubPersonalAgentId != null && currentWorkspaceId != null
    );

    const navigate = useNavigate();

    // Note: the "Personal agents" / "Workflow chats" cascades in the model picker are intentionally NOT
    // wired here. Those entries start a NEW conversation, so they only belong on the home composer
    // (AiHubHomePanel) where the user is choosing what to start — inside an existing task the picker is
    // strictly for switching the model. See AiHubHomePanel for that wiring.

    return (
        <div className="relative flex size-full min-h-[50vh] flex-col overflow-x-hidden">
            {/*
             * Panel header: task title on the left, action row (EnvironmentSelect → Ask/Build
             * toggle → clean messages → panel toggle) on the right. The page-level top header was
             * removed in favor of sidebars stretching top-to-bottom, so the task title and the
             * EnvironmentSelect both live here next to the per-thread action affordances.
             */}

            <div className="flex items-center justify-between gap-4 px-3 py-3">
                <div className="flex min-w-0 flex-1 items-baseline gap-2">
                    <h2 className="min-w-0 truncate text-base font-medium" title={taskTitle}>
                        {taskTitle}
                    </h2>

                    {isWorkflowChat && (
                        <span
                            className="shrink-0 rounded-full bg-surface-brand-secondary px-2 py-1 text-xs leading-none font-medium text-content-brand-primary"
                            title="This task is bound to a workflow execution. Messages are forwarded to the workflow's webhook trigger instead of an LLM."
                        >
                            Workflow chat
                        </span>
                    )}
                </div>

                <div className="flex items-center gap-1">
                    {/*
                     * EnvironmentSelect first so it reads as "set scope, then act on scope". Switching
                     * environments invalidates the active task (it's scoped to one env's tables),
                     * so the onChange resets task state + routes home rather than leaving the
                     * user staring at a stale 404 panel.
                     */}

                    <EnvironmentSelect
                        onChange={() => {
                            aiHubTasksStore.getState().setCurrentTaskId(undefined);

                            resetMessages();
                            generateTaskId();

                            navigate('/automation/ai-hub');
                        }}
                    />

                    {/*
                     * The Ask / Build mode control moved into the message composer as a single labeled
                     * switch (ModeSwitch in AiHubChatComposer) — one toggle near the send button replaces the
                     * two-button segmented control that used to live here.
                     */}

                    <div className="flex items-center gap-1">
                        {/* Tool-call cards are hidden by default so the transcript reads as a conversation;
                         * this flips them on for inspection. Persisted (useAiHubSettingsStore). */}

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label={showToolCalls ? 'Hide tool calls' : 'Show tool calls'}
                                    aria-pressed={showToolCalls}
                                    className={showToolCalls ? 'text-content-brand-primary' : undefined}
                                    icon={<WrenchIcon />}
                                    onClick={() => setShowToolCalls(!showToolCalls)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>{showToolCalls ? 'Hide tool calls' : 'Show tool calls'}</TooltipContent>
                        </Tooltip>

                        {/* Only the OPEN affordance lives in this header. When the panel is open the matching
                         * CLOSE button lives in the right panel itself (after its + button) — keeping the
                         * close affordance contextual to the panel that's being dismissed. */}

                        {!rightPanelOpen && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        aria-label="Open resource panel"
                                        icon={<PanelRightOpenIcon />}
                                        onClick={() => setRightPanelOpen(true)}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Show resources</TooltipContent>
                            </Tooltip>
                        )}
                    </div>
                </div>
            </div>

            <div className="relative -mr-1 flex min-h-0 flex-1 flex-col">
                {/* No retry banner here on purpose: agent / tool failures surface via the global toast layer
                 * (see aiChatRetryableErrorStore consumers + sonner). Rendering both a banner inside
                 * the thread AND a toast double-notifies the user for the same event. */}

                <div className="min-h-0 flex-1">
                    <AiHubThread showSuggestions={!isWorkflowChat && !isPersonalAgentTask} />
                </div>

                {/*
                 * Per-task LLM picker, rendered as the leading control in the composer footer (replacing
                 * the previous header placement). Hidden for WORKFLOW_CHAT tasks — those route through
                 * the workflow's webhook trigger, not the LLM, so a model override would be a no-op.
                 * For PERSONAL_AGENT tasks, the agent's pinned (llmProvider, llmModel) is passed as
                 * agentDefault* so the picker shows the agent's model in the trigger and "Use agent
                 * default" clears the user override (precedence: user-selected > personal-agent >
                 * workspace default).
                 */}

                <AiHubChatComposer
                    modelPicker={
                        !isWorkflowChat && currentTaskId != null && currentWorkspaceId != null ? (
                            <ModelPicker
                                agentDefaultModel={personalAgent?.llmModel ?? null}
                                agentDefaultProvider={personalAgent?.llmProvider ?? null}
                                defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
                                environment={currentEnvironmentId}
                                onChange={(provider, model) => {
                                    writeLastUsedModel(currentWorkspaceId, provider, model);
                                    setTaskLlmSelection(currentTaskId, provider, model);
                                }}
                                selectedModel={
                                    taskLlmSelection?.model ?? readLastUsedModel(currentWorkspaceId)?.model ?? null
                                }
                                selectedProvider={
                                    taskLlmSelection?.provider ??
                                    readLastUsedModel(currentWorkspaceId)?.provider ??
                                    null
                                }
                            />
                        ) : null
                    }
                />
            </div>
        </div>
    );
};

export default AiHubPanel;
