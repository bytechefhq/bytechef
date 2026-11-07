import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {isChannelAgentChat, isWebhookBridgedChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import useCurrentAiHubChat from '@/ee/pages/automation/ai-hub/chats/hooks/useCurrentAiHubChat';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {hasPendingToolApproval} from '@/ee/pages/automation/ai-hub/chats/pendingToolApproval';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubComposer from '@/ee/pages/automation/ai-hub/composer/AiHubComposer';
import AiHubComposerDropZone from '@/ee/pages/automation/ai-hub/composer/AiHubComposerDropZone';
import AiHubSkillsMenu from '@/ee/pages/automation/ai-hub/composer/AiHubSkillsMenu';
import {
    ALLOWED_MIME_TYPES,
    buildFileKey,
    useAiHubAttachmentUpload,
} from '@/ee/pages/automation/ai-hub/composer/hooks/useAiHubAttachmentUpload';
import {
    ReferencedResourceKindType,
    aiHubComposerStore,
    useAiHubComposerStore,
} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {
    aiHubRunStateStore,
    isChatRunning,
    useAiHubRunStateStore,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTabsStore, getTabGenericId} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import ChatToolChips from '@/ee/pages/automation/ai-hub/tools/ChatToolChips';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModeSwitch from '@/shared/components/ModeSwitch/ModeSwitch';
import {usePushToTalk} from '@/shared/hooks/usePushToTalk';
import {useCancelAiHubRunMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ComposerPrimitive, useAui} from '@assistant-ui/react';
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
import {ChangeEvent, KeyboardEvent, ReactNode, useCallback, useEffect, useRef} from 'react';

const KIND_LABELS: Record<ReferencedResourceKindType, string> = {
    aiAgent: 'AI Agent',
    file: 'File',
    workflow: 'Workflow',
};

const KIND_BADGE_CLASSES: Record<ReferencedResourceKindType, string> = {
    aiAgent: 'bg-violet-100 text-violet-700 dark:bg-violet-950 dark:text-violet-300',
    file: 'bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300',
    workflow: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300',
};

interface AiHubChatComposerPropsI {
    /**
     * Pre-built LLM provider/model picker rendered as the first control in the composer footer. The parent owns the
     * selection state (per chat, or the home view's draft), so it passes the picker as a node, and passes none for a
     * workflow chat.
     */
    modelPicker?: ReactNode;
}

const AiHubChatComposer = ({modelPicker}: AiHubChatComposerPropsI) => {
    const atMentionTextareaRef = useRef<HTMLTextAreaElement | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const previousChatIdRef = useRef<string | undefined>(undefined);

    const referencedResources = useAiHubComposerStore((state) => state.referencedResources);
    const resourcePickerOpen = useAiHubComposerStore((state) => state.resourcePickerOpen);
    const selectedSkills = useAiHubComposerStore((state) => state.selectedSkills);
    const chatId = useAiHubStore((state) => state.chatId);
    const mode = useAiHubStore((state) => state.mode);
    const pendingToolApproval = useAiHubStore((state) => hasPendingToolApproval(state.messages));
    const setMode = useAiHubStore((state) => state.setMode);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const isRunning = useAiHubRunStateStore((state) => isChatRunning(state, chatId));
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {dismiss, retry, upload, uploads} = useAiHubAttachmentUpload(currentWorkspaceId, currentEnvironmentId);

    const currentChat = useCurrentAiHubChat();

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

    const isWorkflowChat = isWebhookBridgedChat(currentChat?.kind);

    const handleMicClick = useCallback(() => {
        if (isVoiceRecording) {
            stopPushToTalk();
        } else {
            void startPushToTalk();
        }
    }, [isVoiceRecording, startPushToTalk, stopPushToTalk]);

    const handleCancelTurn = useCallback(() => {
        const runId = chatId != null ? aiHubRunStateStore.getState().runIdByChat[chatId] : undefined;

        aui.thread.cancelRun();

        if (currentChatId == null || currentWorkspaceId == null) {
            return;
        }

        cancelAiHubRunMutation.mutate({
            id: String(currentChatId),
            runId,
            workspaceId: String(currentWorkspaceId),
        });
    }, [aui, cancelAiHubRunMutation, chatId, currentChatId, currentWorkspaceId]);

    const handleAttachClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileInputChange = (event: ChangeEvent<HTMLInputElement>) => {
        const selectedFiles = event.target.files;

        if (selectedFiles && selectedFiles.length > 0) {
            upload(Array.from(selectedFiles));
        }

        event.target.value = '';
    };

    const handleRemoveReference = (id: string, kind: ReferencedResourceKindType) => {
        const composerState = aiHubComposerStore.getState();

        const removedResource = composerState.referencedResources.find(
            (resource) => resource.id === id && resource.kind === kind
        );

        composerState.removeReference(id, kind);

        if (removedResource?.tabId == null || !removedResource.ownsTab) {
            return;
        }

        const tabsState = aiHubTabsStore.getState();

        const attachedTab = tabsState.openTabs.find((tab) => tab.id === removedResource.tabId);

        if (attachedTab && getTabGenericId(attachedTab) === removedResource.id) {
            tabsState.closeTab(attachedTab.id);
        }
    };

    const handleRemoveSkill = (id: string) => {
        aiHubComposerStore.getState().removeSkill(id);
    };

    const handleKeyDown = useCallback(
        (event: KeyboardEvent<HTMLTextAreaElement>) => {
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

            if (event.key !== 'Enter' || !event.altKey || event.shiftKey) {
                return;
            }

            event.preventDefault();

            const textarea = event.currentTarget;
            const selectionStart = textarea.selectionStart;
            const currentValue = textarea.value;

            const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                window.HTMLTextAreaElement.prototype,
                'value'
            )?.set;

            nativeInputValueSetter?.call(
                textarea,
                currentValue.substring(0, selectionStart) + '\n' + currentValue.substring(textarea.selectionEnd)
            );

            textarea.dispatchEvent(new Event('input', {bubbles: true}));

            requestAnimationFrame(() => {
                textarea.selectionStart = selectionStart + 1;
                textarea.selectionEnd = selectionStart + 1;
            });
        },
        [isWorkflowChat]
    );

    const isWorkspaceReady = isWorkspaceIdKnown(currentWorkspaceId);

    const hasChips = referencedResources.length > 0 || selectedSkills.length > 0 || uploads.length > 0;

    const placeholder = pendingToolApproval
        ? 'Approve or reject the tool call above, or send a message to withdraw it...'
        : 'Send a message...';

    useEffect(() => {
        if (
            previousChatIdRef.current !== undefined &&
            previousChatIdRef.current !== chatId &&
            aui.composer.getState().text
        ) {
            aui.composer.setText('');
        }

        if (previousChatIdRef.current !== undefined && previousChatIdRef.current !== chatId) {
            aiHubComposerStore.getState().clear();
        }

        previousChatIdRef.current = chatId;
    }, [aui, chatId]);

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

    useEffect(() => {
        if (resourcePickerOpen || atMentionTextareaRef.current == null) {
            return;
        }

        const textarea = atMentionTextareaRef.current;

        atMentionTextareaRef.current = null;

        requestAnimationFrame(() => textarea.focus());
    }, [resourcePickerOpen]);

    if (currentChat != null && isChannelAgentChat(currentChat)) {
        return (
            <div className="mx-auto flex w-full max-w-[var(--thread-max-width,44rem)] flex-col gap-2 px-3 pb-2">
                <div
                    className="flex items-center gap-2 rounded-2xl border border-border px-4 py-3 text-sm text-muted-foreground"
                    data-testid="channel-born-readonly-notice"
                >
                    <RadioTowerIcon aria-hidden className="size-4 shrink-0" />

                    <span>
                        This conversation happens on the agent&apos;s channel. You can read it here, but you can&apos;t
                        reply from the AI Hub.
                    </span>
                </div>
            </div>
        );
    }

    return (
        <AiHubComposerDropZone
            className="mx-auto flex w-full max-w-[var(--thread-max-width,44rem)] flex-col gap-2 px-3 pb-2"
            disabled={isWorkflowChat}
            onFilesDropped={upload}
        >
            <ComposerPrimitive.Root className="flex w-full flex-col rounded-2xl border border-border bg-muted/40 shadow-sm focus-within:border-foreground/20">
                {chatId && currentWorkspaceId != null && (
                    <ChatToolChips chatId={chatId} workspaceId={currentWorkspaceId} />
                )}

                {hasChips && (
                    <div className="flex flex-wrap gap-1.5 px-3 pt-2" data-testid="reference-chips">
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
                            const fileKey = buildFileKey(uploadState.file);

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

                            if (uploadState.status === 'success') {
                                return null;
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

                <ComposerPrimitive.Input
                    aria-label="Message input"
                    autoFocus
                    className="max-h-32 min-h-12 w-full resize-none border-0 bg-transparent px-4 pt-2 pb-1 text-sm ring-0 outline-none placeholder:text-muted-foreground"
                    disabled={isRunning || !isWorkspaceReady}
                    onKeyDown={handleKeyDown}
                    placeholder={placeholder}
                    rows={1}
                />

                <div className="flex items-center justify-between gap-1 px-2 pb-2">
                    <div className="flex min-w-0 items-center gap-1">
                        {modelPicker}

                        {!isWorkflowChat && (
                            <>
                                <AiHubComposer />

                                <AiHubSkillsMenu />

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <button
                                            aria-label="Attach file"
                                            className="flex size-7 items-center justify-center rounded-full text-muted-foreground hover:bg-accent hover:text-foreground"
                                            disabled={!isWorkspaceReady}
                                            onClick={handleAttachClick}
                                            type="button"
                                        >
                                            <PaperclipIcon className="size-4" />
                                        </button>
                                    </TooltipTrigger>

                                    <TooltipContent>Attach file</TooltipContent>
                                </Tooltip>

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

                        {isRunning ? (
                            <Button
                                aria-label="Stop generating"
                                className="size-8 rounded-full"
                                icon={<SquareIcon className="size-3.5 fill-current" />}
                                onClick={handleCancelTurn}
                                size="icon"
                                type="button"
                                variant="default"
                            />
                        ) : (
                            <ComposerPrimitive.Send asChild>
                                <Button
                                    aria-label="Send message"
                                    className="size-8 rounded-full"
                                    disabled={!isWorkspaceReady}
                                    icon={<ArrowUpIcon className="size-4" />}
                                    size="icon"
                                    type="submit"
                                    variant="default"
                                />
                            </ComposerPrimitive.Send>
                        )}
                    </div>
                </div>
            </ComposerPrimitive.Root>
        </AiHubComposerDropZone>
    );
};

export default AiHubChatComposer;
