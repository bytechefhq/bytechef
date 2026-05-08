import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useCreateAiObservabilityWebhookSubscriptionMutation,
    useUpdateAiObservabilityWebhookSubscriptionMutation,
} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-semibold">{isEditing ? 'Edit Webhook' : 'New Webhook Subscription'}</h3>

                    <button className="text-muted-foreground hover:text-foreground" onClick={onClose}>
                        <XIcon className="size-5" />
                    </button>
                </div>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Name</label>

                    <input
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setName(event.target.value)}
                        placeholder="My Webhook"
                        value={name}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">URL</label>

                    <input
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setUrl(event.target.value)}
                        placeholder="https://example.com/webhook"
                        value={url}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Secret (HMAC-SHA256)</label>

                    <input
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        onChange={(event) => setSecret(event.target.value)}
                        placeholder={isEditing ? 'Leave empty to keep existing' : 'Optional signing secret'}
                        type="password"
                        value={secret}
                    />
                </fieldset>

                <fieldset className="mb-3 border-0 p-0">
                    <label className="mb-1 block text-sm font-medium">Events</label>

                    <div className="flex flex-wrap gap-2">
                        {AVAILABLE_EVENTS.map((event) => (
                            <label className="flex items-center gap-1.5 text-sm" key={event}>
                                <input
                                    checked={selectedEvents.includes(event)}
                                    onChange={() => handleEventToggle(event)}
                                    type="checkbox"
                                />

                                {event}
                            </label>
                        ))}
                    </div>
                </fieldset>

                <fieldset className="mb-6 border-0 p-0">
                    <label className="flex items-center gap-2 text-sm">
                        <input
                            checked={enabled}
                            onChange={(event) => setEnabled(event.target.checked)}
                            type="checkbox"
                        />
                        Enabled
                    </label>
                </fieldset>

                <div className="flex justify-end gap-2">
                    <button
                        className="rounded-md bg-muted px-4 py-2 text-sm text-muted-foreground hover:bg-muted/80"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                        disabled={isPending || !name || !url || selectedEvents.length === 0}
                        onClick={handleSave}
                    >
                        {isPending ? 'Saving...' : isEditing ? 'Update' : 'Create'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityWebhookSubscriptionDialog;
