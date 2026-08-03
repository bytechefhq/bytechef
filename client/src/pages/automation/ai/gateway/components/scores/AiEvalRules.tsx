import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {useRunAiEvalRuleOnHistoricalTracesMutation} from '@/shared/middleware/graphql';
import {BrainCircuitIcon, HistoryIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';

import {AiEvalRuleType} from '../../types';

interface AiEvalRulesProps {
    evalRules: AiEvalRuleType[];
    isLoading: boolean;
}

const toDateTimeLocal = (date: Date): string => {
    const pad = (value: number) => String(value).padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const AiEvalRules = ({evalRules, isLoading}: AiEvalRulesProps) => {
    const defaultEnd = new Date();
    const defaultStart = new Date(defaultEnd.getTime() - 24 * 60 * 60 * 1000);

    const [historyRule, setHistoryRule] = useState<AiEvalRuleType | undefined>(undefined);
    const [startDate, setStartDate] = useState(toDateTimeLocal(defaultStart));
    const [endDate, setEndDate] = useState(toDateTimeLocal(defaultEnd));

    const runHistoricalMutation = useRunAiEvalRuleOnHistoricalTracesMutation({
        onSuccess: (data) => {
            const count = data?.runAiEvalRuleOnHistoricalTraces ?? 0;

            toast(`Queued ${count} trace${count === 1 ? '' : 's'} for re-evaluation.`);

            setHistoryRule(undefined);
        },
    });

    const handleRun = useCallback(() => {
        if (!historyRule) {
            return;
        }

        runHistoricalMutation.mutate({
            endDate: new Date(endDate).getTime(),
            ruleId: historyRule.id,
            startDate: new Date(startDate).getTime(),
        });
    }, [endDate, historyRule, runHistoricalMutation, startDate]);

    if (isLoading) {
        return <PageLoader loading={true} />;
    }

    if (evalRules.length === 0) {
        return (
            <EmptyList
                icon={<BrainCircuitIcon className="size-12 text-muted-foreground" />}
                message="Create eval rules to automatically score traces using LLM-as-judge."
                title="No Eval Rules"
            />
        );
    }

    return (
        <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
                <thead>
                    <tr className="border-b text-muted-foreground">
                        <th className="px-3 py-2 font-medium">Name</th>

                        <th className="px-3 py-2 font-medium">Model</th>

                        <th className="px-3 py-2 font-medium">Sampling Rate</th>

                        <th className="px-3 py-2 font-medium">Delay</th>

                        <th className="px-3 py-2 font-medium">Status</th>

                        <th className="px-3 py-2 font-medium">Actions</th>
                    </tr>
                </thead>

                <tbody>
                    {evalRules.map((rule) => (
                        <tr className="border-b hover:bg-muted/50" key={rule.id}>
                            <td className="px-3 py-2 font-medium">{rule.name}</td>

                            <td className="px-3 py-2">{rule.model}</td>

                            <td className="px-3 py-2">{(Number(rule.samplingRate) * 100).toFixed(0)}%</td>

                            <td className="px-3 py-2">{rule.delaySeconds != null ? `${rule.delaySeconds}s` : '-'}</td>

                            <td className="px-3 py-2">
                                <span
                                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                        rule.enabled
                                            ? 'bg-surface-success-secondary text-content-success-primary'
                                            : 'bg-surface-neutral-secondary text-content-neutral-primary'
                                    }`}
                                >
                                    {rule.enabled ? 'Enabled' : 'Disabled'}
                                </span>
                            </td>

                            <td className="px-3 py-2">
                                <button
                                    className="flex items-center gap-1 text-xs text-content-brand-primary"
                                    onClick={() => setHistoryRule(rule)}
                                    title="Run on historical traces"
                                >
                                    <HistoryIcon className="size-3" />
                                    Run on History
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>

            {historyRule && (
                <Dialog
                    onOpenChange={(open) => {
                        if (!open) {
                            setHistoryRule(undefined);
                        }
                    }}
                    open
                >
                    <DialogContent aria-describedby={undefined} className="max-w-md">
                        <DialogHeader>
                            <DialogTitle>Run {historyRule.name} on History</DialogTitle>
                        </DialogHeader>

                        <div className="space-y-4">
                            <fieldset className="border-0">
                                <Label className="mb-1 block" htmlFor="ai-eval-rule-history-start-date">
                                    Start Date
                                </Label>

                                <Input
                                    id="ai-eval-rule-history-start-date"
                                    onChange={(event) => setStartDate(event.target.value)}
                                    type="datetime-local"
                                    value={startDate}
                                />
                            </fieldset>

                            <fieldset className="border-0">
                                <Label className="mb-1 block" htmlFor="ai-eval-rule-history-end-date">
                                    End Date
                                </Label>

                                <Input
                                    id="ai-eval-rule-history-end-date"
                                    onChange={(event) => setEndDate(event.target.value)}
                                    type="datetime-local"
                                    value={endDate}
                                />
                            </fieldset>
                        </div>

                        <DialogFooter>
                            <Button label="Cancel" onClick={() => setHistoryRule(undefined)} variant="outline" />

                            <Button
                                disabled={runHistoricalMutation.isPending}
                                label={runHistoricalMutation.isPending ? 'Queuing...' : 'Run'}
                                onClick={handleRun}
                            />
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            )}
        </div>
    );
};

export default AiEvalRules;
