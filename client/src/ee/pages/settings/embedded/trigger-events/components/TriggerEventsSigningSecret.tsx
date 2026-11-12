import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {Label} from '@/components/ui/label';
import {
    SendTriggerSubscriptionTestEventMutation,
    useRotateTriggerSubscriptionWebhookSecretMutation,
    useSendTriggerSubscriptionTestEventMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon, EyeIcon, EyeOffIcon, RefreshCwIcon, SendIcon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

type TestEventResultType = SendTriggerSubscriptionTestEventMutation['sendTriggerSubscriptionTestEvent'];

const MASKED_SIGNING_SECRET = '••••••••••••••••••••••••';

interface TriggerEventsSigningSecretProps {
    environmentId: string;
    signingSecret?: string;
}

const TriggerEventsSigningSecret = ({environmentId, signingSecret}: TriggerEventsSigningSecretProps) => {
    const [showRotateDialog, setShowRotateDialog] = useState(false);
    const [signingSecretRevealed, setSigningSecretRevealed] = useState(false);
    const [testEventResult, setTestEventResult] = useState<TestEventResultType | undefined>();

    const [, copyToClipboard] = useCopyToClipboard();
    const queryClient = useQueryClient();

    const rotateSigningSecretMutation = useRotateTriggerSubscriptionWebhookSecretMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['triggerSubscriptionWebhookSettings']});

            setShowRotateDialog(false);

            toast('Signing secret rotated.');
        },
    });

    const sendTestEventMutation = useSendTriggerSubscriptionTestEventMutation({
        onSuccess: (data) => setTestEventResult(data.sendTriggerSubscriptionTestEvent),
    });

    const handleCopyClick = async () => {
        if (signingSecret) {
            await copyToClipboard(signingSecret);

            toast('Signing secret copied to clipboard.');
        }
    };

    const handleSendTestEventClick = () => {
        setTestEventResult(undefined);

        sendTestEventMutation.mutate({environmentId});
    };

    return (
        <>
            <fieldset className="flex flex-col gap-y-2 border-0">
                <Label>Signing secret</Label>

                <div className="flex items-center gap-x-2">
                    <code className="flex h-9 min-w-0 flex-1 items-center truncate rounded-md border border-input px-3 font-mono text-sm">
                        {signingSecret ? (signingSecretRevealed ? signingSecret : MASKED_SIGNING_SECRET) : 'Not set'}
                    </code>

                    <Button
                        disabled={!signingSecret}
                        icon={signingSecretRevealed ? <EyeOffIcon /> : <EyeIcon />}
                        label={signingSecretRevealed ? 'Hide' : 'Reveal'}
                        onClick={() => setSigningSecretRevealed(!signingSecretRevealed)}
                        type="button"
                        variant="outline"
                    />

                    <Button
                        disabled={!signingSecret}
                        icon={<ClipboardIcon />}
                        label="Copy"
                        onClick={handleCopyClick}
                        type="button"
                        variant="outline"
                    />

                    <Button
                        icon={<RefreshCwIcon />}
                        label="Rotate"
                        onClick={() => setShowRotateDialog(true)}
                        type="button"
                        variant="outline"
                    />
                </div>

                <p className="text-xs text-content-neutral-secondary">
                    Verify the signature of every delivered event with this secret.
                </p>
            </fieldset>

            <fieldset className="flex flex-col items-start gap-y-2 border-0">
                <Label>Test delivery</Label>

                <div className="flex items-center gap-x-3">
                    <Button
                        disabled={sendTestEventMutation.isPending}
                        icon={<SendIcon />}
                        label="Send test event"
                        onClick={handleSendTestEventClick}
                        type="button"
                        variant="secondary"
                    />

                    {testEventResult && (
                        <span
                            className={twMerge(
                                'text-sm',
                                testEventResult.success
                                    ? 'text-content-success-primary'
                                    : 'text-content-destructive-primary'
                            )}
                        >
                            {testEventResult.success
                                ? `Delivered (HTTP ${testEventResult.statusCode})`
                                : testEventResult.error || `Failed (HTTP ${testEventResult.statusCode})`}
                        </span>
                    )}
                </div>
            </fieldset>

            <AlertDialog
                confirmButtonVariant="default"
                confirmLabel="Rotate secret"
                description="New events are signed with the new secret right away. Events already waiting for delivery keep the old one."
                isPending={rotateSigningSecretMutation.isPending}
                onCancel={() => setShowRotateDialog(false)}
                onConfirm={() => rotateSigningSecretMutation.mutate({environmentId})}
                open={showRotateDialog}
                title="Rotate signing secret?"
            />
        </>
    );
};

export default TriggerEventsSigningSecret;
