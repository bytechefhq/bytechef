import {
    type AiAgentJudgeType,
    useAiAgentJudgesQuery,
    useCreateAiAgentJudgeMutation,
    useDeleteAiAgentJudgeMutation,
    useUpdateAiAgentJudgeMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';

export default function useAiAgentEvalsJudgesTab(workflowId: string, workflowNodeName: string) {
    const queryClient = useQueryClient();

    const {data: judgesData, isLoading} = useAiAgentJudgesQuery({workflowId, workflowNodeName});

    const judges = useMemo(() => judgesData?.aiAgentJudges ?? [], [judgesData]);

    const invalidateJudges = useCallback(
        () => queryClient.invalidateQueries({queryKey: ['aiAgentJudges']}),
        [queryClient]
    );

    const createJudgeMutation = useCreateAiAgentJudgeMutation({onSuccess: invalidateJudges});
    const updateJudgeMutation = useUpdateAiAgentJudgeMutation({onSuccess: invalidateJudges});
    const deleteJudgeMutation = useDeleteAiAgentJudgeMutation({onSuccess: invalidateJudges});

    const handleCreateJudge = useCallback(
        (name: string, type: AiAgentJudgeType, configuration: Record<string, unknown>) => {
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
