import {TooltipIconButton} from '@/components/assistant-ui/tooltip-icon-button';
import AiHubMessageContent from '@/ee/pages/automation/ai-hub/messages/AiHubMessageContent';
import {ActionBarPrimitive, ComposerPrimitive, MessagePrimitive, useAuiState} from '@assistant-ui/react';
import {CheckIcon, CopyIcon, PencilIcon, RefreshCwIcon} from 'lucide-react';
import {FC} from 'react';

/**
 * Marks the platform-generated prompt that restarts an agent turn after a tool approval is resolved. Nobody typed it,
 * so it renders as a slim status line rather than a user bubble.
 */
const TOOL_APPROVAL_STATUS_PREFIX = '[tool-approval #';

/**
 * Edit composer for user messages. The runtime provider's onEdit does the work (server-side truncate, local rewind,
 * re-run); this is the inline textarea with its send / cancel chrome.
 */
const AiHubEditComposer: FC = () => (
    <div className="aui-cc-edit-composer-wrapper mx-auto flex w-full max-w-[var(--thread-max-width)] flex-col gap-4 px-2 first:mt-4">
        <ComposerPrimitive.Root className="aui-cc-edit-composer-root ml-auto flex w-full max-w-[87.5%] flex-col rounded-xl bg-muted">
            <ComposerPrimitive.Input
                autoFocus
                className="aui-cc-edit-composer-input flex min-h-[60px] w-full resize-none border-0 bg-transparent p-4 text-foreground ring-0 outline-none"
            />

            <div className="flex justify-end gap-2 p-2">
                <ComposerPrimitive.Cancel asChild>
                    <button className="rounded-md border border-input px-3 py-1 text-xs hover:bg-accent" type="button">
                        Cancel
                    </button>
                </ComposerPrimitive.Cancel>

                <ComposerPrimitive.Send asChild>
                    <button
                        className="rounded-md bg-primary px-3 py-1 text-xs text-primary-foreground hover:opacity-90"
                        type="button"
                    >
                        Save and resend
                    </button>
                </ComposerPrimitive.Send>
            </div>
        </ComposerPrimitive.Root>
    </div>
);

const AiHubUserActionBar: FC = () => (
    <ActionBarPrimitive.Root
        autohide="not-last"
        className="aui-cc-user-action-bar flex flex-col items-end"
        hideWhenRunning
    >
        <ActionBarPrimitive.Edit asChild>
            <TooltipIconButton className="aui-cc-user-action-edit p-2" tooltip="Edit">
                <PencilIcon />
            </TooltipIconButton>
        </ActionBarPrimitive.Edit>
    </ActionBarPrimitive.Root>
);

const AiHubAssistantActionBar: FC = () => (
    <ActionBarPrimitive.Root
        autohide="not-last"
        className="aui-cc-assistant-action-bar flex gap-1 text-muted-foreground"
        hideWhenRunning
    >
        <ActionBarPrimitive.Copy asChild>
            <TooltipIconButton tooltip="Copy">
                <MessagePrimitive.If copied>
                    <CheckIcon />
                </MessagePrimitive.If>

                <MessagePrimitive.If copied={false}>
                    <CopyIcon />
                </MessagePrimitive.If>
            </TooltipIconButton>
        </ActionBarPrimitive.Copy>

        <ActionBarPrimitive.Reload asChild>
            <TooltipIconButton tooltip="Refresh">
                <RefreshCwIcon />
            </TooltipIconButton>
        </ActionBarPrimitive.Reload>
    </ActionBarPrimitive.Root>
);

const AiHubUserMessage: FC = () => {
    const content = useAuiState((state) => state.message.content);

    const firstTextPart = content.find((part) => part.type === 'text');
    const firstTextValue = firstTextPart && 'text' in firstTextPart ? firstTextPart.text : undefined;

    if (firstTextValue?.startsWith(TOOL_APPROVAL_STATUS_PREFIX)) {
        const closingBracketIndex = firstTextValue.indexOf(']');
        const statusLine =
            closingBracketIndex >= 0 ? firstTextValue.slice(1, closingBracketIndex) : firstTextValue.slice(1);

        return (
            <div
                className="mx-auto w-full max-w-[var(--thread-max-width)] px-3 py-1 text-xs text-muted-foreground"
                data-testid="tool-approval-status-line"
            >
                {statusLine}
            </div>
        );
    }

    return (
        <MessagePrimitive.Root asChild>
            <div
                className="aui-cc-user-message mx-auto grid w-full max-w-[var(--thread-max-width)] auto-rows-auto grid-cols-[minmax(72px,1fr)_auto] gap-y-2 px-3 py-3 first:mt-3 last:mb-2 [&:where(>*)]:col-start-2"
                data-role="user"
            >
                <div className="relative col-start-2 min-w-0">
                    <div className="rounded-2xl bg-muted px-4 py-2 text-sm break-words text-foreground">
                        <AiHubMessageContent />
                    </div>

                    <div className="absolute top-1/2 left-0 -translate-x-full -translate-y-1/2 pr-2">
                        <AiHubUserActionBar />
                    </div>
                </div>
            </div>
        </MessagePrimitive.Root>
    );
};

const AiHubAssistantMessage: FC = () => (
    <MessagePrimitive.Root asChild>
        <div
            className="aui-cc-assistant-message relative mx-auto w-full max-w-[var(--thread-max-width)] py-3 last:mb-12"
            data-role="assistant"
        >
            <div className="mx-3 text-sm leading-7 break-words text-foreground">
                <AiHubMessageContent />
            </div>

            <div className="mx-2 -mb-6 min-h-6">
                <AiHubAssistantActionBar />
            </div>
        </div>
    </MessagePrimitive.Root>
);

export const AiHubMessageComponents = {
    AssistantMessage: AiHubAssistantMessage,
    EditComposer: AiHubEditComposer,
    UserMessage: AiHubUserMessage,
};

export default AiHubAssistantMessage;
