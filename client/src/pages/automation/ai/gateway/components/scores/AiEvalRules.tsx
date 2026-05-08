import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useRunAiEvalRuleOnHistoricalTracesMutation} from '@/shared/middleware/graphql';
import {BrainCircuitIcon, HistoryIcon, XIcon} from 'lucide-react';
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
                                        rule.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                                    }`}
                                >
                                    {rule.enabled ? 'Enabled' : 'Disabled'}
                                </span>
                            </td>

                            <td className="px-3 py-2">
                                <button
                                    className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800"
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
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                    <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                        <div className="mb-4 flex items-center justify-between">
                            <h3 className="text-lg font-medium">Run {historyRule.name} on History</h3>

                            <button onClick={() => setHistoryRule(undefined)}>
                                <XIcon className="size-4" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <fieldset className="border-0">
                                <label className="mb-1 block text-sm font-medium">Start Date</label>

                                <input
                                    className="w-full rounded-md border px-3 py-2 text-sm"
                                    onChange={(event) => setStartDate(event.target.value)}
                                    type="datetime-local"
                                    value={startDate}
                                />
                            </fieldset>

                            <fieldset className="border-0">
                                <label className="mb-1 block text-sm font-medium">End Date</label>

                                <input
                                    className="w-full rounded-md border px-3 py-2 text-sm"
                                    onChange={(event) => setEndDate(event.target.value)}
                                    type="datetime-local"
                                    value={endDate}
                                />
                            </fieldset>
                        </div>

                        <div className="mt-6 flex justify-end gap-2">
                            <Button label="Cancel" onClick={() => setHistoryRule(undefined)} variant="outline" />

                            <Button
                                disabled={runHistoricalMutation.isPending}
                                label={runHistoricalMutation.isPending ? 'Queuing...' : 'Run'}
                                onClick={handleRun}
                            />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AiEvalRules;
