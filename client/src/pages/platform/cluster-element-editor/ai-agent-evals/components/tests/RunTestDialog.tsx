import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {
    type AgentEvalTestsQuery,
    AgentJudgeType,
    AgentScenarioType,
    useAgentEvalTestQuery,
    useAgentJudgesQuery,
    useStartAgentEvalRunMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {Loader2Icon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

type AgentEvalTestListItemType = AgentEvalTestsQuery['agentEvalTests'][number];

const SCENARIO_TYPE_COLORS: Record<AgentScenarioType, string> = {
    [AgentScenarioType.MultiTurn]: 'border-purple-200 bg-purple-50 text-purple-700',
    [AgentScenarioType.SingleTurn]: 'border-blue-200 bg-blue-50 text-blue-700',
};

const JUDGE_TYPE_COLORS: Record<AgentJudgeType, string> = {
    [AgentJudgeType.ContainsText]: 'border-amber-200 bg-amber-50 text-amber-700',
    [AgentJudgeType.JsonSchema]: 'border-indigo-200 bg-indigo-50 text-indigo-700',
    [AgentJudgeType.LlmRule]: 'border-blue-200 bg-blue-50 text-blue-700',
    [AgentJudgeType.RegexMatch]: 'border-purple-200 bg-purple-50 text-purple-700',
    [AgentJudgeType.ResponseLength]: 'border-green-200 bg-green-50 text-green-700',
    [AgentJudgeType.Similarity]: 'border-teal-200 bg-teal-50 text-teal-700',
    [AgentJudgeType.StringEquals]: 'border-cyan-200 bg-cyan-50 text-cyan-700',
    [AgentJudgeType.ToolUsage]: 'border-orange-200 bg-orange-50 text-orange-700',
};

interface RunTestDialogProps {
    onClose: () => void;
    test: AgentEvalTestListItemType;
    workflowId: string;
    workflowNodeName: string;
}

function generateRunName(): string {
    const now = new Date();
    const date = now.toLocaleDateString('en-US', {day: 'numeric', month: 'short'});
    const time = now.toLocaleTimeString('en-US', {hour: 'numeric', hour12: true, minute: '2-digit'});

    return `Run — ${date}, ${time}`;
}

const RunTestDialog = ({onClose, test, workflowId, workflowNodeName}: RunTestDialogProps) => {
    const [selectedJudgeIds, setSelectedJudgeIds] = useState<Set<string>>(new Set());
    const [selectedScenarioIds, setSelectedScenarioIds] = useState<Set<string>>(new Set());

    const queryClient = useQueryClient();
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setEvalsTab, setSelectedRunId, setSelectedTestId} = useAiAgentEvalsStore();

    const {data: testDetailData} = useAgentEvalTestQuery({id: test.id});
    const {data: judgesData} = useAgentJudgesQuery({workflowId, workflowNodeName});

    const scenariosInitializedRef = useRef(false);
    const judgesInitializedRef = useRef(false);

    const scenarios = useMemo(() => testDetailData?.agentEvalTest?.scenarios ?? [], [testDetailData]);
    const judges = useMemo(() => judgesData?.agentJudges ?? [], [judgesData]);

    useEffect(() => {
        if (scenarios.length > 0 && !scenariosInitializedRef.current) {
            scenariosInitializedRef.current = true;

            setSelectedScenarioIds(new Set(scenarios.map((scenario) => scenario.id)));
        }
    }, [scenarios]);

    useEffect(() => {
        if (judges.length > 0 && !judgesInitializedRef.current) {
            judgesInitializedRef.current = true;

            setSelectedJudgeIds(new Set(judges.map((judge) => judge.id)));
        }
    }, [judges]);

    const startRunMutation = useStartAgentEvalRunMutation({
        onError: (error: Error) => {
            toast.error('Failed to start eval run: ' + error.message);
        },
        onSuccess: (data) => {
            queryClient.invalidateQueries({queryKey: ['agentEvalRuns']});

            setSelectedTestId(test.id);
            setEvalsTab('runs');

            if (data.startAgentEvalRun?.id) {
                setSelectedRunId(data.startAgentEvalRun.id);
            } else {
                toast.warning('Run started but the run ID was not returned. Check the Runs tab for status.');
            }

            onClose();
        },
    });

    const toggleScenarioId = (scenarioId: string) => {
        setSelectedScenarioIds((previous) => {
            const next = new Set(previous);

            if (next.has(scenarioId)) {
                next.delete(scenarioId);
            } else {
                next.add(scenarioId);
            }

            return next;
        });
    };

    const toggleJudgeId = (judgeId: string) => {
        setSelectedJudgeIds((previous) => {
            const next = new Set(previous);

            if (next.has(judgeId)) {
                next.delete(judgeId);
            } else {
                next.add(judgeId);
            }

            return next;
        });
    };

    const handleRunTest = () => {
        startRunMutation.mutate({
            agentEvalTestId: test.id,
            agentJudgeIds: Array.from(selectedJudgeIds),
            environmentId: String(currentEnvironmentId),
            name: generateRunName(),
            scenarioIds: Array.from(selectedScenarioIds),
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent className="max-w-lg">
                <DialogHeader>
                    <DialogTitle>Run Test — {test.name}</DialogTitle>
                </DialogHeader>

                <fieldset className="flex flex-col gap-4 border-0 p-0">
                    {scenarios.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <div className="text-xs font-medium text-gray-500">Scenarios</div>

                            <div className="flex flex-col gap-1 rounded-md border border-border/50 p-2">
                                {scenarios.map((scenario) => (
                                    <label className="flex items-center gap-2 text-sm" key={scenario.id}>
                                        <input
                                            checked={selectedScenarioIds.has(scenario.id)}
                                            onChange={() => toggleScenarioId(scenario.id)}
                                            type="checkbox"
                                        />

                                        <span>{scenario.name}</span>

                                        <span
                                            className={twMerge(
                                                'rounded-full border px-1.5 py-0.5 text-[10px] font-medium',
                                                SCENARIO_TYPE_COLORS[scenario.type]
                                            )}
                                        >
                                            {scenario.type === AgentScenarioType.SingleTurn
                                                ? 'Single-turn'
                                                : 'Multi-turn'}
                                        </span>
                                    </label>
                                ))}
                            </div>
                        </div>
                    )}

                    {judges.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <div className="text-xs font-medium text-gray-500">Global Judges</div>

                            <div className="flex flex-col gap-1 rounded-md border border-border/50 p-2">
                                {judges.map((judge) => (
                                    <label className="flex items-center gap-2 text-sm" key={judge.id}>
                                        <input
                                            checked={selectedJudgeIds.has(judge.id)}
                                            onChange={() => toggleJudgeId(judge.id)}
                                            type="checkbox"
                                        />

                                        <span>{judge.name}</span>

                                        <span
                                            className={twMerge(
                                                'rounded-full border px-1.5 py-0.5 text-[10px] font-medium',
                                                JUDGE_TYPE_COLORS[judge.type]
                                            )}
                                        >
                                            {judge.type.replace(/_/g, ' ')}
                                        </span>
                                    </label>
                                ))}
                            </div>
                        </div>
                    )}
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={selectedScenarioIds.size === 0 || startRunMutation.isPending}
                        icon={startRunMutation.isPending ? <Loader2Icon className="animate-spin" /> : undefined}
                        label={startRunMutation.isPending ? 'Starting...' : 'Run Test'}
                        onClick={handleRunTest}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default RunTestDialog;
