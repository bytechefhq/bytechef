import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Checkbox} from '@/components/ui/checkbox';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
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
                    <DialogTitle>{isEditMode ? 'Edit Alert Rule' : 'Add Alert Rule'}</DialogTitle>
                </DialogHeader>

                <div className="max-h-[70vh] space-y-4 overflow-y-auto">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="alertRuleName">
                            Name
                        </Label>

                        <Input
                            id="alertRuleName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="High Error Rate Alert"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="alertRuleMetric">
                            Metric
                        </Label>

                        <Select
                            onValueChange={(value) => setMetric(value as AiObservabilityAlertMetric)}
                            value={metric}
                        >
                            <SelectTrigger id="alertRuleMetric">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {METRIC_OPTIONS.map((metricOption) => (
                                    <SelectItem key={metricOption} value={metricOption}>
                                        {metricOption}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="alertRuleCondition">
                            Condition
                        </Label>

                        <Select
                            onValueChange={(value) => setCondition(value as AiObservabilityAlertCondition)}
                            value={condition}
                        >
                            <SelectTrigger id="alertRuleCondition">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {CONDITION_OPTIONS.map((conditionOption) => (
                                    <SelectItem key={conditionOption} value={conditionOption}>
                                        {CONDITION_LABELS[conditionOption] || conditionOption}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="alertRuleThreshold">
                            Threshold
                        </Label>

                        <Input
                            id="alertRuleThreshold"
                            onChange={(event) => setThreshold(parseFloat(event.target.value) || 0)}
                            step="any"
                            type="number"
                            value={threshold}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="alertRuleWindowMinutes">
                            Window (minutes)
                        </Label>

                        <Input
                            id="alertRuleWindowMinutes"
                            min="1"
                            onChange={(event) => setWindowMinutes(parseInt(event.target.value, 10) || 1)}
                            type="number"
                            value={windowMinutes}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="alertRuleCooldownMinutes">
                            Cooldown (minutes)
                        </Label>

                        <Input
                            id="alertRuleCooldownMinutes"
                            min="1"
                            onChange={(event) => setCooldownMinutes(parseInt(event.target.value, 10) || 1)}
                            type="number"
                            value={cooldownMinutes}
                        />
                    </fieldset>

                    <fieldset className="flex items-center gap-2 border-0">
                        <Checkbox
                            checked={enabled}
                            id="alertRuleEnabled"
                            onCheckedChange={(checked) => setEnabled(checked === true)}
                        />

                        <Label htmlFor="alertRuleEnabled">Enabled</Label>
                    </fieldset>

                    <fieldset className="border-0">
                        <legend className="mb-1 block text-sm font-medium">Notifications</legend>

                        {availableNotifications.length === 0 ? (
                            <p className="text-sm text-muted-foreground">
                                No notifications available. Create one under Settings &gt; Notifications first.
                            </p>
                        ) : (
                            <div className="space-y-2">
                                {availableNotifications.map((notification) =>
                                    notification ? (
                                        <div className="flex items-center gap-2" key={notification.id}>
                                            <Checkbox
                                                checked={notificationIds.includes(notification.id)}
                                                id={`alertRuleNotification-${notification.id}`}
                                                onCheckedChange={() => handleNotificationToggle(notification.id)}
                                            />

                                            <Label htmlFor={`alertRuleNotification-${notification.id}`}>
                                                {notification.name} ({notification.type})
                                            </Label>
                                        </div>
                                    ) : null
                                )}
                            </div>
                        )}
                    </fieldset>
                </div>

                <DialogFooter>
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
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiObservabilityAlertRuleDialog;
