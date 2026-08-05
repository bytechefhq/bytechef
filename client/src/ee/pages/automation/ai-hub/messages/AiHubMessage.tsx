import {TooltipIconButton} from '@/components/assistant-ui/tooltip-icon-button';
import AiHubMessageContent from '@/ee/pages/automation/ai-hub/messages/AiHubMessageContent';
import {ActionBarPrimitive, ComposerPrimitive, MessagePrimitive} from '@assistant-ui/react';
import {CheckIcon, CopyIcon, PencilIcon, RefreshCwIcon} from 'lucide-react';
import {FC} from 'react';

/**
 * Edit composer for user messages — replaces the previous AiHubUserMessageEditor wrapper. The runtime
 * provider's onEdit handler does the heavy lifting (server-side chat-memory truncate, local store + agent
 * transcript rewind, re-run); this component is just the inline textarea + send/cancel chrome assistant-ui
 * renders inside `MessagePrimitive.If editing`.
 *
 * <p>border-0 + ring-0 are required to defeat the global `* { @apply border-border }` rule in
 * styles/components.css that would otherwise paint a visible UA-default border around the textarea on focus.</p>
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

/**
 * Hover-revealed pencil button on user messages. Clicking it triggers assistant-ui's beginEdit which swaps the
 * bubble for the EditComposer above. Wired through the runtime's onEdit adapter in AiHubRuntimeProvider.
 */
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

/**
 * Action bar floating below assistant messages with Copy + Refresh. Copy reads the message text directly via
 * the assistant-ui primitive (no runtime adapter needed). Refresh fires the runtime's onReload, which truncates
 * the chat-memory + transcript and re-runs the agent against the preceding user message.
 */
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

const AiHubUserMessage: FC = () => (
    <MessagePrimitive.Root asChild>
        <div
            className="aui-cc-user-message mx-auto grid w-full max-w-[var(--thread-max-width)] auto-rows-auto grid-cols-[minmax(72px,1fr)_auto] gap-y-2 px-3 py-3 first:mt-3 last:mb-2 [&:where(>*)]:col-start-2"
            data-role="user"
        >
            <div className="relative col-start-2 min-w-0">
                <div className="rounded-2xl bg-muted px-4 py-2 text-sm break-words text-foreground">
                    <AiHubMessageContent />
                </div>

                {/*
                 * Hover-revealed pencil button. Clicking it triggers assistant-ui's beginEdit which swaps
                 * the message in the thread for the EditComposer below. The save handler is wired through
                 * the runtime provider's onEdit adapter which truncates chat-memory + re-runs the agent.
                 */}

                <div className="absolute top-1/2 left-0 -translate-x-full -translate-y-1/2 pr-2">
                    <AiHubUserActionBar />
                </div>
            </div>
        </div>
    </MessagePrimitive.Root>
);

const AiHubAssistantMessage: FC = () => (
    <MessagePrimitive.Root asChild>
        <div
            className="aui-cc-assistant-message relative mx-auto w-full max-w-[var(--thread-max-width)] py-3 last:mb-12"
            data-role="assistant"
        >
            <div className="mx-3 text-sm leading-7 break-words text-foreground">
                <AiHubMessageContent />
            </div>

            {/*
             * Inline action-bar footer. Reserves exactly the bar's own height (`min-h-6` = the 24px
             * `size-6` icon buttons) and collapses that reservation back with a matching negative margin
             * (`-mb-6`) so revealing the bar on hover doesn't shift surrounding messages. NO top margin: the
             * gap is owned by the preceding content, and EVERY terminal content type (tool card, picker,
             * connect button, …) uses top-margin-only spacing (no bottom margin), so the bar sits uniformly
             * flush beneath all of them. A footer `-mt` was tried but couldn't serve both card and picker
             * (different trailing margins) — normalizing the content is what makes the spacing consistent.
             * The bar must stay IN FLOW (no `autohideFloat`/absolute): a floated bar detaches from the
             * message and opens a hover dead-zone, so the buttons vanish as the cursor moves toward them.
             */}

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
