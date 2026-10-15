import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import {
    useAutomationProjectCodeWorkflowSourceQuery,
    useUpdateAutomationProjectCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface AutomationProjectCodeWorkflowDetailProps {
    automationProjectId: string;
    headerless?: boolean;
    invalidateWorkflowQueries?: () => void;
    language: string;
    onTestConfigurationClick?: () => void;
    testConfigurationDisabled?: boolean;
}

const AutomationProjectCodeWorkflowDetail = ({
    automationProjectId,
    headerless,
    invalidateWorkflowQueries,
    language,
    onTestConfigurationClick,
    testConfigurationDisabled,
}: AutomationProjectCodeWorkflowDetailProps) => {
    const queryClient = useQueryClient();

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useAutomationProjectCodeWorkflowSourceQuery(
        {automationProjectId},
        {enabled: !!automationProjectId}
    );

    const updateAutomationProjectCodeWorkflowSourceMutation =
        useUpdateAutomationProjectCodeWorkflowSourceMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({queryKey: ['automationProjectCodeWorkflowSource']});
                queryClient.invalidateQueries({queryKey: ['automationProjects']});

                invalidateWorkflowQueries?.();
            },
        });

    return (
        <CodeWorkflowSourceEditor
            error={sourceError}
            headerless={headerless}
            isLoading={sourceLoading}
            isSaving={updateAutomationProjectCodeWorkflowSourceMutation.isPending}
            language={language}
            onSave={(content) =>
                updateAutomationProjectCodeWorkflowSourceMutation.mutateAsync({
                    automationProjectId,
                    content,
                })
            }
            onTestConfigurationClick={onTestConfigurationClick}
            source={sourceData?.automationProjectCodeWorkflowSource.content}
            testConfigurationDisabled={testConfigurationDisabled}
        />
    );
};

export default AutomationProjectCodeWorkflowDetail;
