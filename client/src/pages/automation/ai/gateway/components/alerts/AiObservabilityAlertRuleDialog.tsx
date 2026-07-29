import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiObservabilityAlertCondition,
    AiObservabilityAlertMetric,
    useCreateAiObservabilityAlertRuleMutation,
    useTestAiObservabilityAlertRuleMutation,
    useUpdateAiObservabilityAlertRuleMutation,
    useWorkspaceNotificationsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';

import {AiObservabilityAlertRuleType} from '../../types';

interface AiObservabilityAlertRuleDialogProps {
    onClose: () => void;
    rule?: AiObservabilityAlertRuleType;
    workspaceId: string;
}

const METRIC_OPTIONS: AiObservabilityAlertMetric[] = [
    AiObservabilityAlertMetric.ErrorRate,
    AiObservabilityAlertMetric.LatencyP95,
    AiObservabilityAlertMetric.Cost,
    AiObservabilityAlertMetric.TokenUsage,
    AiObservabilityAlertMetric.RequestVolume,
];

const CONDITION_OPTIONS: AiObservabilityAlertCondition[] = [
    AiObservabilityAlertCondition.GreaterThan,
    AiObservabilityAlertCondition.LessThan,
    AiObservabilityAlertCondition.Equals,
];

const CONDITION_LABELS: Record<string, string> = {
    EQUALS: 'Equals',
    GREATER_THAN: 'Greater Than',
    LESS_THAN: 'Less Than',
};

const AiObservabilityAlertRuleDialog = ({onClose, rule, workspaceId}: AiObservabilityAlertRuleDialogProps) => {
    const [notificationIds, setNotificationIds] = useState<string[]>(
        (rule?.notificationIds?.filter(Boolean) as string[]) ?? []
    );
    const [condition, setCondition] = useState<AiObservabilityAlertCondition>(
        rule?.condition ?? AiObservabilityAlertCondition.GreaterThan
    );
    const [cooldownMinutes, setCooldownMinutes] = useState(rule?.cooldownMinutes ?? 15);
    const [enabled, setEnabled] = useState(rule?.enabled ?? true);
    const [metric, setMetric] = useState<AiObservabilityAlertMetric>(
        rule?.metric ?? AiObservabilityAlertMetric.ErrorRate
    );
    const [name, setName] = useState(rule?.name ?? '');
    const [threshold, setThreshold] = useState(rule?.threshold ?? 0);
    const [windowMinutes, setWindowMinutes] = useState(rule?.windowMinutes ?? 5);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const isEditMode = !!rule;

    // Delivery targets come from the central notification registry: the workspace's own notifications plus the
    // global (unassigned) ones. Channels are managed under Settings -> Notifications.
    const {data: workspaceNotificationsData} = useWorkspaceNotificationsQuery(
        {workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : ''},
        {enabled: currentWorkspaceId != null}
    );

    const availableNotifications = workspaceNotificationsData?.workspaceNotifications ?? [];

    const createMutation = useCreateAiObservabilityAlertRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertRules']});

            onClose();
        },
    });

    const testMutation = useTestAiObservabilityAlertRuleMutation({
        onSuccess: (data) => {
            const metricValue = data?.testAiObservabilityAlertRule;

            if (metricValue == null) {
                toast('No data in the configured window yet.');
            } else {
                toast(`Current ${metric} value: ${Number(metricValue).toFixed(4)}`);
            }
        },
    });

    const updateMutation = useUpdateAiObservabilityAlertRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertRules']});

            onClose();
        },
    });

    const handleNotificationToggle = useCallback((notificationId: string) => {
        setNotificationIds((previous) =>
            previous.includes(notificationId)
                ? previous.filter((identifier) => identifier !== notificationId)
                : [...previous, notificationId]
        );
    }, []);

    const handleSubmit = useCallback(() => {
        const input = {
            condition,
            cooldownMinutes,
            enabled,
            metric,
            name,
            notificationIds,
            threshold,
            windowMinutes,
            workspaceId,
        };

        if (isEditMode) {
            updateMutation.mutate({id: rule.id, input});
        } else {
            createMutation.mutate({input});
        }
    }, [
        condition,
        cooldownMinutes,
        createMutation,
        enabled,
        isEditMode,
        metric,
        name,
        notificationIds,
        rule,
        threshold,
        updateMutation,
        windowMinutes,
        workspaceId,
    ]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Alert Rule' : 'Add Alert Rule'}</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="max-h-[70vh] space-y-4 overflow-y-auto">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="High Error Rate Alert"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Metric</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setMetric(event.target.value as AiObservabilityAlertMetric)}
                            value={metric}
                        >
                            {METRIC_OPTIONS.map((metricOption) => (
                                <option key={metricOption} value={metricOption}>
                                    {metricOption}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Condition</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setCondition(event.target.value as AiObservabilityAlertCondition)}
                            value={condition}
                        >
                            {CONDITION_OPTIONS.map((conditionOption) => (
                                <option key={conditionOption} value={conditionOption}>
                                    {CONDITION_LABELS[conditionOption] || conditionOption}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Threshold</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setThreshold(parseFloat(event.target.value) || 0)}
                            step="any"
                            type="number"
                            value={threshold}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Window (minutes)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            min="1"
                            onChange={(event) => setWindowMinutes(parseInt(event.target.value, 10) || 1)}
                            type="number"
                            value={windowMinutes}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Cooldown (minutes)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            min="1"
                            onChange={(event) => setCooldownMinutes(parseInt(event.target.value, 10) || 1)}
                            type="number"
                            value={cooldownMinutes}
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

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Notifications</label>

                        {availableNotifications.length === 0 ? (
                            <p className="text-sm text-muted-foreground">
                                No notifications available. Create one under Settings &gt; Notifications first.
                            </p>
                        ) : (
                            <div className="space-y-2">
                                {availableNotifications.map((notification) =>
                                    notification ? (
                                        <label className="flex items-center gap-2 text-sm" key={notification.id}>
                                            <input
                                                checked={notificationIds.includes(notification.id)}
                                                onChange={() => handleNotificationToggle(notification.id)}
                                                type="checkbox"
                                            />

                                            <span>
                                                {notification.name} ({notification.type})
                                            </span>
                                        </label>
                                    ) : null
                                )}
                            </div>
                        )}
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    {isEditMode && rule && (
                        <Button
                            disabled={testMutation.isPending}
                            label={testMutation.isPending ? 'Testing...' : 'Test'}
                            onClick={() => testMutation.mutate({id: rule.id})}
                            variant="outline"
                        />
                    )}

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiObservabilityAlertRuleDialog;
