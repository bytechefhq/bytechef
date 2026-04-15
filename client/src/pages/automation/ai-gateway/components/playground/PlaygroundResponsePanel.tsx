import {PlaygroundChatCompletionMutation} from '@/shared/middleware/graphql';
import {ActivityIcon, ClockIcon, CoinsIcon, HashIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

type PlaygroundResponseType = NonNullable<PlaygroundChatCompletionMutation['playgroundChatCompletion']>;

interface PlaygroundResponsePanelProps {
    className?: string;
    isLoading: boolean;
    response: PlaygroundResponseType | undefined;
}

const PlaygroundResponsePanel = ({className, isLoading, response}: PlaygroundResponsePanelProps) => {
    if (isLoading) {
        return (
            <div className={twMerge('flex items-center justify-center rounded-lg border p-8', className)}>
                <div className="flex items-center gap-2 text-muted-foreground">
                    <ActivityIcon className="size-4 animate-pulse" />

                    <span className="text-sm">Generating response...</span>
                </div>
            </div>
        );
    }

    if (!response) {
        return (
            <div className={twMerge('flex items-center justify-center rounded-lg border p-8', className)}>
                <span className="text-sm text-muted-foreground">Response will appear here</span>
            </div>
        );
    }

    return (
        <div className={twMerge('rounded-lg border', className)}>
            <div className="flex flex-wrap gap-4 border-b px-4 py-2 text-xs text-muted-foreground">
                {response.model && (
                    <span className="flex items-center gap-1">
                        <HashIcon className="size-3" />

                        {response.model}
                    </span>
                )}

                {response.latencyMs != null && (
                    <span className="flex items-center gap-1">
                        <ClockIcon className="size-3" />

                        <span>{response.latencyMs}ms</span>
                    </span>
                )}

                {(response.promptTokens != null || response.completionTokens != null) && (
                    <span className="flex items-center gap-1">
                        <ActivityIcon className="size-3" />

                        <span>
                            {`${response.promptTokens ?? 0} in / ${response.completionTokens ?? 0} out`}

                            {response.totalTokens != null && ` (${response.totalTokens} total)`}
                        </span>
                    </span>
                )}

                {response.cost != null && (
                    <span className="flex items-center gap-1">
                        <CoinsIcon className="size-3" />

                        <span>${Number(response.cost).toFixed(4)}</span>
                    </span>
                )}

                {response.finishReason && (
                    <span className="rounded bg-muted px-1.5 py-0.5">{response.finishReason}</span>
                )}

                {response.traceId && <span className="text-primary">Trace #{response.traceId}</span>}
            </div>

            <div className="whitespace-pre-wrap p-4 text-sm">
                {response.content || <span className="text-muted-foreground">Empty response</span>}
            </div>
        </div>
    );
};

export default PlaygroundResponsePanel;
