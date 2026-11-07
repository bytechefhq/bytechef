import {ToolCallEntryI} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {AlertCircleIcon, ChevronDownIcon, ChevronRightIcon, Loader2Icon} from 'lucide-react';
import {useState} from 'react';

interface AiHubSubagentProgressProps {
    entry: Pick<ToolCallEntryI, 'progress' | 'progressLabel' | 'progressOutcome' | 'status'>;
    fallbackText?: string;
}

const AiHubSubagentProgress = ({entry, fallbackText}: AiHubSubagentProgressProps) => {
    const [showEarlierSteps, setShowEarlierSteps] = useState(false);

    const {progress, progressLabel, progressOutcome, status} = entry;
    const latestStep = progress.at(-1)?.text;

    if (progressOutcome === 'failed') {
        return (
            <div className="mt-2 flex items-start gap-2 text-sm" role="status">
                <AlertCircleIcon className="mt-0.5 size-4 shrink-0 text-content-destructive-primary" />

                <span className="flex min-w-0 flex-col">
                    <span className="text-content-destructive-primary">Failed</span>

                    {progressLabel && <span className="truncate text-xs text-muted-foreground">{progressLabel}</span>}

                    {latestStep && <span className="truncate text-xs text-muted-foreground">at {latestStep}</span>}
                </span>
            </div>
        );
    }

    if (status !== 'running' || progressOutcome === 'done') {
        return null;
    }

    const prominentText = latestStep ?? fallbackText ?? progressLabel;

    if (!prominentText) {
        return null;
    }

    const earlierSteps = progress.slice(0, -1);

    return (
        <div className="mt-2 flex items-start gap-2 text-sm" role="status">
            <Loader2Icon className="mt-0.5 size-4 shrink-0 animate-spin text-muted-foreground" />

            <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                <span className="truncate font-medium text-foreground">{prominentText}</span>

                {progressLabel && progressLabel !== prominentText && (
                    <span className="truncate text-xs text-muted-foreground">{progressLabel}</span>
                )}

                {earlierSteps.length > 0 && (
                    <button
                        aria-expanded={showEarlierSteps}
                        className="flex items-center gap-1 self-start text-xs text-muted-foreground hover:text-foreground"
                        onClick={() => setShowEarlierSteps((previousShowEarlierSteps) => !previousShowEarlierSteps)}
                        type="button"
                    >
                        {showEarlierSteps ? (
                            <ChevronDownIcon className="size-3" />
                        ) : (
                            <ChevronRightIcon className="size-3" />
                        )}

                        {earlierSteps.length === 1 ? '1 earlier step' : `${earlierSteps.length} earlier steps`}
                    </button>
                )}

                {showEarlierSteps && (
                    <ol className="ml-4 flex list-none flex-col gap-0.5 text-xs text-muted-foreground">
                        {earlierSteps.map((step, index) => (
                            <li className="truncate" key={`${index}-${step.timestamp}`}>
                                {step.text}
                            </li>
                        ))}
                    </ol>
                )}
            </span>
        </div>
    );
};

export default AiHubSubagentProgress;
