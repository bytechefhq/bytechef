import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import {
    useIntegrationCodeWorkflowSourceQuery,
    useUpdateIntegrationCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

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
