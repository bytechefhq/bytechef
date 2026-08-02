import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Checkbox} from '@/components/ui/checkbox';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    WorkflowAlertRuleType,
    WorkflowAlertRulesQuery,
    useCreateWorkflowAlertRuleMutation,
    useUpdateWorkflowAlertRuleMutation,
    useWorkspaceNotificationsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
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
                    <DialogTitle>{isEditMode ? 'Edit Alert Rule' : 'New Alert Rule'}</DialogTitle>
                </DialogHeader>

                <div className="max-h-[70vh] space-y-4 overflow-y-auto">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="workflowAlertRuleName">
                            Name
                        </Label>

                        <Input
                            id="workflowAlertRuleName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Consecutive failures on order sync"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="workflowAlertRuleType">
                            Rule Type
                        </Label>

                        <Select onValueChange={(value) => setRuleType(value as WorkflowAlertRuleType)} value={ruleType}>
                            <SelectTrigger id="workflowAlertRuleType">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {RULE_TYPE_OPTIONS.map((ruleTypeOption) => (
                                    <SelectItem key={ruleTypeOption.value} value={ruleTypeOption.value}>
                                        {ruleTypeOption.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    {ruleType !== WorkflowAlertRuleType.NoActivity && (
                        <fieldset className="border-0">
                            <Label className="mb-1 block" htmlFor="workflowAlertRuleThreshold">
                                {selectedRuleTypeOption?.thresholdLabel || 'Threshold'}
                            </Label>

                            <Input
                                id="workflowAlertRuleThreshold"
                                onChange={(event) => setThreshold(parseFloat(event.target.value) || 0)}
                                step="any"
                                type="number"
                                value={threshold}
                            />
                        </fieldset>
                    )}

                    {ruleType !== WorkflowAlertRuleType.UsageThreshold && (
                        <fieldset className="border-0">
                            <Label className="mb-1 block" htmlFor="workflowAlertRuleWindowMinutes">
                                {ruleType === WorkflowAlertRuleType.NoActivity
                                    ? 'Maximum silence (minutes)'
                                    : 'Window (minutes)'}
                            </Label>

                            <Input
                                id="workflowAlertRuleWindowMinutes"
                                min="1"
                                onChange={(event) => setWindowMinutes(parseInt(event.target.value, 10) || 1)}
                                type="number"
                                value={windowMinutes}
                            />
                        </fieldset>
                    )}

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="workflowAlertRuleCooldownMinutes">
                            Cooldown (minutes)
                        </Label>

                        <Input
                            id="workflowAlertRuleCooldownMinutes"
                            min="1"
                            onChange={(event) => setCooldownMinutes(parseInt(event.target.value, 10) || 1)}
                            type="number"
                            value={cooldownMinutes}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="workflowAlertRuleWorkflowId">
                            Workflow Id (optional)
                        </Label>

                        <Input
                            id="workflowAlertRuleWorkflowId"
                            onChange={(event) => setWorkflowId(event.target.value)}
                            placeholder="Leave empty to match every workflow in the workspace"
                            value={workflowId}
                        />
                    </fieldset>

                    <fieldset className="flex items-center gap-2 border-0">
                        <Checkbox
                            checked={enabled}
                            id="workflowAlertRuleEnabled"
                            onCheckedChange={(checked) => setEnabled(checked === true)}
                        />

                        <Label htmlFor="workflowAlertRuleEnabled">Enabled</Label>
                    </fieldset>

                    <fieldset className="border-0">
                        <legend className="mb-1 block text-sm font-medium">Notifications</legend>

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
                                            <div className="flex items-center gap-2" key={notificationId}>
                                                <Checkbox
                                                    checked={notificationIds.includes(notificationId)}
                                                    id={`workflowAlertRuleNotification-${notificationId}`}
                                                    onCheckedChange={() => handleNotificationToggle(notificationId)}
                                                />

                                                <Label htmlFor={`workflowAlertRuleNotification-${notificationId}`}>
                                                    {notification.name} ({notification.type})
                                                </Label>
                                            </div>
                                        );
                                    })}
                            </div>
                        )}
                    </fieldset>
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowAlertRuleDialog;
