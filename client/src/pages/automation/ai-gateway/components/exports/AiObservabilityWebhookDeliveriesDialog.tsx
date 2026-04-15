import {useAiObservabilityWebhookDeliveriesQuery} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface AiObservabilityWebhookDeliveriesDialogPropsI {
    onClose: () => void;
    subscriptionId: string;
    subscriptionName: string;
}

const STATUS_COLORS: Record<string, string> = {
    FAILED: 'bg-red-100 text-red-800',
    PENDING: 'bg-gray-100 text-gray-800',
    RETRYING: 'bg-yellow-100 text-yellow-800',
    SUCCESS: 'bg-green-100 text-green-800',
};

const AiObservabilityWebhookDeliveriesDialog = ({
    onClose,
    subscriptionId,
    subscriptionName,
}: AiObservabilityWebhookDeliveriesDialogPropsI) => {
    const {data, isLoading} = useAiObservabilityWebhookDeliveriesQuery({subscriptionId});

    const deliveries = data?.aiObservabilityWebhookDeliveries ?? [];

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-4xl rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">Deliveries for {subscriptionName}</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                {isLoading ? (
                    <div className="py-6 text-center text-sm text-muted-foreground">Loading...</div>
                ) : deliveries.length === 0 ? (
                    <div className="py-6 text-center text-sm text-muted-foreground">No deliveries yet.</div>
                ) : (
                    <div className="max-h-[60vh] overflow-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="px-3 py-2 font-medium">Time</th>

                                    <th className="px-3 py-2 font-medium">Event</th>

                                    <th className="px-3 py-2 font-medium">Status</th>

                                    <th className="px-3 py-2 font-medium">HTTP</th>

                                    <th className="px-3 py-2 font-medium">Attempts</th>

                                    <th className="px-3 py-2 font-medium">Error</th>
                                </tr>
                            </thead>

                            <tbody>
                                {deliveries.map((delivery) =>
                                    delivery ? (
                                        <tr className="border-b" key={delivery.id}>
                                            <td className="px-3 py-2 text-muted-foreground">
                                                {delivery.createdDate
                                                    ? new Date(Number(delivery.createdDate)).toLocaleString()
                                                    : '-'}
                                            </td>

                                            <td className="px-3 py-2">{delivery.eventType ?? '-'}</td>

                                            <td className="px-3 py-2">
                                                <span
                                                    className={twMerge(
                                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                                        STATUS_COLORS[delivery.status] || 'bg-gray-100 text-gray-800'
                                                    )}
                                                >
                                                    {delivery.status}
                                                </span>
                                            </td>

                                            <td className="px-3 py-2">{delivery.httpStatus ?? '-'}</td>

                                            <td className="px-3 py-2">{delivery.attemptCount}</td>

                                            <td className="max-w-xs truncate px-3 py-2 text-muted-foreground">
                                                {delivery.errorMessage ?? '-'}
                                            </td>
                                        </tr>
                                    ) : null
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiObservabilityWebhookDeliveriesDialog;
