import {TooltipIconButton} from '@/components/assistant-ui/tooltip-icon-button';
import {AiHubMessageComponents} from '@/ee/pages/automation/ai-hub/messages/AiHubMessage';
import AiHubSuggestionChips from '@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips';
import {useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {ThreadPrimitive} from '@assistant-ui/react';
import {ArrowDownIcon} from 'lucide-react';
import {FC} from 'react';
import {twMerge} from 'tailwind-merge';

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

/**
 * Decides what an empty thread shows. The assistant-ui runtime ingests store messages in an effect, so it still
 * reports "empty" for one commit after the store is populated; treating "the store already has messages" as loading
 * keeps the welcome copy from flashing at the tail of a chat switch, while {@code messagesLoading} covers its front.
 */
export function shouldShowThreadLoadingState(messagesLoading: boolean, hasStoreMessages: boolean): boolean {
    return messagesLoading || hasStoreMessages;
}

interface ThreadEmptyStateProps {
    showSuggestions: boolean;
}

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
                Ask about your projects and workflows, or ask me to build a workflow.
            </p>

            {showSuggestions && <AiHubSuggestionChips className="mt-6" />}
        </div>
    );
};

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
    /** Reserves room on the right for the floating artifacts card, inside the scroll viewport. */
    contentInsetRight?: boolean;
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
