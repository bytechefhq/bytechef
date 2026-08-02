import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useCreateAiObservabilityWebhookSubscriptionMutation,
    useUpdateAiObservabilityWebhookSubscriptionMutation,
} from '@/shared/middleware/graphql';
import {useState} from 'react';

import {AiObservabilityWebhookSubscriptionType} from '../../types';
import {parseWebhookEvents} from '../../util/webhook-events';

interface AiObservabilityWebhookSubscriptionDialogProps {
    onClose: () => void;
    subscription?: AiObservabilityWebhookSubscriptionType;
}

const AVAILABLE_EVENTS = ['alert.triggered', 'budget.exceeded', 'trace.completed'];

const AiObservabilityWebhookSubscriptionDialog = ({
    onClose,
    subscription,
}: AiObservabilityWebhookSubscriptionDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [enabled, setEnabled] = useState(subscription?.enabled ?? true);
    const [name, setName] = useState(subscription?.name ?? '');
    const [secret, setSecret] = useState('');
    const [selectedEvents, setSelectedEvents] = useState<string[]>(parseWebhookEvents(subscription?.events));
    const [url, setUrl] = useState(subscription?.url ?? '');

    const createMutation = useCreateAiObservabilityWebhookSubscriptionMutation({});
    const updateMutation = useUpdateAiObservabilityWebhookSubscriptionMutation({});

    const isEditing = !!subscription;
    const isPending = createMutation.isPending || updateMutation.isPending;

    const handleEventToggle = (event: string) => {
        setSelectedEvents((previousEvents) =>
            previousEvents.includes(event)
                ? previousEvents.filter((previousEvent) => previousEvent !== event)
                : [...previousEvents, event]
        );
    };

    const handleSave = () => {
        const eventsJson = JSON.stringify(selectedEvents);

        if (isEditing) {
            updateMutation.mutate(
                {
                    enabled,
                    events: eventsJson,
                    id: subscription.id,
                    name,
                    secret: secret || undefined,
                    url,
                },
                {
                    onSuccess: () => {
                        onClose();
                    },
                }
            );
        } else {
            createMutation.mutate(
                {
                    enabled,
                    events: eventsJson,
                    name,
                    secret: secret || undefined,
                    url,
                    workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
                },
                {
                    onSuccess: () => {
                        onClose();
                    },
                }
            );
        }
    };

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-describedby={undefined} className="max-w-md">
                <DialogHeader>
                    <DialogTitle>{isEditing ? 'Edit Webhook' : 'New Webhook Subscription'}</DialogTitle>
                </DialogHeader>

                <fieldset className="mb-3 border-0 p-0">
                    <Label className="mb-1 block" htmlFor="ai-observability-webhook-subscription-name">
                        Name
                    </Label>

                    <Input
                        id="ai-observability-webhook-subscription-name"
                        onChange={(event) => setName(event.target.value)}
                        placeholder="My Webhook"
                        value={name}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <Label className="mb-1 block" htmlFor="ai-observability-webhook-subscription-url">
                        URL
                    </Label>

                    <Input
                        id="ai-observability-webhook-subscription-url"
                        onChange={(event) => setUrl(event.target.value)}
                        placeholder="https://example.com/webhook"
                        value={url}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <Label className="mb-1 block" htmlFor="ai-observability-webhook-subscription-secret">
                        Secret (HMAC-SHA256)
                    </Label>

                    <Input
                        id="ai-observability-webhook-subscription-secret"
                        onChange={(event) => setSecret(event.target.value)}
                        placeholder={isEditing ? 'Leave empty to keep existing' : 'Optional signing secret'}
                        type="password"
                        value={secret}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <legend className="mb-1 block text-sm font-medium">Events</legend>

                    <div className="flex flex-wrap gap-2">
                        {AVAILABLE_EVENTS.map((event) => (
                            <div className="flex items-center gap-1.5" key={event}>
                                <Checkbox
                                    checked={selectedEvents.includes(event)}
                                    id={`ai-observability-webhook-subscription-event-${event.replace(/\./g, '-')}`}
                                    onCheckedChange={() => handleEventToggle(event)}
                                />

                                <Label
                                    className="font-normal"
                                    htmlFor={`ai-observability-webhook-subscription-event-${event.replace(/\./g, '-')}`}
                                >
                                    {event}
                                </Label>
                            </div>
                        ))}
                    </div>
                </fieldset>

                <fieldset className="mb-6 border-0 p-0">
                    <div className="flex items-center gap-2">
                        <Checkbox
                            checked={enabled}
                            id="ai-observability-webhook-subscription-enabled"
                            onCheckedChange={(checked) => setEnabled(checked === true)}
                        />

                        <Label htmlFor="ai-observability-webhook-subscription-enabled">Enabled</Label>
                    </div>
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={isPending || !name || !url || selectedEvents.length === 0}
                        label={isPending ? 'Saving...' : isEditing ? 'Update' : 'Create'}
                        onClick={handleSave}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiObservabilityWebhookSubscriptionDialog;
