import GettingStartedStatusBox from '@/components/GettingStarted/GettingStartedStatusBox';
import WorkflowExecutionContentClipboardButton from '@/shared/components/workflow-executions/WorkflowExecutionContentClipboardButton';
import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

type LastFailedCallType = EmbeddedGettingStartedQuery['embeddedGettingStarted']['lastFailedCall'];

interface FirstCallStepContentProps {
    callReceived: boolean;
    curl: string;
    lastFailedCall: LastFailedCallType;
    listening: boolean;
    muted: boolean;
}

const FirstCallStepContent = ({callReceived, curl, lastFailedCall, listening, muted}: FirstCallStepContentProps) => (
    <div className="mt-2 space-y-3">
        <div
            className={twMerge(
                'relative rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary',
                muted && 'opacity-50'
            )}
        >
            <pre
                className="max-h-72 overflow-auto p-3 pr-10 text-xs break-all whitespace-pre-wrap text-content-neutral-primary"
                data-testid="first-call-curl"
            >
                {curl}
            </pre>

            <div className="absolute top-2 right-2">
                <WorkflowExecutionContentClipboardButton value={curl} />
            </div>
        </div>

        {callReceived && (
            <GettingStartedStatusBox data-testid="call-received" title="Call received" tone="success">
                <p className="text-xs text-content-neutral-secondary">
                    Your first call succeeded in Development. Setup is complete.
                </p>

                <Link
                    className="text-xs font-medium text-content-brand-primary hover:underline"
                    to="/embedded/external-invocations"
                >
                    View external invocations
                </Link>
            </GettingStartedStatusBox>
        )}

        {listening && (
            <GettingStartedStatusBox
                indicatorTestId="listening-indicator"
                title="Listening for the call"
                tone={muted ? 'idle' : 'waiting'}
            >
                <p className="text-xs text-content-neutral-secondary">
                    A call to Development finishes setup. You can leave this page; we&apos;ll remember.
                </p>

                {lastFailedCall && (
                    <div className="space-y-1 pt-1 text-xs" data-testid="last-failed-call">
                        <p className="text-content-destructive-primary">
                            {`Calls are arriving, but none has succeeded yet. Latest: ${[lastFailedCall.status, lastFailedCall.errorMessage].filter(Boolean).join(' - ')}`}
                        </p>

                        <Link
                            className="font-medium text-content-brand-primary hover:underline"
                            to="/embedded/external-invocations"
                        >
                            View external invocations
                        </Link>
                    </div>
                )}
            </GettingStartedStatusBox>
        )}
    </div>
);

export default FirstCallStepContent;
