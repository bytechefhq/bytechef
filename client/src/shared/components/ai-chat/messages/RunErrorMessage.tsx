import {DataMessagePartProps} from '@assistant-ui/react';
import {AlertCircleIcon} from 'lucide-react';

export interface RunErrorDataI {
    message: string;
}

/**
 * Renders a RUN_ERROR inline in the assistant bubble with red foreground, a left border, and an alert icon,
 * so the failure reads as a distinct system error rather than a normal assistant reply.
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
