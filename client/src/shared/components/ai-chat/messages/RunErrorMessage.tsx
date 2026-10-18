import {RUN_ERROR_CODE_NO_AI_PROVIDER} from '@/shared/components/ai-chat/messages/toRunErrorData';
import {DataMessagePartProps} from '@assistant-ui/react';
import {AlertCircleIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

const AI_PROVIDERS_SETTINGS_PATH = '/automation/settings/ai-providers';

export interface RunErrorDataI {
    code?: string;
    message: string;
}

/**
 * Renders a RUN_ERROR inline in the assistant bubble with red foreground, a left border, and an alert icon,
 * so the failure reads as a distinct system error rather than a normal assistant reply.
 */
const RunErrorMessage = ({data}: DataMessagePartProps<RunErrorDataI>) => {
    return (
        <div
            className="my-2 flex items-start gap-2 rounded-md border-l-4 border-red-500 bg-surface-destructive-secondary px-3 py-2 text-sm text-content-destructive dark:bg-red-950/40 dark:text-red-300"
            data-testid="ai-hub-run-error"
            role="alert"
        >
            <AlertCircleIcon className="mt-0.5 size-4 shrink-0" />

            <div className="flex flex-col gap-1">
                <div className="break-words whitespace-pre-wrap">{data.message}</div>

                {data.code === RUN_ERROR_CODE_NO_AI_PROVIDER && (
                    <Link className="font-medium underline" to={AI_PROVIDERS_SETTINGS_PATH}>
                        Open AI Providers
                    </Link>
                )}
            </div>
        </div>
    );
};

export default RunErrorMessage;
