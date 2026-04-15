import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAiObservabilityAlertRulesQuery,
    useDeleteAiObservabilityAlertRuleMutation,
    useSnoozeAiObservabilityAlertRuleMutation,
    useUnsnoozeAiObservabilityAlertRuleMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BellIcon, BellOffIcon, PencilIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import {AiObservabilityAlertRuleType} from '../../types';
import AiObservabilityAlertRuleDialog from './AiObservabilityAlertRuleDialog';

const CONDITION_LABELS: Record<string, string> = {
    EQUALS: '=',
    GREATER_THAN: '>',
    LESS_THAN: '<',
};

const SNOOZE_DURATIONS: {hours: number; label: string}[] = [
    {hours: 1, label: '1 hour'},
    {hours: 4, label: '4 hours'},
    {hours: 24, label: '24 hours'},
    {hours: 24 * 7, label: '7 days'},
];

const formatRemainingSnooze = (snoozedUntil: number): string => {
    const remainingMs = snoozedUntil - Date.now();

    if (remainingMs <= 0) {
        return '';
    }

    const hours = Math.floor(remainingMs / (1000 * 60 * 60));

    if (hours >= 24) {
        const days = Math.floor(hours / 24);

        return `${days}d ${hours % 24}h`;
    }

    if (hours >= 1) {
        return `${hours}h`;
    }

    const minutes = Math.floor(remainingMs / (1000 * 60));

    return `${minutes}m`;
};

const AiObservabilityAlertRules = () => {
    const [deletingRuleId, setDeletingRuleId] = useState<string | undefined>(undefined);
    const [editingRule, setEditingRule] = useState<AiObservabilityAlertRuleType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);
    const [snoozeMenuRuleId, setSnoozeMenuRuleId] = useState<string | undefined>(undefined);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: rulesData, isLoading: rulesIsLoading} = useAiObservabilityAlertRulesQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteRuleMutation = useDeleteAiObservabilityAlertRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertRules']});

            setDeletingRuleId(undefined);
        },
    });

    const snoozeRuleMutation = useSnoozeAiObservabilityAlertRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertRules']});

            toast.success('Alert rule snoozed');

            setSnoozeMenuRuleId(undefined);
        },
    });

    const unsnoozeRuleMutation = useUnsnoozeAiObservabilityAlertRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityAlertRules']});

            toast.success('Alert rule unsnoozed');
        },
    });

    const rules = rulesData?.aiObservabilityAlertRules ?? [];

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingRule(undefined);
    }, []);

    const handleConfirmDelete = useCallback(() => {
        if (deletingRuleId) {
            deleteRuleMutation.mutate({id: deletingRuleId});
        }
    }, [deleteRuleMutation, deletingRuleId]);

    const handleEditRule = useCallback((rule: AiObservabilityAlertRuleType) => {
        setEditingRule(rule);
        setShowDialog(true);
    }, []);

    const handleSnooze = useCallback(
        (ruleId: string, hours: number) => {
            const until = Date.now() + hours * 60 * 60 * 1000;

            snoozeRuleMutation.mutate({id: ruleId, until});
        },
        [snoozeRuleMutation]
    );

    const handleUnsnooze = useCallback(
        (ruleId: string) => {
            unsnoozeRuleMutation.mutate({id: ruleId});
        },
        [unsnoozeRuleMutation]
    );

    if (rulesIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div>
            {rules.length === 0 ? (
                <EmptyList
                    button={<Button label="Add Rule" onClick={() => setShowDialog(true)} />}
                    icon={<BellIcon className="size-12 text-muted-foreground" />}
                    message="Create alert rules to get notified when metrics exceed thresholds."
                    title="No Alert Rules"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Add Rule"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Metric</th>

                                    <th className="pb-2 font-medium">Condition</th>

                                    <th className="pb-2 font-medium">Window</th>

                                    <th className="pb-2 font-medium">Enabled</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {rules.map((rule) =>
                                    rule ? (
                                        <tr className="border-b" key={rule.id}>
                                            <td className="py-3 font-medium">{rule.name}</td>

                                            <td className="py-3">{rule.metric}</td>

                                            <td className="py-3">
                                                {CONDITION_LABELS[rule.condition] || rule.condition} {rule.threshold}
                                            </td>

                                            <td className="py-3">{rule.windowMinutes}m</td>

                                            <td className="py-3">
                                                <div className="flex items-center gap-1">
                                                    <span
                                                        className={twMerge(
                                                            'rounded-full px-2 py-0.5 text-xs font-medium',
                                                            rule.enabled
                                                                ? 'bg-green-100 text-green-800'
                                                                : 'bg-gray-100 text-gray-800'
                                                        )}
                                                    >
                                                        {rule.enabled ? 'Active' : 'Disabled'}
                                                    </span>

                                                    {rule.snoozedUntil && Number(rule.snoozedUntil) > Date.now() && (
                                                        <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-xs font-medium text-yellow-800">
                                                            Snoozed {formatRemainingSnooze(Number(rule.snoozedUntil))}
                                                        </span>
                                                    )}
                                                </div>
                                            </td>

                                            <td className="py-3">
                                                <div className="relative flex gap-2">
                                                    {rule.snoozedUntil && Number(rule.snoozedUntil) > Date.now() ? (
                                                        <button
                                                            className="text-muted-foreground hover:text-foreground"
                                                            onClick={() => handleUnsnooze(rule.id)}
                                                            title="Unsnooze"
                                                        >
                                                            <BellIcon className="size-4" />
                                                        </button>
                                                    ) : (
                                                        <button
                                                            className="text-muted-foreground hover:text-foreground"
                                                            onClick={() =>
                                                                setSnoozeMenuRuleId(
                                                                    snoozeMenuRuleId === rule.id ? undefined : rule.id
                                                                )
                                                            }
                                                            title="Snooze"
                                                        >
                                                            <BellOffIcon className="size-4" />
                                                        </button>
                                                    )}

                                                    {snoozeMenuRuleId === rule.id && (
                                                        <div className="absolute right-0 top-6 z-10 w-32 rounded-md border bg-background shadow-md">
                                                            {SNOOZE_DURATIONS.map((duration) => (
                                                                <button
                                                                    className="block w-full px-3 py-1.5 text-left text-xs hover:bg-muted"
                                                                    key={duration.hours}
                                                                    onClick={() =>
                                                                        handleSnooze(rule.id, duration.hours)
                                                                    }
                                                                >
                                                                    {duration.label}
                                                                </button>
                                                            ))}
                                                        </div>
                                                    )}

                                                    <button
                                                        className="text-muted-foreground hover:text-foreground"
                                                        onClick={() => handleEditRule(rule)}
                                                    >
                                                        <PencilIcon className="size-4" />
                                                    </button>

                                                    <button
                                                        className="text-destructive hover:text-destructive/80"
                                                        onClick={() => setDeletingRuleId(rule.id)}
                                                    >
                                                        <TrashIcon className="size-4" />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ) : null
                                )}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <AlertDialog open={!!deletingRuleId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the alert rule and stop all
                            notifications.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeletingRuleId(undefined)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                            onClick={handleConfirmDelete}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showDialog && currentWorkspaceId != null && (
                <AiObservabilityAlertRuleDialog
                    onClose={handleCloseDialog}
                    rule={editingRule}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiObservabilityAlertRules;
