import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {
    useIntegrationCodeWorkflowSourceQuery,
    useUpdateIntegrationCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';

interface IntegrationCodeWorkflowDetailProps {
    integrationId: string;
    language: string;
}

const IntegrationCodeWorkflowDetail = ({integrationId, language}: IntegrationCodeWorkflowDetailProps) => {
    const queryClient = useQueryClient();

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useIntegrationCodeWorkflowSourceQuery({integrationId}, {enabled: !!integrationId});

    const updateIntegrationCodeWorkflowSourceMutation = useUpdateIntegrationCodeWorkflowSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['integrationCodeWorkflowSource', {integrationId}]});
        },
    });

    useEffect(() => {
        // A Copilot BUILD turn can rewrite the source server-side; refresh the editor after each turn so
        // Monaco does not keep showing stale text.
        return useCopilotPostTurnRegistry.getState().register(Source.CODE_WORKFLOW_EMBEDDED, () => {
            queryClient.invalidateQueries({queryKey: ['integrationCodeWorkflowSource', {integrationId}]});
        });
    }, [integrationId, queryClient]);

    return (
        <CodeWorkflowSourceEditor
            error={sourceError}
            isLoading={sourceLoading}
            isSaving={updateIntegrationCodeWorkflowSourceMutation.isPending}
            language={language}
            onSave={(content) => updateIntegrationCodeWorkflowSourceMutation.mutateAsync({content, integrationId})}
            source={sourceData?.integrationCodeWorkflowSource ?? undefined}
        />
    );
};

export default IntegrationCodeWorkflowDetail;
