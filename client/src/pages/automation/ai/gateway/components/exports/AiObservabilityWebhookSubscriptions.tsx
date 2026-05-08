import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAiObservabilityWebhookSubscriptionsQuery,
    useDeleteAiObservabilityWebhookSubscriptionMutation,
    useTestAiObservabilityWebhookSubscriptionMutation,
} from '@/shared/middleware/graphql';
import {ListIcon, PencilIcon, PlayIcon, Trash2Icon, WebhookIcon} from 'lucide-react';
import {useState} from 'react';

import {AiObservabilityWebhookSubscriptionType} from '../../types';
import {parseWebhookEventsDetailed} from '../../util/webhook-events';
import AiObservabilityWebhookDeliveriesDialog from './AiObservabilityWebhookDeliveriesDialog';
import AiObservabilityWebhookSubscriptionDialog from './AiObservabilityWebhookSubscriptionDialog';

const AiObservabilityWebhookSubscriptions = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [editingSubscription, setEditingSubscription] = useState<
        AiObservabilityWebhookSubscriptionType | undefined
    >();
    const [showCreateDialog, setShowCreateDialog] = useState(false);
    const [viewingDeliveriesSubscription, setViewingDeliveriesSubscription] = useState<
        AiObservabilityWebhookSubscriptionType | undefined
    >();

    const {data: subscriptionsData, isLoading: subscriptionsIsLoading} = useAiObservabilityWebhookSubscriptionsQuery(
        {workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : ''},
        {enabled: currentWorkspaceId != null}
    );

    const deleteSubscriptionMutation = useDeleteAiObservabilityWebhookSubscriptionMutation({});
    const testSubscriptionMutation = useTestAiObservabilityWebhookSubscriptionMutation({});

    const subscriptions = subscriptionsData?.aiObservabilityWebhookSubscriptions ?? [];

    const handleDelete = (subscriptionId: string) => {
        deleteSubscriptionMutation.mutate({id: subscriptionId});
    };

    const handleTest = (subscriptionId: string) => {
        testSubscriptionMutation.mutate({id: subscriptionId});
    };

    return (
        <div>
            <div className="mb-4 flex justify-end">
                <button
                    className="rounded-md bg-primary px-3 py-1 text-sm text-primary-foreground hover:bg-primary/90"
                    onClick={() => setShowCreateDialog(true)}
                >
                    New Webhook
                </button>
            </div>

            {subscriptionsIsLoading ? (
                <PageLoader loading={true} />
            ) : subscriptions.length === 0 ? (
                <EmptyList
                    icon={<WebhookIcon className="size-12 text-muted-foreground" />}
                    message="Subscribe to events like trace.completed, alert.triggered, and budget.exceeded."
                    title="No Webhook Subscriptions"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="px-3 py-2 font-medium">Name</th>

                                <th className="px-3 py-2 font-medium">URL</th>

                                <th className="px-3 py-2 font-medium">Events</th>

                                <th className="px-3 py-2 font-medium">Enabled</th>

                                <th className="px-3 py-2 font-medium">Last Triggered</th>

                                <th className="px-3 py-2 font-medium">Actions</th>
                            </tr>
                        </thead>

                        <tbody>
                            {subscriptions.map((subscription) =>
                                subscription ? (
                                    <tr className="border-b" key={subscription.id}>
                                        <td className="px-3 py-2 font-medium">{subscription.name}</td>

                                        <td className="max-w-xs truncate px-3 py-2 text-muted-foreground">
                                            {subscription.url}
                                        </td>

                                        <td className="px-3 py-2">
                                            {(() => {
                                                const {events, malformed} = parseWebhookEventsDetailed(
                                                    subscription.events
                                                );

                                                return (
                                                    <div className="flex flex-wrap gap-1">
                                                        {malformed && (
                                                            <span
                                                                className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800"
                                                                title="The events JSON for this subscription is malformed — it will not deliver any events. Edit the subscription to reset events."
                                                            >
                                                                Malformed events
                                                            </span>
                                                        )}

                                                        {events.map((event) => (
                                                            <span
                                                                className="rounded-full bg-muted px-2 py-0.5 text-xs"
                                                                key={event}
                                                            >
                                                                {event}
                                                            </span>
                                                        ))}
                                                    </div>
                                                );
                                            })()}
                                        </td>

                                        <td className="px-3 py-2">
                                            <span
                                                className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                    subscription.enabled
                                                        ? 'bg-green-100 text-green-800'
                                                        : 'bg-gray-100 text-gray-800'
                                                }`}
                                            >
                                                {subscription.enabled ? 'Active' : 'Disabled'}
                                            </span>
                                        </td>

                                        <td className="px-3 py-2 text-muted-foreground">
                                            {subscription.lastTriggeredDate
                                                ? new Date(Number(subscription.lastTriggeredDate)).toLocaleString()
                                                : 'Never'}
                                        </td>

                                        <td className="px-3 py-2">
                                            <div className="flex gap-1">
                                                <button
                                                    className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                                    onClick={() => handleTest(subscription.id)}
                                                    title="Test webhook"
                                                >
                                                    <PlayIcon className="size-4" />
                                                </button>

                                                <button
                                                    className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                                    onClick={() => setViewingDeliveriesSubscription(subscription)}
                                                    title="View Deliveries"
                                                >
                                                    <ListIcon className="size-4" />
                                                </button>

                                                <button
                                                    className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                                    onClick={() => setEditingSubscription(subscription)}
                                                    title="Edit"
                                                >
                                                    <PencilIcon className="size-4" />
                                                </button>

                                                <button
                                                    className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-red-600"
                                                    onClick={() => handleDelete(subscription.id)}
                                                    title="Delete"
                                                >
                                                    <Trash2Icon className="size-4" />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ) : null
                            )}
                        </tbody>
                    </table>
                </div>
            )}

            {(showCreateDialog || editingSubscription) && (
                <AiObservabilityWebhookSubscriptionDialog
                    onClose={() => {
                        setShowCreateDialog(false);
                        setEditingSubscription(undefined);
                    }}
                    subscription={editingSubscription}
                />
            )}

            {viewingDeliveriesSubscription && (
                <AiObservabilityWebhookDeliveriesDialog
                    onClose={() => setViewingDeliveriesSubscription(undefined)}
                    subscriptionId={viewingDeliveriesSubscription.id}
                    subscriptionName={viewingDeliveriesSubscription.name}
                />
            )}
        </div>
    );
};

export default AiObservabilityWebhookSubscriptions;
