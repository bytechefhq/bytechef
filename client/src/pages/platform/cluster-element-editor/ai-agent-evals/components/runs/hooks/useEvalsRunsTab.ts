import useAiAgentEvalsRunsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/runs/hooks/useAiAgentEvalsRunsTab';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {useAiAgentEvalTestsQuery} from '@/shared/middleware/graphql';
import {useEffect, useMemo} from 'react';

export default function useEvalsRunsTab(workflowId: string, workflowNodeName: string) {
    const {selectedTestId, setSelectedTestId} = useAiAgentEvalsStore();

    const {data: evalTestsData} = useAiAgentEvalTestsQuery({workflowId, workflowNodeName});

    const evalTests = useMemo(() => evalTestsData?.aiAgentEvalTests ?? [], [evalTestsData]);

    useEffect(() => {
        if (selectedTestId == null && evalTests.length > 0) {
            setSelectedTestId(evalTests[0].id);
        }
    }, [evalTests, selectedTestId, setSelectedTestId]);

    const {handleCancelRun, handleSelectRun, runSummary, runs, runsLoading, selectedRun, selectedRunId} =
        useAiAgentEvalsRunsTab(selectedTestId);

    return {
        handleCancelRun,
        handleSelectRun,
        runSummary,
        runs,
        runsLoading,
        selectedRun,
        selectedRunId,
        selectedTestId,
    };
}
