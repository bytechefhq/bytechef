import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiHubChatsSidebarToggle from '@/ee/pages/automation/ai-hub/AiHubChatsSidebarToggle';
import AiHubArtifactsCard from '@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactsCard';
import useAiHubArtifactsCard from '@/ee/pages/automation/ai-hub/artifacts/useAiHubArtifactsCard';
import {isWebhookBridgedChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore, useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubChatComposer from '@/ee/pages/automation/ai-hub/composer/AiHubChatComposer';
import useAiHubChatLaunchers from '@/ee/pages/automation/ai-hub/hooks/useAiHubChatLaunchers';
import AiHubThread from '@/ee/pages/automation/ai-hub/messages/AiHubThread';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useAiHubTaskQuery} from '@/ee/pages/automation/ai-hub/tasks/hooks/useAiHubTasks';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {useAiDefaultModelQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PanelRightOpenIcon, WrenchIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const AiHubPanel = () => {
    const {generateChatId, resetMessages} = useAiHubStore(
        useShallow((state) => ({
            generateChatId: state.generateChatId,
            resetMessages: state.resetMessages,
        }))
    );

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);

    const {data: defaultModelData} = useAiDefaultModelQuery({environment: String(currentEnvironmentId)});
    // The picker's Agents / Workflows cascades. Picking one leaves this chat and lands on a freshly created
    // one, so the launchers belong here just as much as on the home composer — a user already in a
    // conversation shouldn't have to navigate home to start a different kind of chat.
    const {agentChats, onSelectAgentChat, onSelectWorkflowChat, workflowChats} = useAiHubChatLaunchers();
    // Per-chat LLM picker selection. Reads/writes the per-chat map in the chats store; both null = no
    // override (server uses workspace default; task chats still get their per-task override
    // applied server-side ahead of the workspace default).
    const chatLlmSelection = useAiHubChatsStore((state) =>
        currentChatId != null ? state.chatLlmSelections[currentChatId] : undefined
    );
    const setChatLlmSelection = useAiHubChatsStore((state) => state.setChatLlmSelection);

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

    // The chat list query is already loaded by the sidebar; reading from the same query key here
    // is essentially a cache lookup. We don't fetch a single chat by id because we need to react
    // to title-generation updates that the list query already invalidates.
    const {data: chats} = useAiHubChatsQuery(currentWorkspaceId, currentEnvironmentId, 'ACTIVE');

    const currentChat = chats?.find((chat) => chat.id === currentChatId);
    // "New Chat" placeholder until the auto-title generator (kicked off by runPostTurnTelemetry
    // around message-count >= 6) writes a real title back. Using this label everywhere the title falls
    // back lets the user see "this is a fresh chat" instead of the more terminal-sounding "Untitled".
    const chatTitle = currentChat?.title || 'New Chat';
    // Webhook-bridged chats get a small badge under the title. The badge anchors the routing distinction
    // visibly when the panel header is the only place the chat identity surfaces, and names what the user
    // picked: an agent chat says "agent", never the workflow behind it.
    const isWorkflowChat = isWebhookBridgedChat(currentChat?.kind);
    const isAgentChat = currentChat?.kind === 'AGENT_CHAT';
    const isTaskChat = currentChat?.kind === 'TASK';

    // For TASK-kind chats, fetch the bound task so the ModelPicker can surface "Use task
    // default" as the revert label (instead of "Use workspace default") and preselect the task's
    // pinned (llmProvider, llmModel) in the trigger when the user hasn't overridden per-conversation.
    // The query is gated on the kind + task id presence so it's a true no-op for STANDARD / WORKFLOW_CHAT
    // chats (TanStack Query short-circuits at the enabled=false fence).
    const {data: task} = useAiHubTaskQuery(
        currentChat?.aiHubTaskId ?? undefined,
        currentWorkspaceId ?? 0,
        isTaskChat && currentChat?.aiHubTaskId != null && currentWorkspaceId != null
    );

    const navigate = useNavigate();

    // Same hook the card itself reads, so the column's inset and the card's presence can never disagree.
    // Both calls resolve against the same two react-query keys, so this one is a cache read.
    const {visible: artifactsCardVisible} = useAiHubArtifactsCard();

    // Note: the "Tasks" / "Workflow chats" cascades in the model picker are intentionally NOT
    // wired here. Those entries start a NEW conversation, so they only belong on the home composer
    // (AiHubHomePanel) where the user is choosing what to start — inside an existing chat the picker is
    // strictly for switching the model. See AiHubHomePanel for that wiring.

    return (
        <div className="relative flex size-full min-h-[50vh] flex-col overflow-x-hidden">
            {/* Floating artifacts list, absolutely positioned against this relative root. Self-hiding —
             * see useAiHubArtifactsCard for the conditions under which it renders nothing at all. */}

            <AiHubArtifactsCard />

            {/*
             * Panel header: sidebar toggle + chat title on the left, action row (EnvironmentSelect →
             * tool-call toggle → resource panel toggle) on the right. The page-level top header was
             * removed in favor of sidebars stretching top-to-bottom, so the chat title and the
             * EnvironmentSelect both live here next to the per-thread action affordances.
             */}

            <div className="flex items-center justify-between gap-4 px-3 py-3">
                <div className="flex min-w-0 flex-1 items-center gap-2">
                    {/* Same place the shared Header puts the sidebar toggle on other pages: first in the
                     * title row. See AiHubChatsSidebarToggle for why the AI Hub renders its own. */}

                    <AiHubChatsSidebarToggle />

                    <h2 className="min-w-0 truncate text-base font-medium" title={chatTitle}>
                        {chatTitle}
                    </h2>

                    {isWorkflowChat && (
                        <span
                            className="shrink-0 rounded-full bg-surface-brand-secondary px-2 py-1 text-xs leading-none font-medium text-content-brand-primary"
                            title={
                                isAgentChat
                                    ? "This chat is bound to an AI Agent. Messages are forwarded to the agent's workflow instead of the AI Hub's own LLM."
                                    : "This chat is bound to a workflow execution. Messages are forwarded to the workflow's webhook trigger instead of an LLM."
                            }
                        >
                            {isAgentChat ? 'Agent chat' : 'Workflow chat'}
                        </span>
                    )}
                </div>

                <div className="flex items-center gap-1">
                    {/*
                     * EnvironmentSelect first so it reads as "set scope, then act on scope". Switching
                     * environments invalidates the active chat (it's scoped to one env's tables),
                     * so the onChange resets chat state + routes home rather than leaving the
                     * user staring at a stale 404 panel.
                     */}

                    <EnvironmentSelect
                        onChange={() => {
                            aiHubChatsStore.getState().setCurrentChatId(undefined);

                            resetMessages();
                            generateChatId();

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

                {/*
                 * The artifacts card floats over the top-right of this column, so while it is up both the
                 * transcript and the composer reserve its width (w-64 + right-3 = 268px) instead of letting
                 * content run underneath it. The two insets are applied separately rather than once on this
                 * container because the thread's scroll viewport is a descendant: padding the container
                 * would narrow the scroller and drag the scrollbar away from the pane edge. Both are the
                 * same value, and both children are `mx-auto` max-width boxes, so they shift by the same
                 * amount and keep a shared right edge.
                 */}

                <div className="min-h-0 flex-1">
                    <AiHubThread
                        contentInsetRight={artifactsCardVisible}
                        showSuggestions={!isWorkflowChat && !isTaskChat}
                    />
                </div>

                {/*
                 * Per-chat LLM picker, rendered as the leading control in the composer footer (replacing
                 * the previous header placement). Hidden for WORKFLOW_CHAT chats — those route through
                 * the workflow's webhook trigger, not the LLM, so a model override would be a no-op.
                 * For TASK chats, the task's pinned (llmProvider, llmModel) is passed as
                 * taskDefault* so the picker shows the task's model in the trigger and "Use task
                 * default" clears the user override (precedence: user-selected > task >
                 * workspace default).
                 */}

                <div
                    className={twMerge(
                        'transition-[padding] duration-200 ease-in-out',
                        artifactsCardVisible && 'pr-68'
                    )}
                >
                    <AiHubChatComposer
                        modelPicker={
                            !isWorkflowChat && currentChatId != null && currentWorkspaceId != null ? (
                                <ModelPicker
                                    agentChats={agentChats}
                                    defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                    defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
                                    environment={currentEnvironmentId}
                                    onChange={(provider, model) => {
                                        writeLastUsedModel(currentWorkspaceId, provider, model);
                                        setChatLlmSelection(currentChatId, provider, model);
                                    }}
                                    onSelectAgentChat={onSelectAgentChat}
                                    onSelectWorkflowChat={onSelectWorkflowChat}
                                    selectedModel={
                                        chatLlmSelection?.model ?? readLastUsedModel(currentWorkspaceId)?.model ?? null
                                    }
                                    selectedProvider={
                                        chatLlmSelection?.provider ??
                                        readLastUsedModel(currentWorkspaceId)?.provider ??
                                        null
                                    }
                                    taskDefaultModel={task?.llmModel ?? null}
                                    taskDefaultProvider={task?.llmProvider ?? null}
                                    workflowChats={workflowChats}
                                />
                            ) : null
                        }
                    />
                </div>
            </div>
        </div>
    );
};

export default AiHubPanel;
