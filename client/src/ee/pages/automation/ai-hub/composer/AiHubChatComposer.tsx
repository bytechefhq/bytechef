import Button from '@/components/Button/Button';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {hasPendingToolApproval} from '@/ee/pages/automation/ai-hub/chats/pendingToolApproval';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {
    aiHubRunStateStore,
    isChatRunning,
    useAiHubRunStateStore,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModeSwitch from '@/shared/components/ModeSwitch/ModeSwitch';
import {useCancelAiHubRunMutation} from '@/shared/middleware/graphql';
import {ComposerPrimitive, useAui} from '@assistant-ui/react';
import {ArrowUpIcon, SquareIcon} from 'lucide-react';
import {KeyboardEvent, ReactNode, useCallback, useEffect, useRef} from 'react';

interface AiHubChatComposerPropsI {
    /**
     * Pre-built LLM provider/model picker rendered as the first control in the composer footer. The parent owns the
     * selection state (per chat, or the home view's draft), so it passes the picker as a node.
     */
    modelPicker?: ReactNode;
}

const AiHubChatComposer = ({modelPicker}: AiHubChatComposerPropsI) => {
    const previousChatIdRef = useRef<string | undefined>(undefined);

    const chatId = useAiHubStore((state) => state.chatId);
    const mode = useAiHubStore((state) => state.mode);
    const pendingToolApproval = useAiHubStore((state) => hasPendingToolApproval(state.messages));
    const setMode = useAiHubStore((state) => state.setMode);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const isRunning = useAiHubRunStateStore((state) => isChatRunning(state, chatId));
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const cancelAiHubRunMutation = useCancelAiHubRunMutation();

    const aui = useAui();

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

    const handleKeyDown = useCallback((event: KeyboardEvent<HTMLTextAreaElement>) => {
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
    }, []);

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

        previousChatIdRef.current = chatId;
    }, [aui, chatId]);

    return (
        <div className="mx-auto flex w-full max-w-[var(--thread-max-width,44rem)] flex-col gap-2 px-3 pb-2">
            <ComposerPrimitive.Root className="flex w-full flex-col rounded-2xl border border-border bg-muted/40 shadow-sm focus-within:border-foreground/20">
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
                    <div className="flex min-w-0 items-center gap-1">{modelPicker}</div>

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
