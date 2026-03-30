import AgentEvalRunDetail from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AgentEvalRunDetail';
import AgentEvalRunList from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AgentEvalRunList';
import useAgentEvalsRunsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAgentEvalsRunsTab';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {useAgentEvalTestsQuery} from '@/shared/middleware/graphql';
import {Loader2Icon, PlayCircleIcon} from 'lucide-react';
import {useEffect, useMemo} from 'react';

interface EvalsRunsTabProps {
    workflowId: string;
    workflowNodeName: string;
}

const EvalsRunsTab = ({workflowId, workflowNodeName}: EvalsRunsTabProps) => {
    const {selectedTestId, setSelectedTestId} = useAiAgentEvalsStore();

    const {data: evalTestsData} = useAgentEvalTestsQuery({workflowId, workflowNodeName});

    const evalTests = useMemo(() => evalTestsData?.agentEvalTests ?? [], [evalTestsData]);

    useEffect(() => {
        if (selectedTestId == null && evalTests.length > 0) {
            setSelectedTestId(evalTests[0].id);
        }
    }, [evalTests, selectedTestId, setSelectedTestId]);

    const {handleCancelRun, handleSelectRun, runSummary, runs, runsLoading, selectedRun, selectedRunId} =
        useAgentEvalsRunsTab(selectedTestId);

    if (selectedRunId && selectedRun) {
        return <AgentEvalRunDetail onBack={() => handleSelectRun(null)} run={selectedRun} summary={runSummary} />;
    }

    if (selectedTestId == null) {
        return (
            <div className="flex flex-1 flex-col items-center justify-center gap-3 px-4">
                <div className="flex size-12 items-center justify-center rounded-full bg-blue-100">
                    <PlayCircleIcon className="size-6 text-blue-600" />
                </div>

                <h3 className="text-sm font-semibold">No tests yet</h3>

                <p className="max-w-xs text-center text-xs text-gray-500">
                    Create a test in the Tests tab, then run it to see results here.
                </p>
            </div>
        );
    }

    if (runsLoading) {
        return (
            <div className="flex flex-1 items-center justify-center py-12">
                <Loader2Icon className="size-5 animate-spin text-gray-400" />
            </div>
        );
    }

    return (
        <div className="flex flex-1 flex-col gap-3 px-4">
            <div className="text-xs text-gray-500">
                {runs.length} {runs.length === 1 ? 'run' : 'runs'}
            </div>

            <AgentEvalRunList onCancelRun={handleCancelRun} onSelectRun={handleSelectRun} runs={runs} />
        </div>
    );
};

export default EvalsRunsTab;
