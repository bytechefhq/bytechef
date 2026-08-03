import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowAlertRuleDialog from '@/pages/settings/platform/workflow-alerts/components/WorkflowAlertRuleDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    WorkflowAlertRulesQuery,
    useDeleteWorkflowAlertRuleMutation,
    useEnableWorkflowAlertRuleMutation,
    useSendTestWorkflowAlertMutation,
    useWorkflowAlertEventsQuery,
    useWorkflowAlertRulesQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BellIcon, BellOffIcon, BellRingIcon, PencilIcon, PlusIcon, SendIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';

type WorkflowAlertRuleItemType = WorkflowAlertRulesQuery['workflowAlertRules'][number];

const RULE_TYPE_LABELS: Record<string, string> = {
    CONSECUTIVE_FAILURES: 'Consecutive Failures',
    COST_THRESHOLD: 'Cost Threshold',
    ERROR_COUNT: 'Error Count',
    FAILURE_RATE: 'Failure Rate',
    LATENCY_SPIKE: 'Latency Spike',
    LATENCY_THRESHOLD: 'Latency Threshold',
    NO_ACTIVITY: 'No Activity',
    USAGE_THRESHOLD: 'Usage Threshold',
};

const WorkflowAlerts = () => {
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedRule, setSelectedRule] = useState<WorkflowAlertRuleItemType | undefined>(undefined);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const workspaceId = String(currentWorkspaceId);

    const {
        data: rulesData,
        error: rulesError,
        isLoading: rulesLoading,
    } = useWorkflowAlertRulesQuery({workspaceId}, {enabled: !!currentWorkspaceId});

    const {data: eventsData} = useWorkflowAlertEventsQuery({workspaceId}, {enabled: !!currentWorkspaceId});

    const deleteMutation = useDeleteWorkflowAlertRuleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['workflowAlertRules']}),
    });

    const enableMutation = useEnableWorkflowAlertRuleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['workflowAlertRules']}),
    });

    const sendTestMutation = useSendTestWorkflowAlertMutation({
        onSuccess: () => toast('Test alert sent through the configured notifications.'),
    });

    const rules = rulesData?.workflowAlertRules ?? [];
    const events = eventsData?.workflowAlertEvents ?? [];

    const handleOpenDialog = useCallback((rule?: WorkflowAlertRuleItemType) => {
        setSelectedRule(rule);

        setDialogOpen(true);
    }, []);

    const handleCloseDialog = useCallback(() => {
        setSelectedRule(undefined);

        setDialogOpen(false);
    }, []);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    position="main"
                    right={<Button icon={<PlusIcon />} label="New Alert Rule" onClick={() => handleOpenDialog()} />}
                    title="Alerts"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[rulesError]} loading={rulesLoading}>
                {rules.length > 0 ? (
                    <div className="w-full space-y-8 px-4">
                        <ul className="divide-y divide-border/50">
                            {rules.map((rule) => (
                                <li className="flex items-center justify-between py-3" key={rule.id}>
                                    <div className="flex flex-col">
                                        <span className="text-sm font-medium">{rule.name}</span>

                                        <span className="text-xs text-muted-foreground">
                                            {RULE_TYPE_LABELS[rule.ruleType] || rule.ruleType}

                                            {rule.ruleType !== 'NO_ACTIVITY' && ` · threshold ${rule.threshold}`}

                                            {rule.windowMinutes != null && ` · window ${rule.windowMinutes}m`}

                                            {rule.workflowId ? ` · workflow ${rule.workflowId}` : ' · all workflows'}

                                            {` · ${rule.notificationIds.length} notification${rule.notificationIds.length === 1 ? '' : 's'}`}
                                        </span>
                                    </div>

                                    <div className="flex items-center gap-2">
                                        <Button
                                            icon={rule.enabled ? <BellIcon /> : <BellOffIcon />}
                                            label={rule.enabled ? 'Enabled' : 'Disabled'}
                                            onClick={() => enableMutation.mutate({enabled: !rule.enabled, id: rule.id})}
                                            size="sm"
                                            variant="outline"
                                        />

                                        <Button
                                            disabled={sendTestMutation.isPending}
                                            icon={<SendIcon />}
                                            label="Test"
                                            onClick={() => sendTestMutation.mutate({id: rule.id})}
                                            size="sm"
                                            variant="outline"
                                        />

                                        <Button
                                            icon={<PencilIcon />}
                                            label="Edit"
                                            onClick={() => handleOpenDialog(rule)}
                                            size="sm"
                                            variant="outline"
                                        />

                                        <Button
                                            icon={<TrashIcon />}
                                            label="Delete"
                                            onClick={() => deleteMutation.mutate({id: rule.id})}
                                            size="sm"
                                            variant="destructive"
                                        />
                                    </div>
                                </li>
                            ))}
                        </ul>

                        {events.length > 0 && (
                            <div>
                                <h4 className="mb-2 text-sm font-medium">Recent alerts</h4>

                                <ul className="divide-y divide-border/50">
                                    {events.map((event) => (
                                        <li className="py-2 text-xs text-muted-foreground" key={event.id}>
                                            <span className="font-medium text-foreground">{event.createdDate}</span>

                                            {` — ${event.message}`}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                ) : (
                    <EmptyList
                        button={
                            <Button icon={<PlusIcon />} label="New Alert Rule" onClick={() => handleOpenDialog()} />
                        }
                        icon={<BellRingIcon className="size-12 text-content-neutral-tertiary" />}
                        message="Alert rules watch workflow runs and deliver through your configured Notifications."
                        title="No Alert Rules"
                    />
                )}
            </PageLoader>

            {dialogOpen && <WorkflowAlertRuleDialog onClose={handleCloseDialog} rule={selectedRule} />}
        </LayoutContainer>
    );
};

export default WorkflowAlerts;
