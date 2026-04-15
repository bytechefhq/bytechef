import Button from '@/components/Button/Button';
import {
    AiObservabilityNotificationChannelType as ChannelTypeEnum,
    useCreateAiObservabilityNotificationChannelMutation,
    useTestAiObservabilityNotificationChannelMutation,
    useUpdateAiObservabilityNotificationChannelMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';

import {AiObservabilityNotificationChannelType} from '../../types';

interface AiObservabilityNotificationChannelDialogProps {
    channel?: AiObservabilityNotificationChannelType;
    onClose: () => void;
    workspaceId: string;
}

const CHANNEL_TYPES: ChannelTypeEnum[] = [ChannelTypeEnum.Webhook, ChannelTypeEnum.Email, ChannelTypeEnum.Slack];

const CONFIG_PLACEHOLDERS: Record<string, string> = {
    EMAIL: '{"to": "alerts@example.com"}',
    SLACK: '{"webhookUrl": "https://hooks.slack.com/..."}',
    WEBHOOK: '{"url": "https://example.com/webhook"}',
};

const AiObservabilityNotificationChannelDialog = ({
    channel,
    onClose,
    workspaceId,
}: AiObservabilityNotificationChannelDialogProps) => {
    const [config, setConfig] = useState(channel?.config ?? '');
    const [enabled, setEnabled] = useState(channel?.enabled ?? true);
    const [name, setName] = useState(channel?.name ?? '');
    const [type, setType] = useState<ChannelTypeEnum>(channel?.type ?? ChannelTypeEnum.Webhook);

    const queryClient = useQueryClient();

    const isEditMode = !!channel;

    const createMutation = useCreateAiObservabilityNotificationChannelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityNotificationChannels']});

            onClose();
        },
    });

    const updateMutation = useUpdateAiObservabilityNotificationChannelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityNotificationChannels']});

            onClose();
        },
    });

    const testMutation = useTestAiObservabilityNotificationChannelMutation({
        onError: (error: Error) => {
            toast.error(`Test notification failed: ${error.message}`);
        },
        onSuccess: () => {
            toast.success('Test notification sent successfully');
        },
    });

    const handleSendTest = useCallback(() => {
        if (!channel) {
            return;
        }

        testMutation.mutate({id: channel.id});
    }, [channel, testMutation]);

    const handleSubmit = useCallback(() => {
        const input = {
            config,
            enabled,
            name,
            type,
            workspaceId,
        };

        if (isEditMode) {
            updateMutation.mutate({id: channel.id, input});
        } else {
            createMutation.mutate({input});
        }
    }, [channel, config, createMutation, enabled, isEditMode, name, type, updateMutation, workspaceId]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">
                        {isEditMode ? 'Edit Notification Channel' : 'Add Notification Channel'}
                    </h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Slack Channel"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Type</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setType(event.target.value as ChannelTypeEnum)}
                            value={type}
                        >
                            {CHANNEL_TYPES.map((channelType) => (
                                <option key={channelType} value={channelType}>
                                    {channelType}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Configuration (JSON)</label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                            onChange={(event) => setConfig(event.target.value)}
                            placeholder={CONFIG_PLACEHOLDERS[type] || '{}'}
                            rows={4}
                            value={config}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 flex items-center gap-2 text-sm font-medium">
                            <input
                                checked={enabled}
                                onChange={(event) => setEnabled(event.target.checked)}
                                type="checkbox"
                            />
                            Enabled
                        </label>
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    {isEditMode && (
                        <Button
                            disabled={testMutation.isPending}
                            label="Send Test"
                            onClick={handleSendTest}
                            variant="outline"
                        />
                    )}

                    <Button
                        disabled={!name || !config || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityNotificationChannelDialog;
