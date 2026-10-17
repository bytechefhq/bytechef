import AiAgentEvalRunDetail from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AiAgentEvalRunDetail';
import AiAgentEvalRunList from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/AiAgentEvalRunList';
import useEvalsRunsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/hooks/useEvalsRunsTab';
import {Loader2Icon, PlayCircleIcon} from 'lucide-react';

interface EvalsRunsTabProps {
    workflowId: string;
    workflowNodeName: string;
}

const EvalsRunsTab = ({workflowId, workflowNodeName}: EvalsRunsTabProps) => {
    const {
        handleCancelRun,
        handleSelectRun,
        runSummary,
        runs,
        runsLoading,
        selectedRun,
        selectedRunId,
        selectedTestId,
    } = useEvalsRunsTab(workflowId, workflowNodeName);

    if (selectedRunId && selectedRun) {
        return <AiAgentEvalRunDetail onBack={() => handleSelectRun(null)} run={selectedRun} summary={runSummary} />;
    }

    if (selectedTestId == null) {
        return (
            <div className="flex flex-1 flex-col items-center justify-center gap-3 px-4">
                <div className="flex size-12 items-center justify-center rounded-full bg-surface-brand-secondary">
                    <PlayCircleIcon className="size-6 text-content-brand-primary" />
                </div>

                <h3 className="text-sm font-semibold">No tests yet</h3>

                <p className="max-w-xs text-center text-xs text-content-neutral-secondary">
                    Create a test in the Tests tab, then run it to see results here.
                </p>
            </div>
        );
    }

    if (runsLoading) {
        return (
            <div className="flex flex-1 items-center justify-center py-12">
                <Loader2Icon className="size-5 animate-spin text-content-neutral-tertiary" />
            </div>
        );
    }

    return (
        <div className="flex flex-1 flex-col gap-3 px-4">
            <div className="text-xs text-content-neutral-secondary">
                {runs.length} {runs.length === 1 ? 'run' : 'runs'}
            </div>

            <AiAgentEvalRunList onCancelRun={handleCancelRun} onSelectRun={handleSelectRun} runs={runs} />
        </div>
    );
};

export default EvalsRunsTab;
