import {
    AiAgentScenarioType,
    useAiAgentEvalTestsQuery,
    useCreateAiAgentEvalScenarioMutation,
    useCreateAiAgentEvalTestMutation,
    useDeleteAiAgentEvalScenarioMutation,
    useDeleteAiAgentEvalTestMutation,
    useUpdateAiAgentEvalScenarioMutation,
    useUpdateAiAgentEvalTestMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {toast} from 'sonner';

export default function useAiAgentEvalsTestsTab(workflowId: string, workflowNodeName: string) {
    const queryClient = useQueryClient();

    const {data: evalTestsData, isLoading} = useAiAgentEvalTestsQuery({workflowId, workflowNodeName});

    const evalTests = useMemo(() => evalTestsData?.aiAgentEvalTests ?? [], [evalTestsData]);

    const invalidateTests = useCallback(() => {
        queryClient.invalidateQueries({queryKey: ['aiAgentEvalTest']});
        queryClient.invalidateQueries({queryKey: ['aiAgentEvalTests']});
    }, [queryClient]);

    const createTestMutation = useCreateAiAgentEvalTestMutation({
        onError: (error: Error) => toast.error('Failed to create test: ' + error.message),
        onSuccess: invalidateTests,
    });
    const updateTestMutation = useUpdateAiAgentEvalTestMutation({
        onError: (error: Error) => toast.error('Failed to update test: ' + error.message),
        onSuccess: invalidateTests,
    });
    const deleteTestMutation = useDeleteAiAgentEvalTestMutation({
        onError: (error: Error) => toast.error('Failed to delete test: ' + error.message),
        onSuccess: invalidateTests,
    });
    const createScenarioMutation = useCreateAiAgentEvalScenarioMutation({
        onError: (error: Error) => toast.error('Failed to create scenario: ' + error.message),
        onSuccess: invalidateTests,
    });
    const updateScenarioMutation = useUpdateAiAgentEvalScenarioMutation({
        onError: (error: Error) => toast.error('Failed to update scenario: ' + error.message),
        onSuccess: invalidateTests,
    });
    const deleteScenarioMutation = useDeleteAiAgentEvalScenarioMutation({
        onError: (error: Error) => toast.error('Failed to delete scenario: ' + error.message),
        onSuccess: invalidateTests,
    });

    const handleCreateTest = useCallback(
        (name: string, description?: string) => {
            createTestMutation.mutate({description, name, workflowId, workflowNodeName});
        },
        [createTestMutation, workflowId, workflowNodeName]
    );

    const handleDeleteTest = useCallback(
        (id: string) => {
            deleteTestMutation.mutate({id});
        },
        [deleteTestMutation]
    );

    const handleCreateScenario = useCallback(
        (
            agentEvalTestId: string,
            name: string,
            type: AiAgentScenarioType,
            fields: {
                expectedOutput?: string;
                maxTurns?: number;
                numberOfRuns?: number;
                personaPrompt?: string;
                userMessage?: string;
            }
        ) => {
            createScenarioMutation.mutate({agentEvalTestId, name, type, ...fields});
        },
        [createScenarioMutation]
    );

    const handleUpdateScenario = useCallback(
        (
            id: string,
            name: string,
            fields: {
                expectedOutput?: string;
                maxTurns?: number;
                personaPrompt?: string;
                userMessage?: string;
            }
        ) => {
            updateScenarioMutation.mutate({id, name, ...fields});
        },
        [updateScenarioMutation]
    );

    const handleDeleteScenario = useCallback(
        (id: string) => {
            deleteScenarioMutation.mutate({id});
        },
        [deleteScenarioMutation]
    );

    return {
        createScenarioMutation,
        createTestMutation,
        evalTests,
        handleCreateScenario,
        handleCreateTest,
        handleDeleteScenario,
        handleDeleteTest,
        handleUpdateScenario,
        isLoading,
        updateScenarioMutation,
        updateTestMutation,
    };
}
