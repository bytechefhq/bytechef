import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import {useCodeWorkflowSourceQuery, useUpdateCodeWorkflowSourceMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface ProjectCodeWorkflowDetailProps {
    headerless?: boolean;
    invalidateWorkflowQueries?: () => void;
    language: string;
    onTestConfigurationClick?: () => void;
    projectId: string;
    testConfigurationDisabled?: boolean;
}

const ProjectCodeWorkflowDetail = ({
    headerless,
    invalidateWorkflowQueries,
    language,
    onTestConfigurationClick,
    projectId,
    testConfigurationDisabled,
}: ProjectCodeWorkflowDetailProps) => {
    const queryClient = useQueryClient();

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useCodeWorkflowSourceQuery({projectId}, {enabled: !!projectId});

    const updateCodeWorkflowSourceMutation = useUpdateCodeWorkflowSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['codeWorkflowSource', {projectId}]});

            invalidateWorkflowQueries?.();
        },
    });

    return (
        <CodeWorkflowSourceEditor
            error={sourceError}
            headerless={headerless}
            isLoading={sourceLoading}
            isSaving={updateCodeWorkflowSourceMutation.isPending}
            language={language}
            onSave={(content) => updateCodeWorkflowSourceMutation.mutateAsync({content, projectId})}
            onTestConfigurationClick={onTestConfigurationClick}
            source={sourceData?.codeWorkflowSource ?? undefined}
            testConfigurationDisabled={testConfigurationDisabled}
        />
    );
};

export default ProjectCodeWorkflowDetail;
