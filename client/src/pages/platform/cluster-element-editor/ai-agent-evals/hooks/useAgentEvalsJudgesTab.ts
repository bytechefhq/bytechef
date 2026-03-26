import {
    type AgentJudgeType,
    useAgentJudgesQuery,
    useCreateAgentJudgeMutation,
    useDeleteAgentJudgeMutation,
    useUpdateAgentJudgeMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';

export default function useAgentEvalsJudgesTab(workflowId: string, workflowNodeName: string) {
    const queryClient = useQueryClient();

    const {data: judgesData, isLoading} = useAgentJudgesQuery({workflowId, workflowNodeName});

    const judges = useMemo(() => judgesData?.agentJudges ?? [], [judgesData]);

    const invalidateJudges = useCallback(
        () => queryClient.invalidateQueries({queryKey: ['agentJudges']}),
        [queryClient]
    );

    const createJudgeMutation = useCreateAgentJudgeMutation({onSuccess: invalidateJudges});
    const updateJudgeMutation = useUpdateAgentJudgeMutation({onSuccess: invalidateJudges});
    const deleteJudgeMutation = useDeleteAgentJudgeMutation({onSuccess: invalidateJudges});

    const handleCreateJudge = useCallback(
        (name: string, type: AgentJudgeType, configuration: Record<string, unknown>) => {
            createJudgeMutation.mutate({configuration, name, type, workflowId, workflowNodeName});
        },
        [createJudgeMutation, workflowId, workflowNodeName]
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

    return {
        createJudgeMutation,
        handleCreateJudge,
        handleDeleteJudge,
        handleUpdateJudge,
        isLoading,
        judges,
        updateJudgeMutation,
    };
}
