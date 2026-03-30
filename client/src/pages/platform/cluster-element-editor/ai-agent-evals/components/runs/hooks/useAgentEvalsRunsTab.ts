import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
import {
    useAgentEvalRunQuery,
    useAgentEvalRunsQuery,
    useCancelAgentEvalRunMutation,
    useStartAgentEvalRunMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {toast} from 'sonner';

export default function useAgentEvalsRunsTab(agentEvalTestId: string | null) {
    const queryClient = useQueryClient();
    const {selectedRunId, setSelectedRunId} = useAiAgentEvalsStore();

    const {data: runsData, isLoading: runsLoading} = useAgentEvalRunsQuery(
        {agentEvalTestId: agentEvalTestId!},
        {enabled: agentEvalTestId != null}
    );

    const runs = useMemo(() => runsData?.agentEvalRuns ?? [], [runsData]);

    const {data: selectedRunData} = useAgentEvalRunQuery(
        {id: selectedRunId!},
        {
            enabled: selectedRunId != null,
            refetchInterval: (query) => {
                const status = query.state.data?.agentEvalRun?.status;

                return status === 'RUNNING' || status === 'PENDING' ? 2000 : false;
            },
        }
    );

    const selectedRun = useMemo(() => selectedRunData?.agentEvalRun ?? null, [selectedRunData]);

    const invalidateRuns = useCallback(
        () => queryClient.invalidateQueries({queryKey: ['agentEvalRuns']}),
        [queryClient]
    );

    const startRunMutation = useStartAgentEvalRunMutation({
        onError: (error: Error) => {
            toast.error('Failed to start eval run: ' + error.message);
        },
        onSuccess: (data) => {
            invalidateRuns();

            if (data.startAgentEvalRun?.id) {
                setSelectedRunId(data.startAgentEvalRun.id);
            }
        },
    });

    const cancelRunMutation = useCancelAgentEvalRunMutation({
        onError: (error: Error) => {
            toast.error('Failed to cancel eval run: ' + error.message);
        },
        onSuccess: () => {
            invalidateRuns();
            queryClient.invalidateQueries({queryKey: ['agentEvalRun']});
        },
    });

    const handleStartRun = useCallback(
        (testId: string, name: string, environmentId: string, scenarioIds?: string[], agentJudgeIds?: string[]) => {
            startRunMutation.mutate({agentEvalTestId: testId, agentJudgeIds, environmentId, name, scenarioIds});
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
                queryClient.invalidateQueries({queryKey: ['agentEvalRuns']});
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
