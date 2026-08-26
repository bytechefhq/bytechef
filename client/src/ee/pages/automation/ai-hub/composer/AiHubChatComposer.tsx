import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {isChannelAgentChat, isWebhookBridgedChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubComposer from '@/ee/pages/automation/ai-hub/composer/AiHubComposer';
import AiHubComposerDropZone from '@/ee/pages/automation/ai-hub/composer/AiHubComposerDropZone';
import AiHubSkillsMenu from '@/ee/pages/automation/ai-hub/composer/AiHubSkillsMenu';
import {
    ALLOWED_MIME_TYPES,
    useAiHubAttachmentUpload,
} from '@/ee/pages/automation/ai-hub/composer/hooks/useAiHubAttachmentUpload';
import {
    ReferencedResourceKindType,
    aiHubComposerStore,
    useAiHubComposerStore,
} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import ChatToolChips from '@/ee/pages/automation/ai-hub/tools/ChatToolChips';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModeSwitch from '@/shared/components/ModeSwitch/ModeSwitch';
import {usePushToTalk} from '@/shared/hooks/usePushToTalk';
import {useCancelAiHubRunMutation, useCancelWorkflowChatTurnMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ComposerPrimitive, ThreadPrimitive, useAui} from '@assistant-ui/react';
import {
    ArrowUpIcon,
    HexagonIcon,
    Loader2Icon,
    MicIcon,
    PaperclipIcon,
    RadioTowerIcon,
    RotateCcwIcon,
    SquareIcon,
    XIcon,
} from 'lucide-react';
import {KeyboardEvent, ReactNode, useCallback, useEffect, useRef} from 'react';
import {twMerge} from 'tailwind-merge';

const KIND_LABELS: Record<ReferencedResourceKindType, string> = {
    aiAgent: 'AI Agent',
    apiCollection: 'API Collection',
    chat: 'Previous Chat',
    dataTable: 'Data Table',
    file: 'File',
    knowledgeBase: 'Knowledge Base',
    mcpServer: 'MCP Server',
    workflow: 'Workflow',
    workflowExecution: 'Workflow Execution',
};

const KIND_BADGE_CLASSES: Record<ReferencedResourceKindType, string> = {
    aiAgent: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300',
    apiCollection: 'bg-pink-100 text-pink-700 dark:bg-pink-950 dark:text-pink-300',
    chat: 'bg-slate-100 text-slate-700 dark:bg-slate-950 dark:text-slate-300',
    dataTable: 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300',
    file: 'bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300',
    knowledgeBase: 'bg-purple-100 text-purple-700 dark:bg-purple-950 dark:text-purple-300',
    mcpServer: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-950 dark:text-cyan-300',
    workflow: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300',
    workflowExecution: 'bg-orange-100 text-orange-700 dark:bg-orange-950 dark:text-orange-300',
};

interface AiHubChatComposerPropsI {
    /**
     * Optional pre-built LLM provider/model picker rendered as the first control in the composer's
     * left footer group (before the resource picker / paperclip). The parent passes the picker as a
     * node so it can wire the per-chat selection state (chatLlmSelections), workflow-chat / personal-
     * agent overrides, and workspace-scoped GraphQL queries without duplicating those hooks here.
     * When undefined, no picker is rendered (workflow chats pass nothing).
     */
    modelPicker?: ReactNode;
}

const AiHubChatComposer = ({modelPicker}: AiHubChatComposerPropsI) => {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const previousChatIdRef = useRef<string | undefined>(undefined);
    // The textarea a '@' was last typed into, held so focus can be handed back when the picker it raised
    // closes. Null whenever the picker was opened some other way (the "+" button), which must keep Radix's
    // ordinary behaviour of restoring focus to its trigger.
    const atMentionTextareaRef = useRef<HTMLTextAreaElement | null>(null);

    const referencedResources = useAiHubComposerStore((state) => state.referencedResources);
    const resourcePickerOpen = useAiHubComposerStore((state) => state.resourcePickerOpen);
    const selectedSkills = useAiHubComposerStore((state) => state.selectedSkills);
    const mode = useAiHubStore((state) => state.mode);
    const setMode = useAiHubStore((state) => state.setMode);
    const chatId = useAiHubStore((state) => state.chatId);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {dismiss, retry, upload, uploads} = useAiHubAttachmentUpload(currentWorkspaceId);

    // Look up the active chat's kind so we route to the right server-side cancel mutation:
    // - WORKFLOW_CHAT → cancelWorkflowChatTurn (stops the workflow execution via JobFacade)
    // - STANDARD → cancelAiHubRun (marks the InFlightAiHubRunRegistry entry
    //   terminated so a re-mount probe sees the chat as not in flight and stops showing the
    //   streaming UI)
    // Without the second mutation, a Stop click only closed the client-side SSE; the agent kept
    // streaming server-side and a return-to-chat re-attached to it, re-showing the Stop button.
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const {data: chats} = useAiHubChatsQuery(currentWorkspaceId, currentEnvironmentId, 'ACTIVE');
    const activeChat = chats?.find((chat) => chat.id === currentChatId);
    const isWorkflowChat = isWebhookBridgedChat(activeChat?.kind);
    // Channel-born agent chats (Slack, a schedule, …) are read-only: the conversation is driven by its
    // channel, not by AI Hub, so a typed message here has nowhere real to go — the server's
    // WebhookBridgeAgent rejects the null workflowExecutionId with an error rather than misrouting into
    // the actual Slack channel, but that's a server-side safety net, not a substitute for the client not
    // offering the affordance at all. isWorkflowChat alone is NOT this gate: typing is the entire point of
    // a WORKFLOW_CHAT (and a composer-created AGENT_CHAT), so gating on it would break those.
    const isChannelBornChat = activeChat != null && isChannelAgentChat(activeChat);

    const cancelWorkflowChatTurnMutation = useCancelWorkflowChatTurnMutation();
    const cancelAiHubRunMutation = useCancelAiHubRunMutation();

    const aui = useAui();

    const {
        isRecording: isVoiceRecording,
        isTranscribing: isVoiceTranscribing,
        start: startPushToTalk,
        stop: stopPushToTalk,
    } = usePushToTalk({
        onTranscript: (text) => {
            const currentText = aui.composer.getState().text;

            aui.composer.setText(currentText ? `${currentText} ${text}` : text);
        },
    });

    const handleMicClick = useCallback(() => {
        if (isVoiceRecording) {
            stopPushToTalk();
        } else {
            void startPushToTalk();
        }
    }, [isVoiceRecording, startPushToTalk, stopPushToTalk]);

    const handleCancelTurn = useCallback(() => {
        // Always close the AG-UI stream first via the runtime — this fires the AbortController which
        // cleans up the client-side SSE subscriber and flips isAgentRunning false. Then issue the
        // server-side cancel mutation matching the chat's kind, so the agent / workflow actually
        // stops producing events on the server too. Without the server-side cancel, the run stays
        // in the InFlightAiHubRunRegistry and a re-mount probe re-attaches to it, re-showing the
        // Stop button.
        //
        // The mutation runs to completion even if the user navigates away — the result isn't
        // user-facing, just a stopJob() / registry.cancel() side effect.
        aui.thread.cancelRun();

        if (currentChatId == null || currentWorkspaceId == null) {
            return;
        }

        if (isWorkflowChat) {
            cancelWorkflowChatTurnMutation.mutate({
                id: String(currentChatId),
                workspaceId: String(currentWorkspaceId),
            });
        } else {
            // runId lets the server tombstone the exact run if this Stop reaches it before the agent
            // POST registers the run. undefined when no turn runId is known — server falls back to
            // threadId-only cancel.
            const runId = chatId != null ? aiHubRunStateStore.getState().runIdByChat[chatId] : undefined;

            cancelAiHubRunMutation.mutate({
                id: String(currentChatId),
                runId,
                workspaceId: String(currentWorkspaceId),
            });
        }
    }, [
        cancelAiHubRunMutation,
        cancelWorkflowChatTurnMutation,
        currentChatId,
        currentWorkspaceId,
        isWorkflowChat,
        chatId,
        aui,
    ]);

    const handleAttachClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const selected = event.target.files;

        if (selected && selected.length > 0) {
            upload(Array.from(selected));
        }

        event.target.value = '';
    };

    const handleFilesDropped = useCallback(
        (files: File[]) => {
            upload(files);
        },
        [upload]
    );

    const handleRemoveReference = (id: string, kind: ReferencedResourceKindType) => {
        aiHubComposerStore.getState().removeReference(id, kind);
    };

    const handleRemoveSkill = (id: string) => {
        aiHubComposerStore.getState().removeSkill(id);
    };

    // Paste handler — moved here from AiHubComposer (which no longer owns the upload hook). Document-level
    // listener so any paste with files while this composer is mounted triggers an upload. Skipped for
    // workflow chats, which cannot have any resource attached (messages forward to a webhook, not an
    // LLM agent that consumes attached context).
    useEffect(() => {
        if (isWorkflowChat) {
            return;
        }

        const handlePaste = (event: ClipboardEvent) => {
            const clipboardFiles = event.clipboardData?.files;

            if (!clipboardFiles || clipboardFiles.length === 0) {
                return;
            }

            event.preventDefault();

            upload(Array.from(clipboardFiles));
        };

        document.addEventListener('paste', handlePaste);

        return () => {
            document.removeEventListener('paste', handlePaste);
        };
    }, [isWorkflowChat, upload]);

    // Clear the composer draft when switching chats. The composer runtime is hoisted once for the whole
    // AI Hub surface (see AiHubRuntimeProvider in AiHub.tsx), so an unsent draft typed under chat A would
    // otherwise carry over into chat B. The first render (previousChatIdRef = undefined) doesn't clear, so
    // a draft typed on the home view survives the home→chat auto-create transition. Only call setText when
    // there's actually a draft — calling setText('') on an already-empty composer suppresses the native
    // textarea placeholder ("Send a message...").
    useEffect(() => {
        if (
            previousChatIdRef.current !== undefined &&
            previousChatIdRef.current !== chatId &&
            aui.composer.getState().text
        ) {
            aui.composer.setText('');
        }

        previousChatIdRef.current = chatId;
    }, [aui, chatId]);

    // Radix restores focus to the popover's trigger on close — the "+" button. That is right when the "+"
    // button is what opened it, and wrong when '@' did: the user was mid-sentence, and handing them back a
    // button means the next thing they type goes nowhere. Reclaim the caret for the textarea instead.
    useEffect(() => {
        if (resourcePickerOpen || atMentionTextareaRef.current == null) {
            return;
        }

        const textarea = atMentionTextareaRef.current;

        atMentionTextareaRef.current = null;

        // After Radix's own close-focus pass, or it simply overwrites this.
        requestAnimationFrame(() => textarea.focus());
    }, [resourcePickerOpen]);

    const handleKeyDown = useCallback(
        (event: KeyboardEvent<HTMLTextAreaElement>) => {
            // '@' raises the resource picker — the affordance the home panel's subtitle ("Ask anything, mention
            // files / workflows / data tables…") has always promised. Only at a word boundary: an address like
            // support@bytechef.io must stay ordinary text, and its '@' follows a letter rather than a space.
            // The keystroke is consumed rather than inserted, because here '@' is a command and not content —
            // the same bargain '/' makes in most chat composers. Workflow chats have no picker at all (their
            // messages go to a webhook, not an agent), so the key stays inert there.
            if (event.key === '@' && !isWorkflowChat) {
                const textarea = event.currentTarget;
                const caret = textarea.selectionStart;
                const atWordBoundary = caret === 0 || /\s/.test(textarea.value.charAt(caret - 1));

                if (atWordBoundary) {
                    event.preventDefault();

                    atMentionTextareaRef.current = textarea;

                    aiHubComposerStore.getState().setResourcePickerOpen(true);

                    return;
                }
            }

            if (event.key === 'Enter' && event.altKey && !event.shiftKey) {
                event.preventDefault();

                const textarea = event.currentTarget;
                const start = textarea.selectionStart;
                const end = textarea.selectionEnd;
                const currentValue = textarea.value;

                const newValue = currentValue.substring(0, start) + '\n' + currentValue.substring(end);

                const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                    window.HTMLTextAreaElement.prototype,
                    'value'
                )?.set;

                nativeInputValueSetter?.call(textarea, newValue);
                textarea.dispatchEvent(new Event('input', {bubbles: true}));

                requestAnimationFrame(() => {
                    textarea.selectionStart = start + 1;
                    textarea.selectionEnd = start + 1;
                });
            }
        },
        [isWorkflowChat]
    );

    const hasChips = referencedResources.length > 0 || selectedSkills.length > 0 || uploads.length > 0;

    return (
        // Previously had `sticky bottom-0` which caused a layout-shift on the home page: the home panel
        // centers this composer vertically along with the welcome title, but after the first paint the
        // sticky positioning re-anchored the composer to the bottom of the centered flex container,
        // bumping it up a few pixels. The composer is already the last flex child of its parent in both
        // home and panel layouts, so it sits at the bottom naturally without needing `sticky`.
        //
        // `px-2` matches the assistant message content's horizontal inset (`mx-2` in AiHubMessage) so the
        // composer's left/right edges line up with the messages above. `px-3` left the composer looking
        // narrower/indented by ~4px each side relative to the message content.
        //
        // `pb-2` (8px) matches the right resource panel's `py-2` bottom gutter (see AiHubResourcePanel) so
        // the composer's bottom edge lines up with the bottom edge of the resource panel's island card when
        // both panels are open — they're full-height siblings sharing the same bottom reference line.
        <div className={twMerge('mx-auto flex w-full max-w-[var(--thread-max-width,44rem)] flex-col gap-2 px-3 pb-2')}>
            <AiHubComposerDropZone disabled={isWorkflowChat} onFilesDropped={handleFilesDropped}>
                <ComposerPrimitive.Root className="flex w-full flex-col rounded-2xl border border-border bg-muted/40 shadow-sm focus-within:border-foreground/20">
                    {/*
                     * Chat-attached tools (persisted across turns) get their own chip row above
                     * the in-flight referenced-resources row. Sibling layout keeps the two distinct mental
                     * models — "tools the agent can use" vs. "context I'm @-mentioning right now" —
                     * visually separate. The component renders nothing when no tools are attached, so
                     * mounting it unconditionally is cheap.
                     */}

                    {chatId && currentWorkspaceId != null && (
                        <ChatToolChips chatId={chatId} workspaceId={currentWorkspaceId} />
                    )}

                    {hasChips && (
                        <div className="flex flex-wrap gap-1.5 px-3 pt-2" data-testid="reference-chips">
                            {/*
                             * Skills first in the row: they change how the agent behaves for this message,
                             * whereas the chips after them only add context to it.
                             */}

                            {selectedSkills.map((skill) => (
                                <Badge
                                    className="flex items-center gap-1 bg-lime-100 px-2 py-0.5 text-xs font-normal text-lime-700 dark:bg-lime-950 dark:text-lime-300"
                                    data-testid="skill-chip"
                                    key={`skill-${skill.id}`}
                                    styleType="outline-outline"
                                >
                                    <HexagonIcon className="size-3" />

                                    <span>{skill.name}</span>

                                    <button
                                        aria-label={`Remove ${skill.name}`}
                                        className="ml-0.5 rounded hover:opacity-75"
                                        onClick={() => handleRemoveSkill(skill.id)}
                                        type="button"
                                    >
                                        <XIcon className="size-3" />
                                    </button>
                                </Badge>
                            ))}

                            {referencedResources.map((resource) => (
                                <Badge
                                    className={`flex items-center gap-1 px-2 py-0.5 text-xs font-normal ${KIND_BADGE_CLASSES[resource.kind]}`}
                                    key={`${resource.kind}-${resource.id}`}
                                    styleType="outline-outline"
                                >
                                    <span className="font-medium">{KIND_LABELS[resource.kind]}:</span>

                                    <span>{resource.name}</span>

                                    <button
                                        aria-label={`Remove ${resource.name}`}
                                        className="ml-0.5 rounded hover:opacity-75"
                                        onClick={() => handleRemoveReference(resource.id, resource.kind)}
                                        type="button"
                                    >
                                        <XIcon className="size-3" />
                                    </button>
                                </Badge>
                            ))}

                            {uploads.map((uploadState) => {
                                const fileKey = `${uploadState.file.name}-${uploadState.file.size}-${uploadState.file.lastModified}`;

                                if (uploadState.status === 'error') {
                                    return (
                                        <Badge
                                            className="flex items-center gap-1 bg-surface-destructive-secondary px-2 py-0.5 text-xs font-normal text-content-destructive"
                                            data-testid="upload-chip-error"
                                            key={`upload-error-${fileKey}`}
                                            styleType="outline-outline"
                                        >
                                            <span className="font-medium">Failed:</span>

                                            <span>{uploadState.file.name}</span>

                                            <button
                                                aria-label={`Retry upload ${uploadState.file.name}`}
                                                className="ml-0.5 rounded hover:opacity-75"
                                                onClick={() => retry(uploadState.file)}
                                                type="button"
                                            >
                                                <RotateCcwIcon className="size-3" />
                                            </button>

                                            <button
                                                aria-label={`Dismiss upload ${uploadState.file.name}`}
                                                className="ml-0.5 rounded hover:opacity-75"
                                                onClick={() => dismiss(uploadState.file)}
                                                type="button"
                                            >
                                                <XIcon className="size-3" />
                                            </button>
                                        </Badge>
                                    );
                                }

                                return (
                                    <Badge
                                        className="flex items-center gap-1 bg-surface-brand-secondary px-2 py-0.5 text-xs font-normal text-content-brand-primary"
                                        data-testid="upload-chip-uploading"
                                        key={`upload-${fileKey}`}
                                        styleType="outline-outline"
                                    >
                                        <Loader2Icon className="size-3 animate-spin" />

                                        <span>{uploadState.file.name}</span>
                                    </Badge>
                                );
                            })}
                        </div>
                    )}

                    {isChannelBornChat ? (
                        // Read-only affordance replacing the input entirely, rather than a disabled
                        // ComposerPrimitive.Input: a channel-born row's conversation is driven by its
                        // channel, not by AI Hub, so there is nothing for a typed message here to do — the
                        // server-side WebhookBridgeAgent rejects it as an error rather than delivering it,
                        // since the row has no workflowExecutionId to bridge into. Replacing the whole
                        // control (rather than disabling it) makes that unambiguous without depending on
                        // how the primitive's own disabled state renders.
                        <div
                            className="flex items-center gap-2 px-4 py-3 text-sm text-muted-foreground"
                            data-testid="channel-born-readonly-notice"
                        >
                            <RadioTowerIcon aria-hidden className="size-4 shrink-0" />

                            <span>
                                This conversation happens on its channel (Slack, a schedule, …) — you can read it here,
                                but you can&apos;t reply from AI Hub.
                            </span>
                        </div>
                    ) : (
                        <>
                            <ComposerPrimitive.Input
                                aria-label="Message input"
                                autoFocus
                                className="max-h-32 min-h-12 w-full resize-none border-0 bg-transparent px-4 pt-2 pb-1 text-sm ring-0 outline-none placeholder:text-muted-foreground"
                                onKeyDown={handleKeyDown}
                                placeholder="Send a message..."
                                rows={1}
                            />

                            <div className="flex items-center justify-between gap-1 px-2 pb-2">
                                <div className="flex min-w-0 items-center gap-1">
                                    {modelPicker}

                                    {/*
                                     * Resource attachment is hidden for workflow chats: the resource picker
                                     * (+), the paperclip file button, and the hidden file input. A workflow chat
                                     * forwards messages to a webhook trigger rather than an LLM agent, so attached
                                     * files/resources have nowhere to flow. Drag-drop and paste are gated separately
                                     * (the drop zone's `disabled` prop and the paste effect's early return).
                                     */}

                                    {!isWorkflowChat && (
                                        <>
                                            <AiHubComposer />

                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <button
                                                        aria-label="Attach file"
                                                        className="flex size-7 items-center justify-center rounded-full text-muted-foreground hover:bg-accent hover:text-foreground"
                                                        onClick={handleAttachClick}
                                                        type="button"
                                                    >
                                                        <PaperclipIcon className="size-4" />
                                                    </button>
                                                </TooltipTrigger>

                                                <TooltipContent>Attach file</TooltipContent>
                                            </Tooltip>

                                            <AiHubSkillsMenu />

                                            <input
                                                accept={ALLOWED_MIME_TYPES.join(',')}
                                                aria-label="File upload input"
                                                className="hidden"
                                                multiple
                                                onChange={handleFileInputChange}
                                                ref={fileInputRef}
                                                type="file"
                                            />
                                        </>
                                    )}
                                </div>

                                <div className="flex shrink-0 items-center gap-2">
                                    <ModeSwitch
                                        build={mode === MODE.BUILD}
                                        onBuildChange={(build) => setMode(build ? MODE.BUILD : MODE.ASK)}
                                    />

                                    {/*
                                     * Push-to-talk dictation: records a clip and transcribes it server-side via the configured
                                     * STT provider (POST /api/platform/internal/ai/transcribe), then appends the text to the
                                     * composer draft. See usePushToTalk.
                                     */}

                                    <Button
                                        aria-label={isVoiceRecording ? 'Stop voice input' : 'Start voice input'}
                                        className="size-8 rounded-full"
                                        disabled={isVoiceTranscribing}
                                        icon={
                                            isVoiceTranscribing ? (
                                                <Loader2Icon className="size-4 animate-spin" />
                                            ) : isVoiceRecording ? (
                                                <SquareIcon className="size-3.5 animate-pulse fill-current text-content-destructive" />
                                            ) : (
                                                <MicIcon className="size-4" />
                                            )
                                        }
                                        onClick={handleMicClick}
                                        size="icon"
                                        type="button"
                                        variant="ghost"
                                    />

                                    <ThreadPrimitive.If running={false}>
                                        <ComposerPrimitive.Send asChild>
                                            <Button
                                                aria-label="Send message"
                                                className="size-8 rounded-full"
                                                icon={<ArrowUpIcon className="size-4" />}
                                                size="icon"
                                                type="submit"
                                                variant="default"
                                            />
                                        </ComposerPrimitive.Send>
                                    </ThreadPrimitive.If>

                                    <ThreadPrimitive.If running>
                                        {/*
                                         * Custom Stop button (replaces ComposerPrimitive.Cancel) so we can fire the
                                         * server-side cancelWorkflowChatTurn mutation alongside the client-side
                                         * stream cancel for workflow chats. ComposerPrimitive.Cancel
                                         * only closes the SSE stream — without the server-side stopJob call, the
                                         * workflow keeps running on the server while the UI thinks it stopped.
                                         */}

                                        <Button
                                            aria-label={isWorkflowChat ? 'Stop running workflow' : 'Stop generating'}
                                            className="size-8 rounded-full"
                                            icon={<SquareIcon className="size-3.5 fill-current" />}
                                            onClick={handleCancelTurn}
                                            size="icon"
                                            type="button"
                                            variant="default"
                                        />
                                    </ThreadPrimitive.If>
                                </div>
                            </div>
                        </>
                    )}
                </ComposerPrimitive.Root>
            </AiHubComposerDropZone>
        </div>
    );
};

export default AiHubChatComposer;
