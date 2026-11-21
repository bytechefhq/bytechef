import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {isWebhookBridgedChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import useCurrentAiHubChat from '@/ee/pages/automation/ai-hub/chats/hooks/useCurrentAiHubChat';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {hasPendingToolApproval} from '@/ee/pages/automation/ai-hub/chats/pendingToolApproval';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import AiHubComposer from '@/ee/pages/automation/ai-hub/composer/AiHubComposer';
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
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModeSwitch from '@/shared/components/ModeSwitch/ModeSwitch';
import {useCancelAiHubRunMutation} from '@/shared/middleware/graphql';
import {ComposerPrimitive, useAui} from '@assistant-ui/react';
import {ArrowUpIcon, SquareIcon, XIcon} from 'lucide-react';
import {KeyboardEvent, ReactNode, useCallback, useEffect, useRef} from 'react';

const KIND_LABELS: Record<ReferencedResourceKindType, string> = {
    workflow: 'Workflow',
};

const KIND_BADGE_CLASSES: Record<ReferencedResourceKindType, string> = {
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
    const previousChatIdRef = useRef<string | undefined>(undefined);

    const referencedResources = useAiHubComposerStore((state) => state.referencedResources);
    const resourcePickerOpen = useAiHubComposerStore((state) => state.resourcePickerOpen);
    const chatId = useAiHubStore((state) => state.chatId);
    const mode = useAiHubStore((state) => state.mode);
    const pendingToolApproval = useAiHubStore((state) => hasPendingToolApproval(state.messages));
    const setMode = useAiHubStore((state) => state.setMode);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const isRunning = useAiHubRunStateStore((state) => isChatRunning(state, chatId));
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const currentChat = useCurrentAiHubChat();

    const cancelAiHubRunMutation = useCancelAiHubRunMutation();

    const aui = useAui();

    const isWorkflowChat = isWebhookBridgedChat(currentChat?.kind);

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

        if (attachedTab?.workflowId === removedResource.id) {
            tabsState.closeTab(attachedTab.id);
        }
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
        if (resourcePickerOpen || atMentionTextareaRef.current == null) {
            return;
        }

        const textarea = atMentionTextareaRef.current;

        atMentionTextareaRef.current = null;

        requestAnimationFrame(() => textarea.focus());
    }, [resourcePickerOpen]);

    return (
        <div className="mx-auto flex w-full max-w-[var(--thread-max-width,44rem)] flex-col gap-2 px-3 pb-2">
            <ComposerPrimitive.Root className="flex w-full flex-col rounded-2xl border border-border bg-muted/40 shadow-sm focus-within:border-foreground/20">
                {referencedResources.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 px-3 pt-2" data-testid="reference-chips">
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

                        {!isWorkflowChat && <AiHubComposer />}
                    </div>

                    <div className="flex shrink-0 items-center gap-2">
                        <ModeSwitch
                            build={mode === MODE.BUILD}
                            onBuildChange={(build) => setMode(build ? MODE.BUILD : MODE.ASK)}
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
        </div>
    );
};

export default AiHubChatComposer;
