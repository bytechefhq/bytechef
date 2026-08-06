import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {useCodeWorkflowSourceQuery, useUpdateCodeWorkflowSourceMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';

interface ProjectCodeWorkflowDetailProps {
    invalidateWorkflowQueries?: () => void;
    language: string;
    onTestConfigurationClick?: () => void;
    projectId: string;
    testConfigurationDisabled?: boolean;
}

const ProjectCodeWorkflowDetail = ({
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

            // The saved source changes the generated workflow definition (declared connections, task list),
            // which the editor chrome reads from the workflow queries — refresh them so e.g. Test
            // Configuration enables without a page reload.
            invalidateWorkflowQueries?.();
        },
    });

    useEffect(() => {
        // A Copilot BUILD turn can rewrite the source server-side; refresh the editor after each turn so
        // Monaco does not keep showing stale text.
        return useCopilotPostTurnRegistry.getState().register(Source.CODE_WORKFLOW, () => {
            queryClient.invalidateQueries({queryKey: ['codeWorkflowSource', {projectId}]});
        });
    }, [projectId, queryClient]);

    return (
        <CodeWorkflowSourceEditor
            error={sourceError}
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
