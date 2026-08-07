import {TooltipIconButton} from '@/components/assistant-ui/tooltip-icon-button';
import {AiHubMessageComponents} from '@/ee/pages/automation/ai-hub/messages/AiHubMessage';
import AiHubSuggestionChips from '@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips';
import {useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {ThreadPrimitive} from '@assistant-ui/react';
import {ArrowDownIcon} from 'lucide-react';
import {FC} from 'react';
import {twMerge} from 'tailwind-merge';

// Shown while a task switch is fetching that task's history (messages are momentarily empty). The same
// four-dot pulse as LazyLoadWrapper's suspense fallback and AiHub's deep-link loader, so chunk load →
// task resolution → history fetch read as ONE continuous loading state — message-shaped skeletons here
// used to register as two floating gray boxes because the small user-bubble bars are barely visible.
const ThreadLoadingState: FC = () => (
    <div className="flex flex-1 items-center justify-center p-8">
        <div className="flex animate-pulse space-x-2">
            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>
        </div>
    </div>
);

// Decide what an empty thread shows. `ThreadPrimitive.If empty` gates this on the assistant-ui runtime's
// projected message list, which lags `aiHubStore.messages` by one commit: `useExternalStoreRuntime` ingests
// new messages via `runtime.setAdapter(store)` inside a `useEffect`, so the runtime still reports "empty" for
// one frame after the store has already been populated. At the tail of a task switch that leaves a window
// where `messagesLoading` has flipped false but the runtime hasn't caught up — which would flash the welcome
// copy (identical to the home page). Treating "the store already has messages" as still-loading keeps the
// skeleton on screen for that frame instead. `messagesLoading` alone covers the front of the switch (store
// cleared to [], history in flight); `hasStoreMessages` covers the tail (history landed, runtime syncing).
export function shouldShowThreadLoadingState(messagesLoading: boolean, hasStoreMessages: boolean): boolean {
    return messagesLoading || hasStoreMessages;
}

interface ThreadEmptyStateProps {
    showSuggestions: boolean;
}

// Empty thread: a loading placeholder while history is being fetched (or landing), otherwise the welcome
// prompt. Both only render when the runtime has zero messages; see shouldShowThreadLoadingState for how the
// "fetching" vs "new task" disambiguation is made without flashing the welcome mid-switch.
const ThreadEmptyState: FC<ThreadEmptyStateProps> = ({showSuggestions}) => {
    const messagesLoading = useAiHubStore((state) => state.messagesLoading);
    const hasStoreMessages = useAiHubStore((state) => state.messages.length > 0);

    if (shouldShowThreadLoadingState(messagesLoading, hasStoreMessages)) {
        return <ThreadLoadingState />;
    }

    return (
        <div className="mx-auto flex w-full max-w-[var(--thread-max-width)] flex-1 flex-col items-center justify-center px-6 py-10 text-center">
            <h1 className="animate-in text-2xl font-semibold text-foreground duration-200 fill-mode-both fade-in slide-in-from-bottom-1">
                What should we get done?
            </h1>

            <p className="mt-2 text-sm text-muted-foreground">
                Ask anything, mention files / workflows / data tables, or ask me to build a workflow.
            </p>

            {showSuggestions && <AiHubSuggestionChips className="mt-6" />}
        </div>
    );
};

// Pinned to the bottom-center of the scroll viewport. assistant-ui disables the button (→ `invisible`
// via `disabled:invisible`) whenever the viewport is already at the bottom, so it only appears once the
// user scrolls up. Ported from the shared `components/assistant-ui/thread.tsx` so AI Hub matches the
// Copilot's scroll affordance instead of leaving the user with no way back to the latest message.
const ThreadScrollToBottom: FC = () => (
    <ThreadPrimitive.ScrollToBottom asChild>
        <TooltipIconButton
            className="aui-cc-thread-scroll-to-bottom sticky bottom-2 z-10 size-9 self-center rounded-full bg-background p-2 shadow-sm disabled:invisible dark:border-border dark:bg-background dark:hover:bg-accent"
            tooltip="Scroll to bottom"
            variant="outline"
        >
            <ArrowDownIcon className="size-4" />
        </TooltipIconButton>
    </ThreadPrimitive.ScrollToBottom>
);

interface AiHubThreadProps {
    // Reserves room on the right for the floating artifacts card. The padding sits INSIDE the scroll
    // viewport rather than on an ancestor, which is what keeps the scrollbar pinned to the pane's edge —
    // padding an ancestor would narrow the scroller itself and drag the scrollbar inward with it.
    // AiHubPanel drives this off the same visibility hook the card reads, and applies the matching inset
    // to the composer so the two keep a shared right edge.
    contentInsetRight?: boolean;
    // Sample-question chips only make sense for plain copilot tasks — WORKFLOW_CHAT routes to a webhook
    // trigger and PERSONAL_AGENT conversations carry their own purpose, so the panel gates them off there.
    showSuggestions?: boolean;
}

const AiHubThread: FC<AiHubThreadProps> = ({contentInsetRight = false, showSuggestions = true}) => {
    return (
        <ThreadPrimitive.Root
            className="aui-cc-thread-root @container flex h-full flex-col"
            style={{['--thread-max-width' as string]: '44rem'}}
        >
            <ThreadPrimitive.Viewport
                className={twMerge(
                    'aui-cc-thread-viewport relative mx-1 flex flex-1 flex-col overflow-x-hidden overflow-y-auto',
                    // Eased so the transcript slides aside as the card appears rather than jumping the
                    // instant the first artifact lands mid-turn.
                    'transition-[padding] duration-200 ease-in-out',
                    contentInsetRight && 'pr-68'
                )}
            >
                <ThreadPrimitive.If empty>
                    <ThreadEmptyState showSuggestions={showSuggestions} />
                </ThreadPrimitive.If>

                <ThreadPrimitive.Messages components={AiHubMessageComponents} />

                <ThreadPrimitive.If empty={false}>
                    <div className="min-h-4 grow" />

                    <ThreadScrollToBottom />
                </ThreadPrimitive.If>
            </ThreadPrimitive.Viewport>
        </ThreadPrimitive.Root>
    );
};

export default AiHubThread;
