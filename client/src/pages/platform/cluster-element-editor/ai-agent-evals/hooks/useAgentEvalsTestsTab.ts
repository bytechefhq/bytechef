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

export default function useAgentEvalsTestsTab(workflowId: string, workflowNodeName: string) {
    const queryClient = useQueryClient();

    const {data: evalTestsData, isLoading} = useAgentEvalTestsQuery({workflowId, workflowNodeName});

    const evalTests = useMemo(() => evalTestsData?.agentEvalTests ?? [], [evalTestsData]);

    const invalidateTests = useCallback(() => {
        queryClient.invalidateQueries({queryKey: ['agentEvalTest']});
        queryClient.invalidateQueries({queryKey: ['agentEvalTests']});
    }, [queryClient]);

    const createTestMutation = useCreateAgentEvalTestMutation({onSuccess: invalidateTests});
    const updateTestMutation = useUpdateAgentEvalTestMutation({onSuccess: invalidateTests});
    const deleteTestMutation = useDeleteAgentEvalTestMutation({onSuccess: invalidateTests});
    const createScenarioMutation = useCreateAgentEvalScenarioMutation({onSuccess: invalidateTests});
    const updateScenarioMutation = useUpdateAgentEvalScenarioMutation({onSuccess: invalidateTests});
    const deleteScenarioMutation = useDeleteAgentEvalScenarioMutation({onSuccess: invalidateTests});

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
                personaPrompt?: string;
                userMessage?: string;
            }
        ) => {
            createScenarioMutation.mutate({agentEvalTestId, name, type, ...fields});
        },
        [createScenarioMutation]
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
        isLoading,
        updateScenarioMutation,
        updateTestMutation,
    };
}
