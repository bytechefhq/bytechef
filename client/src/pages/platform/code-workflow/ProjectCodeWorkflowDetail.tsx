import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import {useCodeWorkflowSourceQuery, useUpdateCodeWorkflowSourceMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface ProjectCodeWorkflowDetailProps {
    language: string;
    projectId: string;
}

const ProjectCodeWorkflowDetail = ({language, projectId}: ProjectCodeWorkflowDetailProps) => {
    const queryClient = useQueryClient();

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useCodeWorkflowSourceQuery({projectId}, {enabled: !!projectId});

    const updateCodeWorkflowSourceMutation = useUpdateCodeWorkflowSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['codeWorkflowSource', {projectId}]});
        },
    });

    return (
        <CodeWorkflowSourceEditor
            error={sourceError}
            isLoading={sourceLoading}
            isSaving={updateCodeWorkflowSourceMutation.isPending}
            language={language}
            onSave={(content) => updateCodeWorkflowSourceMutation.mutateAsync({content, projectId})}
            source={sourceData?.codeWorkflowSource ?? undefined}
        />
    );
};

export default ProjectCodeWorkflowDetail;
