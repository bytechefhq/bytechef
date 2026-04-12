import {
    AiAgentJudgeType,
    useCreateAiAgentScenarioJudgeMutation,
    useCreateAiAgentScenarioToolSimulationMutation,
    useDeleteAiAgentScenarioJudgeMutation,
    useDeleteAiAgentScenarioToolSimulationMutation,
    useUpdateAiAgentScenarioJudgeMutation,
    useUpdateAiAgentScenarioToolSimulationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';
import {toast} from 'sonner';

export default function useAiAgentEvalScenarioRow(scenarioId: string) {
    const queryClient = useQueryClient();

    const invalidateTest = useCallback(
        () => queryClient.invalidateQueries({queryKey: ['aiAgentEvalTest']}),
        [queryClient]
    );

    const createJudgeMutation = useCreateAiAgentScenarioJudgeMutation({
        onError: (error: Error) => toast.error('Failed to create judge: ' + error.message),
        onSuccess: invalidateTest,
    });
    const deleteJudgeMutation = useDeleteAiAgentScenarioJudgeMutation({
        onError: (error: Error) => toast.error('Failed to delete judge: ' + error.message),
        onSuccess: invalidateTest,
    });
    const updateJudgeMutation = useUpdateAiAgentScenarioJudgeMutation({
        onError: (error: Error) => toast.error('Failed to update judge: ' + error.message),
        onSuccess: invalidateTest,
    });

    const createToolSimulationMutation = useCreateAiAgentScenarioToolSimulationMutation({
        onError: (error: Error) => toast.error('Failed to create tool simulation: ' + error.message),
        onSuccess: invalidateTest,
    });
    const deleteToolSimulationMutation = useDeleteAiAgentScenarioToolSimulationMutation({
        onError: (error: Error) => toast.error('Failed to delete tool simulation: ' + error.message),
        onSuccess: invalidateTest,
    });
    const updateToolSimulationMutation = useUpdateAiAgentScenarioToolSimulationMutation({
        onError: (error: Error) => toast.error('Failed to update tool simulation: ' + error.message),
        onSuccess: invalidateTest,
    });

    const handleCreateJudge = useCallback(
        (name: string, type: AiAgentJudgeType, configuration: Record<string, unknown>) => {
            createJudgeMutation.mutate({
                agentEvalScenarioId: scenarioId,
                configuration,
                name,
                type,
            });
        },
        [createJudgeMutation, scenarioId]
    );

    const handleUpdateJudge = useCallback(
        (id: string, name?: string, configuration?: Record<string, unknown>) => {
            updateJudgeMutation.mutate({configuration, id, name});
        },
        [updateJudgeMutation]
    );

    const handleDeleteJudge = useCallback(
        (id: string) => {
            deleteJudgeMutation.mutate({id});
        },
        [deleteJudgeMutation]
    );

    const handleCreateToolSimulation = useCallback(
        async (toolName: string, responsePrompt: string, simulationModel?: string) => {
            await createToolSimulationMutation.mutateAsync({
                agentEvalScenarioId: scenarioId,
                responsePrompt,
                simulationModel,
                toolName,
            });
        },
        [createToolSimulationMutation, scenarioId]
    );

    const handleDeleteToolSimulation = useCallback(
        (id: string) => {
            deleteToolSimulationMutation.mutate({id});
        },
        [deleteToolSimulationMutation]
    );

    const handleUpdateToolSimulation = useCallback(
        async (id: string, toolName?: string, responsePrompt?: string, simulationModel?: string) => {
            await updateToolSimulationMutation.mutateAsync({id, responsePrompt, simulationModel, toolName});
        },
        [updateToolSimulationMutation]
    );

    return {
        handleCreateJudge,
        handleCreateToolSimulation,
        handleDeleteJudge,
        handleDeleteToolSimulation,
        handleUpdateJudge,
        handleUpdateToolSimulation,
    };
}
