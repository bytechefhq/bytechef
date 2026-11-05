import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import {
    useIntegrationCodeWorkflowSourceQuery,
    useUpdateIntegrationCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface IntegrationCodeWorkflowDetailProps {
    headerless?: boolean;
    integrationId: string;
    invalidateWorkflowQueries?: () => void;
    language: string;
    onTestConfigurationClick?: () => void;
    testConfigurationDisabled?: boolean;
}

const IntegrationCodeWorkflowDetail = ({
    headerless,
    integrationId,
    invalidateWorkflowQueries,
    language,
    onTestConfigurationClick,
    testConfigurationDisabled,
}: IntegrationCodeWorkflowDetailProps) => {
    const queryClient = useQueryClient();

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useIntegrationCodeWorkflowSourceQuery({integrationId}, {enabled: !!integrationId});

    const updateIntegrationCodeWorkflowSourceMutation = useUpdateIntegrationCodeWorkflowSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['integrationCodeWorkflowSource', {integrationId}]});

            invalidateWorkflowQueries?.();
        },
    });

    return (
        <CodeWorkflowSourceEditor
            embedded
            error={sourceError}
            headerless={headerless}
            isLoading={sourceLoading}
            isSaving={updateIntegrationCodeWorkflowSourceMutation.isPending}
            language={language}
            onSave={(content) => updateIntegrationCodeWorkflowSourceMutation.mutateAsync({content, integrationId})}
            onTestConfigurationClick={onTestConfigurationClick}
            source={sourceData?.integrationCodeWorkflowSource ?? undefined}
            testConfigurationDisabled={testConfigurationDisabled}
        />
    );
};

export default IntegrationCodeWorkflowDetail;
