import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    WorkflowAlertRuleType,
    WorkflowAlertRulesQuery,
    useCreateWorkflowAlertRuleMutation,
    useUpdateWorkflowAlertRuleMutation,
    useWorkspaceNotificationsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

type WorkflowAlertRuleItemType = WorkflowAlertRulesQuery['workflowAlertRules'][number];

interface WorkflowAlertRuleDialogProps {
    onClose: () => void;
    rule?: WorkflowAlertRuleItemType;
}

const RULE_TYPE_OPTIONS: {label: string; thresholdLabel: string; value: WorkflowAlertRuleType}[] = [
    {
        label: 'Consecutive Failures',
        thresholdLabel: 'Failures (count)',
        value: WorkflowAlertRuleType.ConsecutiveFailures,
    },
    {label: 'Failure Rate', thresholdLabel: 'Failure rate (%)', value: WorkflowAlertRuleType.FailureRate},
    {label: 'Error Count', thresholdLabel: 'Failed runs (count)', value: WorkflowAlertRuleType.ErrorCount},
    {label: 'Latency Threshold', thresholdLabel: 'Duration (ms)', value: WorkflowAlertRuleType.LatencyThreshold},
    {label: 'Latency Spike', thresholdLabel: 'Multiplier over average', value: WorkflowAlertRuleType.LatencySpike},
    {label: 'Cost Threshold', thresholdLabel: 'Run cost (USD)', value: WorkflowAlertRuleType.CostThreshold},
    {label: 'No Activity', thresholdLabel: 'Unused', value: WorkflowAlertRuleType.NoActivity},
    {
        label: 'Usage Threshold',
        thresholdLabel: 'Percent of monthly plan cost (%)',
        value: WorkflowAlertRuleType.UsageThreshold,
    },
];

const WorkflowAlertRuleDialog = ({onClose, rule}: WorkflowAlertRuleDialogProps) => {
    const [cooldownMinutes, setCooldownMinutes] = useState(rule?.cooldownMinutes ?? 60);
    const [enabled, setEnabled] = useState(rule?.enabled ?? true);
    const [name, setName] = useState(rule?.name ?? '');
    const [notificationIds, setNotificationIds] = useState<string[]>(rule?.notificationIds ?? []);
    const [ruleType, setRuleType] = useState<WorkflowAlertRuleType>(
        rule?.ruleType ?? WorkflowAlertRuleType.ConsecutiveFailures
    );
    const [threshold, setThreshold] = useState(rule?.threshold ?? 3);
    const [windowMinutes, setWindowMinutes] = useState(rule?.windowMinutes ?? 60);
    const [workflowId, setWorkflowId] = useState(rule?.workflowId ?? '');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    // Workspace-scoped picker: the workspace's own notifications plus the global (unassigned) ones.
    const {data: workspaceNotificationsData} = useWorkspaceNotificationsQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: !!currentWorkspaceId}
    );

    const notifications = workspaceNotificationsData?.workspaceNotifications;

    const isEditMode = !!rule;

    const selectedRuleTypeOption = RULE_TYPE_OPTIONS.find((option) => option.value === ruleType);

    const createMutation = useCreateWorkflowAlertRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workflowAlertRules']});

            onClose();
        },
    });

    const updateMutation = useUpdateWorkflowAlertRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workflowAlertRules']});

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
            cooldownMinutes,
            enabled,
            name,
            notificationIds,
            ruleType,
            threshold,
            windowMinutes,
            workflowId: workflowId || null,
        };

        if (isEditMode) {
            updateMutation.mutate({id: rule.id, input});
        } else {
            createMutation.mutate({input, workspaceId: String(currentWorkspaceId)});
        }
    }, [
        cooldownMinutes,
        createMutation,
        currentWorkspaceId,
        enabled,
        isEditMode,
        name,
        notificationIds,
        rule,
        ruleType,
        threshold,
        updateMutation,
        windowMinutes,
        workflowId,
    ]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Alert Rule' : 'New Alert Rule'}</h3>

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
                            placeholder="Consecutive failures on order sync"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Rule Type</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setRuleType(event.target.value as WorkflowAlertRuleType)}
                            value={ruleType}
                        >
                            {RULE_TYPE_OPTIONS.map((ruleTypeOption) => (
                                <option key={ruleTypeOption.value} value={ruleTypeOption.value}>
                                    {ruleTypeOption.label}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    {ruleType !== WorkflowAlertRuleType.NoActivity && (
                        <fieldset className="border-0">
                            <label className="mb-1 block text-sm font-medium">
                                {selectedRuleTypeOption?.thresholdLabel || 'Threshold'}
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                onChange={(event) => setThreshold(parseFloat(event.target.value) || 0)}
                                step="any"
                                type="number"
                                value={threshold}
                            />
                        </fieldset>
                    )}

                    {ruleType !== WorkflowAlertRuleType.UsageThreshold && (
                        <fieldset className="border-0">
                            <label className="mb-1 block text-sm font-medium">
                                {ruleType === WorkflowAlertRuleType.NoActivity
                                    ? 'Maximum silence (minutes)'
                                    : 'Window (minutes)'}
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                min="1"
                                onChange={(event) => setWindowMinutes(parseInt(event.target.value, 10) || 1)}
                                type="number"
                                value={windowMinutes}
                            />
                        </fieldset>
                    )}

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
                        <label className="mb-1 block text-sm font-medium">Workflow Id (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setWorkflowId(event.target.value)}
                            placeholder="Leave empty to match every workflow in the workspace"
                            value={workflowId}
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

                        {!notifications || notifications.length === 0 ? (
                            <p className="text-sm text-muted-foreground">
                                No notifications configured. Create one on the Notifications page first.
                            </p>
                        ) : (
                            <div className="space-y-2">
                                {notifications
                                    .filter((notification) => notification?.id != null)
                                    .map((notification) => {
                                        const notificationId = String(notification.id);

                                        return (
                                            <label className="flex items-center gap-2 text-sm" key={notificationId}>
                                                <input
                                                    checked={notificationIds.includes(notificationId)}
                                                    onChange={() => handleNotificationToggle(notificationId)}
                                                    type="checkbox"
                                                />

                                                <span>
                                                    {notification.name} ({notification.type})
                                                </span>
                                            </label>
                                        );
                                    })}
                            </div>
                        )}
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

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

export default WorkflowAlertRuleDialog;
