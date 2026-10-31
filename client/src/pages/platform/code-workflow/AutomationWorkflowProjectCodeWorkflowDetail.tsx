import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import {
    useAutomationWorkflowProjectCodeWorkflowSourceQuery,
    useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface AutomationWorkflowProjectCodeWorkflowDetailProps {
    automationWorkflowProjectId: string;
    headerless?: boolean;
    invalidateWorkflowQueries?: () => void;
    language: string;
    onTestConfigurationClick?: () => void;
    testConfigurationDisabled?: boolean;
}

const AutomationWorkflowProjectCodeWorkflowDetail = ({
    automationWorkflowProjectId,
    headerless,
    invalidateWorkflowQueries,
    language,
    onTestConfigurationClick,
    testConfigurationDisabled,
}: AutomationWorkflowProjectCodeWorkflowDetailProps) => {
    const queryClient = useQueryClient();

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useAutomationWorkflowProjectCodeWorkflowSourceQuery(
        {automationWorkflowProjectId},
        {enabled: !!automationWorkflowProjectId}
    );

    const updateAutomationWorkflowProjectCodeWorkflowSourceMutation =
        useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectCodeWorkflowSource']});
                queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                invalidateWorkflowQueries?.();
            },
        });

    return (
        <CodeWorkflowSourceEditor
            embedded
            error={sourceError}
            headerless={headerless}
            isLoading={sourceLoading}
            isSaving={updateAutomationWorkflowProjectCodeWorkflowSourceMutation.isPending}
            language={language}
            onSave={(content) =>
                updateAutomationWorkflowProjectCodeWorkflowSourceMutation.mutateAsync({
                    automationWorkflowProjectId,
                    content,
                })
            }
            onTestConfigurationClick={onTestConfigurationClick}
            source={sourceData?.automationWorkflowProjectCodeWorkflowSource.content}
            testConfigurationDisabled={testConfigurationDisabled}
        />
    );
};

export default AutomationWorkflowProjectCodeWorkflowDetail;
