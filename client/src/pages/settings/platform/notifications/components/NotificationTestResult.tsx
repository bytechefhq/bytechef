import {
    NotificationTestResult as NotificationTestResultType,
    NotificationTestResultUnsignedReasonEnum,
    NotificationTypeEnum,
} from '@/shared/middleware/platform/notification';
import {CheckCircle2Icon, XCircleIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface NotificationTestResultProps {
    notificationType: NotificationTypeEnum;
    result: NotificationTestResultType;
}

const NotificationTestResult = ({notificationType, result}: NotificationTestResultProps) => {
    const isUnsignedWebhook = result.success && !result.signed && notificationType === NotificationTypeEnum.Webhook;
    const hasStatusCode =
        result.statusCode != null && (!result.success || notificationType === NotificationTypeEnum.Webhook);

    return (
        <div
            aria-live="polite"
            className={twMerge(
                'flex flex-col gap-2 rounded-md border p-3 text-sm',
                result.success ? 'border-success' : 'border-destructive'
            )}
            role="status"
        >
            <div className="flex items-center gap-2 font-medium">
                {result.success ? (
                    <CheckCircle2Icon className="size-4 text-success" />
                ) : (
                    <XCircleIcon className="size-4 text-destructive" />
                )}

                <span>{result.success ? 'Test sent' : 'Test failed'}</span>
            </div>

            {!result.success && result.error && <p>{result.error}</p>}

            {hasStatusCode && <p>{`HTTP ${result.statusCode}`}</p>}

            {result.responseBody && (
                <pre className="max-h-40 overflow-auto rounded bg-surface-neutral-secondary p-2 font-mono text-xs break-all whitespace-pre-wrap">
                    {result.responseBody}
                </pre>
            )}

            {isUnsignedWebhook && (
                <p className="text-content-neutral-secondary">
                    {result.unsignedReason === NotificationTestResultUnsignedReasonEnum.UrlChanged
                        ? 'Sent without a signature because the URL changed; save the notification to sign with its secret.'
                        : 'Sent without a signature; save the notification to get a signing secret.'}
                </p>
            )}
        </div>
    );
};

export default NotificationTestResult;
