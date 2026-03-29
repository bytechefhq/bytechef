import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import AgentEvalRunDetail from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AgentEvalRunDetail';
import AgentEvalRunList from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AgentEvalRunList';
import useAgentEvalsRunsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAgentEvalsRunsTab';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {
    AgentJudgeType,
    AgentScenarioType,
    useAgentEvalTestsQuery,
    useAgentJudgesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Loader2Icon, PlayCircleIcon, PlayIcon} from 'lucide-react';
import {useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface EvalsRunsTabProps {
    workflowId: string;
    workflowNodeName: string;
}

function generateRunName(): string {
    const now = new Date();

    const date = now.toLocaleDateString('en-US', {day: 'numeric', month: 'short'});
    const time = now.toLocaleTimeString('en-US', {hour: 'numeric', hour12: true, minute: '2-digit'});

    return `Run — ${date}, ${time}`;
}

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

const EvalsRunsTab = ({workflowId, workflowNodeName}: EvalsRunsTabProps) => {
    const [selectedJudgeIds, setSelectedJudgeIds] = useState<Set<string>>(new Set());
    const [selectedScenarioIds, setSelectedScenarioIds] = useState<Set<string>>(new Set());

    const {selectedTestId, setSelectedRunId, setSelectedTestId} = useAiAgentEvalsStore();
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: evalTestsData} = useAgentEvalTestsQuery({workflowId, workflowNodeName});
    const {data: judgesData} = useAgentJudgesQuery({workflowId, workflowNodeName});

    const evalTests = useMemo(() => evalTestsData?.agentEvalTests ?? [], [evalTestsData]);
    const judges = useMemo(() => judgesData?.agentJudges ?? [], [judgesData]);

    const validSelectedTestId = useMemo(() => {
        if (selectedTestId == null) {
            return null;
        }

        return evalTests.some((evalTest) => evalTest.id === selectedTestId) ? selectedTestId : null;
    }, [evalTests, selectedTestId]);

    const selectedTest = useMemo(
        () => evalTests.find((evalTest) => evalTest.id === validSelectedTestId) ?? null,
        [evalTests, validSelectedTestId]
    );

    const scenarios = useMemo(() => selectedTest?.scenarios ?? [], [selectedTest]);

    useEffect(() => {
        if (validSelectedTestId !== selectedTestId) {
            setSelectedTestId(null);
            setSelectedRunId(null);
        }
    }, [validSelectedTestId, selectedTestId, setSelectedTestId, setSelectedRunId]);

    useEffect(() => {
        setSelectedScenarioIds(new Set(scenarios.map((scenario) => scenario.id)));
    }, [scenarios]);

    useEffect(() => {
        setSelectedJudgeIds(new Set(judges.map((judge) => judge.id)));
    }, [judges]);

    const {
        handleCancelRun,
        handleSelectRun,
        handleStartRun,
        runSummary,
        runs,
        runsLoading,
        selectedRun,
        selectedRunId,
    } = useAgentEvalsRunsTab(validSelectedTestId);

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

    if (selectedRunId && selectedRun) {
        return <AgentEvalRunDetail onBack={() => handleSelectRun(null)} run={selectedRun} summary={runSummary} />;
    }

    return (
        <div className="flex flex-1 flex-col gap-3 px-4">
            <div className="flex items-center gap-2">
                <Select onValueChange={(value) => setSelectedTestId(value || null)} value={validSelectedTestId ?? ''}>
                    <SelectTrigger className="w-[400px] text-xs">
                        <SelectValue placeholder="Select a test..." />
                    </SelectTrigger>

                    <SelectContent>
                        {evalTests.map((evalTest) => (
                            <SelectItem key={evalTest.id} value={evalTest.id}>
                                {evalTest.name}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>

                <button
                    className="inline-flex items-center gap-1.5 rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                    disabled={validSelectedTestId == null || selectedScenarioIds.size === 0}
                    onClick={() =>
                        handleStartRun(
                            validSelectedTestId!,
                            generateRunName(),
                            String(currentEnvironmentId),
                            Array.from(selectedScenarioIds),
                            Array.from(selectedJudgeIds)
                        )
                    }
                >
                    <PlayIcon className="size-3.5" />
                    Run Test
                </button>
            </div>

            {validSelectedTestId != null && scenarios.length > 0 && (
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
                                    {scenario.type === AgentScenarioType.SingleTurn ? 'Single-turn' : 'Multi-turn'}
                                </span>
                            </label>
                        ))}
                    </div>
                </div>
            )}

            {validSelectedTestId != null && judges.length > 0 && (
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

            {validSelectedTestId == null ? (
                <div className="flex flex-1 flex-col items-center justify-center gap-3">
                    <div className="flex size-12 items-center justify-center rounded-full bg-blue-100">
                        <PlayCircleIcon className="size-6 text-blue-600" />
                    </div>

                    <h3 className="text-sm font-semibold">No test selected</h3>

                    <p className="max-w-xs text-center text-xs text-gray-500">
                        Select a test from the dropdown above to view previous runs or start a new evaluation run.
                    </p>
                </div>
            ) : runsLoading ? (
                <div className="flex flex-1 items-center justify-center py-12">
                    <Loader2Icon className="size-5 animate-spin text-gray-400" />
                </div>
            ) : (
                <>
                    <div className="text-xs text-gray-500">
                        {runs.length} {runs.length === 1 ? 'run' : 'runs'}
                    </div>

                    <AgentEvalRunList onCancelRun={handleCancelRun} onSelectRun={handleSelectRun} runs={runs} />
                </>
            )}
        </div>
    );
};

export default EvalsRunsTab;
