import {
    AgentScenarioType,
    useAgentEvalTestsQuery,
    useCreateAgentEvalScenarioMutation,
    useCreateAgentEvalTestMutation,
    useDeleteAgentEvalScenarioMutation,
    useDeleteAgentEvalTestMutation,
    useUpdateAgentEvalScenarioMutation,
    useUpdateAgentEvalTestMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {toast} from 'sonner';

export default function useAgentEvalsTestsTab(workflowId: string, workflowNodeName: string) {
    const queryClient = useQueryClient();

    const {data: evalTestsData, isLoading} = useAgentEvalTestsQuery({workflowId, workflowNodeName});

    const evalTests = useMemo(() => evalTestsData?.agentEvalTests ?? [], [evalTestsData]);

    const invalidateTests = useCallback(() => {
        queryClient.invalidateQueries({queryKey: ['agentEvalTest']});
        queryClient.invalidateQueries({queryKey: ['agentEvalTests']});
    }, [queryClient]);

    const createTestMutation = useCreateAgentEvalTestMutation({
        onError: (error: Error) => toast.error('Failed to create test: ' + error.message),
        onSuccess: invalidateTests,
    });
    const updateTestMutation = useUpdateAgentEvalTestMutation({
        onError: (error: Error) => toast.error('Failed to update test: ' + error.message),
        onSuccess: invalidateTests,
    });
    const deleteTestMutation = useDeleteAgentEvalTestMutation({
        onError: (error: Error) => toast.error('Failed to delete test: ' + error.message),
        onSuccess: invalidateTests,
    });
    const createScenarioMutation = useCreateAgentEvalScenarioMutation({
        onError: (error: Error) => toast.error('Failed to create scenario: ' + error.message),
        onSuccess: invalidateTests,
    });
    const updateScenarioMutation = useUpdateAgentEvalScenarioMutation({
        onError: (error: Error) => toast.error('Failed to update scenario: ' + error.message),
        onSuccess: invalidateTests,
    });
    const deleteScenarioMutation = useDeleteAgentEvalScenarioMutation({
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
            type: AgentScenarioType,
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
