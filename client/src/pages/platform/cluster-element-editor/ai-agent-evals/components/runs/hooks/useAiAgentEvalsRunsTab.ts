import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {
    useAiAgentEvalRunQuery,
    useAiAgentEvalRunsQuery,
    useCancelAiAgentEvalRunMutation,
    useStartAiAgentEvalRunMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {toast} from 'sonner';

export default function useAiAgentEvalsRunsTab(agentEvalTestId: string | null) {
    const {selectedRunId, setSelectedRunId} = useAiAgentEvalsStore();

    const queryClient = useQueryClient();

    const {data: runsData, isLoading: runsLoading} = useAiAgentEvalRunsQuery(
        {agentEvalTestId: agentEvalTestId!},
        {enabled: agentEvalTestId != null}
    );

    const runs = useMemo(() => runsData?.aiAgentEvalRuns ?? [], [runsData]);

    const {data: selectedRunData} = useAiAgentEvalRunQuery(
        {id: selectedRunId!},
        {
            enabled: selectedRunId != null,
            refetchInterval: (query) => {
                const status = query.state.data?.aiAgentEvalRun?.status;

                return status === 'RUNNING' || status === 'PENDING' ? 2000 : false;
            },
        }
    );

    const selectedRun = useMemo(() => selectedRunData?.aiAgentEvalRun ?? null, [selectedRunData]);

    const invalidateRuns = useCallback(
        () => queryClient.invalidateQueries({queryKey: ['aiAgentEvalRuns']}),
        [queryClient]
    );

    const startRunMutation = useStartAiAgentEvalRunMutation({
        onError: (error: Error) => {
            toast.error('Failed to start eval run: ' + error.message);
        },
        onSuccess: (data) => {
            invalidateRuns();

            if (data.startAiAgentEvalRun?.id) {
                setSelectedRunId(data.startAiAgentEvalRun.id);
            }
        },
    });

    const cancelRunMutation = useCancelAiAgentEvalRunMutation({
        onError: (error: Error) => {
            toast.error('Failed to cancel eval run: ' + error.message);
        },
        onSuccess: () => {
            invalidateRuns();
            queryClient.invalidateQueries({queryKey: ['aiAgentEvalRun']});
        },
    });

    const handleStartRun = useCallback(
        (testId: string, name: string, environmentId: string, scenarioIds?: string[], aiAgentJudgeIds?: string[]) => {
            startRunMutation.mutate({agentEvalTestId: testId, aiAgentJudgeIds, environmentId, name, scenarioIds});
        },
        [startRunMutation]
    );

    const handleCancelRun = useCallback(
        (id: string) => {
            cancelRunMutation.mutate({id});
        },
        [cancelRunMutation]
    );

    const handleSelectRun = useCallback(
        (id: string | null) => {
            if (id === null) {
                queryClient.invalidateQueries({queryKey: ['aiAgentEvalRuns']});
            }

            setSelectedRunId(id);
        },
        [queryClient, setSelectedRunId]
    );

    const runSummary = useMemo(() => {
        if (!selectedRun?.results) {
            return null;
        }

        const results = selectedRun.results;

        const passedCount = results.filter(
            (result) => result.status === 'COMPLETED' && (result.score ?? 0) >= 1.0
        ).length;

        const failedCount = results.filter(
            (result) => result.status === 'COMPLETED' && (result.score ?? 0) < 1.0
        ).length;

        const errorCount = results.filter((result) => result.status === 'FAILED').length;

        return {
            agentVersion: selectedRun.agentVersion,
            averageScore: selectedRun.averageScore,
            errorCount,
            failedCount,
            passedCount,
            totalInputTokens: selectedRun.totalInputTokens,
            totalOutputTokens: selectedRun.totalOutputTokens,
            totalScenarios: selectedRun.totalScenarios,
        };
    }, [selectedRun]);

    return {
        cancelRunMutation,
        handleCancelRun,
        handleSelectRun,
        handleStartRun,
        runSummary,
        runs,
        runsLoading,
        selectedRun,
        selectedRunId,
        startRunMutation,
    };
}
