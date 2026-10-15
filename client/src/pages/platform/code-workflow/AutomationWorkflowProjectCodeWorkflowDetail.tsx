import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import {
    useAutomationWorkflowProjectCodeWorkflowSourceQuery,
    useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface AutomationWorkflowProjectCodeWorkflowDetailProps {
    automationProjectId: string;
    headerless?: boolean;
    invalidateWorkflowQueries?: () => void;
    language: string;
    onTestConfigurationClick?: () => void;
    testConfigurationDisabled?: boolean;
}

const AutomationWorkflowProjectCodeWorkflowDetail = ({
    automationProjectId,
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
        {automationProjectId},
        {enabled: !!automationProjectId}
    );

    const updateAutomationWorkflowProjectCodeWorkflowSourceMutation =
        useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectCodeWorkflowSource']});
                queryClient.invalidateQueries({queryKey: ['automationProjects']});

                invalidateWorkflowQueries?.();
            },
        });

    return (
        <CodeWorkflowSourceEditor
            error={sourceError}
            headerless={headerless}
            isLoading={sourceLoading}
            isSaving={updateAutomationWorkflowProjectCodeWorkflowSourceMutation.isPending}
            language={language}
            onSave={(content) =>
                updateAutomationWorkflowProjectCodeWorkflowSourceMutation.mutateAsync({
                    automationProjectId,
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
