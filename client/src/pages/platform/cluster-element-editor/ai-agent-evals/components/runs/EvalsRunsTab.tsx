import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import AgentEvalRunDetail from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AgentEvalRunDetail';
import AgentEvalRunList from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AgentEvalRunList';
import useAgentEvalsRunsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAgentEvalsRunsTab';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {useAgentEvalTestsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Loader2Icon, PlayCircleIcon, PlayIcon} from 'lucide-react';
import {useEffect, useMemo} from 'react';

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

const EvalsRunsTab = ({workflowId, workflowNodeName}: EvalsRunsTabProps) => {
    const {selectedTestId, setSelectedRunId, setSelectedTestId} = useAiAgentEvalsStore();
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: evalTestsData} = useAgentEvalTestsQuery({workflowId, workflowNodeName});

    const evalTests = useMemo(() => evalTestsData?.agentEvalTests ?? [], [evalTestsData]);

    const validSelectedTestId = useMemo(() => {
        if (selectedTestId == null) {
            return null;
        }

        return evalTests.some((evalTest) => evalTest.id === selectedTestId) ? selectedTestId : null;
    }, [evalTests, selectedTestId]);

    useEffect(() => {
        if (validSelectedTestId !== selectedTestId) {
            setSelectedTestId(null);
            setSelectedRunId(null);
        }
    }, [validSelectedTestId, selectedTestId, setSelectedTestId, setSelectedRunId]);

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

    if (selectedRunId && selectedRun) {
        return <AgentEvalRunDetail onBack={() => handleSelectRun(null)} run={selectedRun} summary={runSummary} />;
    }

    return (
        <div className="flex flex-1 flex-col gap-3">
            <div className="flex items-center gap-2 px-4">
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
                    disabled={validSelectedTestId == null}
                    onClick={() =>
                        handleStartRun(validSelectedTestId!, generateRunName(), String(currentEnvironmentId))
                    }
                >
                    <PlayIcon className="size-3.5" />
                    Run Test
                </button>
            </div>

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
