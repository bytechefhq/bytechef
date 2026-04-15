import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiGatewayBudgetEnforcementMode,
    AiGatewayBudgetPeriod,
    useAiGatewayBudgetQuery,
    useCreateAiGatewayBudgetMutation,
    useDeleteAiGatewayBudgetMutation,
    useUpdateAiGatewayBudgetMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, TrashIcon, WalletIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const PERIOD_OPTIONS: AiGatewayBudgetPeriod[] = [
    AiGatewayBudgetPeriod.Daily,
    AiGatewayBudgetPeriod.Weekly,
    AiGatewayBudgetPeriod.Monthly,
    AiGatewayBudgetPeriod.Quarterly,
    AiGatewayBudgetPeriod.Yearly,
];

const ENFORCEMENT_MODE_OPTIONS: AiGatewayBudgetEnforcementMode[] = [
    AiGatewayBudgetEnforcementMode.Soft,
    AiGatewayBudgetEnforcementMode.Hard,
];

const AiGatewayBudget = () => {
    const [alertThreshold, setAlertThreshold] = useState('80');
    const [amount, setAmount] = useState('');
    const [confirmingDelete, setConfirmingDelete] = useState(false);
    const [editing, setEditing] = useState(false);
    const [enforcementMode, setEnforcementMode] = useState<AiGatewayBudgetEnforcementMode>(
        AiGatewayBudgetEnforcementMode.Soft
    );
    const [period, setPeriod] = useState<AiGatewayBudgetPeriod>(AiGatewayBudgetPeriod.Monthly);
    const [showForm, setShowForm] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: budgetData, isLoading: budgetIsLoading} = useAiGatewayBudgetQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const budget = budgetData?.aiGatewayBudget;

    const createMutation = useCreateAiGatewayBudgetMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayBudget']});

            setShowForm(false);
        },
    });

    const updateMutation = useUpdateAiGatewayBudgetMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayBudget']});

            setEditing(false);
            setShowForm(false);
        },
    });

    const deleteMutation = useDeleteAiGatewayBudgetMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiGatewayBudget']});

            setConfirmingDelete(false);
        },
    });

    const formattedPeriod = useMemo(() => {
        if (!budget) {
            return '';
        }

        return budget.period.charAt(0) + budget.period.slice(1).toLowerCase();
    }, [budget]);

    const handleEdit = useCallback(() => {
        if (budget) {
            setAlertThreshold(budget.alertThreshold.toString());
            setAmount(budget.amount.toString());
            setEnforcementMode(budget.enforcementMode);
            setPeriod(budget.period);
            setEditing(true);
            setShowForm(true);
        }
    }, [budget]);

    const handleConfirmDelete = useCallback(() => {
        if (budget) {
            deleteMutation.mutate({id: budget.id});
        }
    }, [budget, deleteMutation]);

    const handleSubmit = useCallback(() => {
        if (editing && budget) {
            updateMutation.mutate({
                id: budget.id,
                input: {
                    alertThreshold: alertThreshold ? parseInt(alertThreshold, 10) : undefined,
                    amount: amount || undefined,
                    enforcementMode,
                    period,
                },
            });
        } else {
            createMutation.mutate({
                input: {
                    alertThreshold: alertThreshold ? parseInt(alertThreshold, 10) : undefined,
                    amount,
                    enforcementMode,
                    period,
                    workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
                },
            });
        }
    }, [
        alertThreshold,
        amount,
        budget,
        createMutation,
        currentWorkspaceId,
        editing,
        enforcementMode,
        period,
        updateMutation,
    ]);

    const handleCancel = useCallback(() => {
        setEditing(false);
        setShowForm(false);
    }, []);

    if (budgetIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (showForm) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <div className="py-4">
                    <h3 className="text-lg font-medium">{editing ? 'Edit Budget' : 'Create Budget'}</h3>
                </div>

                <div className="max-w-md space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Budget Amount ($)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setAmount(event.target.value)}
                            placeholder="100.00"
                            step="0.01"
                            type="number"
                            value={amount}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Period</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setPeriod(event.target.value as AiGatewayBudgetPeriod)}
                            value={period}
                        >
                            {PERIOD_OPTIONS.map((periodOption) => (
                                <option key={periodOption} value={periodOption}>
                                    {periodOption.charAt(0) + periodOption.slice(1).toLowerCase()}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Enforcement Mode</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) =>
                                setEnforcementMode(event.target.value as AiGatewayBudgetEnforcementMode)
                            }
                            value={enforcementMode}
                        >
                            {ENFORCEMENT_MODE_OPTIONS.map((mode) => (
                                <option key={mode} value={mode}>
                                    {mode.charAt(0) + mode.slice(1).toLowerCase()}
                                </option>
                            ))}
                        </select>

                        <p className="mt-1 text-xs text-muted-foreground">
                            Soft: alerts only. Hard: blocks requests when budget is exceeded.
                        </p>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Alert Threshold (%)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            max="100"
                            min="0"
                            onChange={(event) => setAlertThreshold(event.target.value)}
                            placeholder="80"
                            type="number"
                            value={alertThreshold}
                        />

                        <p className="mt-1 text-xs text-muted-foreground">
                            Alert when spending reaches this percentage of the budget.
                        </p>
                    </fieldset>

                    <div className="flex gap-2 pt-2">
                        <Button label="Cancel" onClick={handleCancel} variant="outline" />

                        <Button
                            disabled={!amount || createMutation.isPending || updateMutation.isPending}
                            label={editing ? 'Save' : 'Create'}
                            onClick={handleSubmit}
                        />
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {!budget ? (
                <EmptyList
                    button={<Button label="Create Budget" onClick={() => setShowForm(true)} />}
                    icon={<WalletIcon className="size-12 text-muted-foreground" />}
                    message="Set up a spending budget to control LLM gateway costs."
                    title="No Budget Configured"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <div className="flex gap-2">
                            <Button
                                icon={<PencilIcon className="size-4" />}
                                label="Edit"
                                onClick={handleEdit}
                                variant="outline"
                            />

                            <Button
                                icon={<TrashIcon className="size-4" />}
                                label="Delete"
                                onClick={() => setConfirmingDelete(true)}
                                variant="outline"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="rounded-lg border p-4">
                            <p className="text-sm text-muted-foreground">Budget Amount</p>

                            <p className="text-2xl font-bold">${parseFloat(budget.amount).toFixed(2)}</p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <p className="text-sm text-muted-foreground">Period</p>

                            <p className="text-2xl font-bold">{formattedPeriod}</p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <p className="text-sm text-muted-foreground">Enforcement Mode</p>

                            <p className="text-2xl font-bold">
                                {budget.enforcementMode.charAt(0) + budget.enforcementMode.slice(1).toLowerCase()}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <p className="text-sm text-muted-foreground">Alert Threshold</p>

                            <p className="text-2xl font-bold">{budget.alertThreshold}%</p>
                        </div>
                    </div>

                    <div className="mt-4">
                        <span
                            className={twMerge(
                                'rounded-full px-2 py-0.5 text-xs font-medium',
                                budget.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                            )}
                        >
                            {budget.enabled ? 'Active' : 'Disabled'}
                        </span>
                    </div>
                </>
            )}

            <DeleteAlertDialog
                onCancel={() => setConfirmingDelete(false)}
                onDelete={handleConfirmDelete}
                open={confirmingDelete}
            />
        </div>
    );
};

export default AiGatewayBudget;
