import {DataMessagePartProps} from '@assistant-ui/react';
import {AlertCircleIcon} from 'lucide-react';

export interface RunErrorDataI {
    /**
     * Human-readable error text. The runtime provider's onRunErrorEvent already runs the raw RUN_ERROR
     * payload through {@code humanizeAgentErrorMessage} to strip Java exception FQCNs / unwrap Anthropic
     * + OpenAI JSON envelopes before this renderer ever sees it — so this string is safe to display
     * verbatim. Multi-line content renders with whitespace-preserved formatting via {@code whitespace-pre-wrap}.
     */
    message: string;
}

/**
 * Renders a RUN_ERROR inline in the assistant bubble using red foreground + a left border + an alert icon,
 * so the failure reads as a distinct system error rather than as the assistant's reply. Previously the error
 * was written into the regular text content via {@code appendToLastAssistantMessage} with a bold "Error:"
 * prefix; that rendered in the same colour as a normal reply and a user skimming the transcript could
 * easily miss it.
 */
const RunErrorMessage = ({data}: DataMessagePartProps<RunErrorDataI>) => {
    return (
        <div
            className="my-2 flex items-start gap-2 rounded-md border-l-4 border-red-500 bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300"
            data-testid="ai-hub-run-error"
            role="alert"
        >
            <AlertCircleIcon className="mt-0.5 size-4 shrink-0" />

            <div className="break-words whitespace-pre-wrap">{data.message}</div>
        </div>
    );
};

export default RunErrorMessage;
