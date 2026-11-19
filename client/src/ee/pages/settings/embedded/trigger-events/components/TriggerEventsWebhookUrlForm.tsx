import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Label} from '@/components/ui/label';
import {useUpdateTriggerSubscriptionWebhookUrlMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {FormEvent, useState} from 'react';
import {toast} from 'sonner';

interface TriggerEventsWebhookUrlFormProps {
    environmentId: string;
    webhookUrl: string;
}

const TriggerEventsWebhookUrlForm = ({environmentId, webhookUrl}: TriggerEventsWebhookUrlFormProps) => {
    const [webhookUrlValue, setWebhookUrlValue] = useState(webhookUrl);

    const queryClient = useQueryClient();

    const updateWebhookUrlMutation = useUpdateTriggerSubscriptionWebhookUrlMutation({
        onSuccess: (_data, variables) => {
            void queryClient.invalidateQueries({queryKey: ['triggerSubscriptionWebhookSettings']});

            toast(variables.webhookUrl ? 'Webhook URL saved.' : 'Webhook URL cleared.');
        },
    });

    const trimmedWebhookUrl = webhookUrlValue.trim();

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        updateWebhookUrlMutation.mutate({environmentId, webhookUrl: trimmedWebhookUrl});
    };

    return (
        <form onSubmit={handleSubmit}>
            <fieldset className="flex flex-col gap-y-2 border-0">
                <Label htmlFor="trigger-events-webhook-url">Webhook URL</Label>

                <div className="flex items-center gap-x-2">
                    <Input
                        id="trigger-events-webhook-url"
                        onChange={(event) => setWebhookUrlValue(event.target.value)}
                        placeholder="https://example.com/webhooks/bytechef"
                        type="url"
                        value={webhookUrlValue}
                    />

                    <Button
                        disabled={trimmedWebhookUrl === webhookUrl || updateWebhookUrlMutation.isPending}
                        label="Save"
                        type="submit"
                    />
                </div>

                <p className="text-xs text-content-neutral-secondary">
                    Trigger events of all connected users in this environment are delivered to this URL.
                </p>
            </fieldset>
        </form>
    );
};

export default TriggerEventsWebhookUrlForm;
