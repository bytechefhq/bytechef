import Button from '@/components/Button/Button';
import isEditableCodeLanguage from '@/ee/pages/embedded/automation-workflows/utils/isEditableCodeLanguage';
import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import useCodeWorkflowHeaderStore from '@/pages/platform/code-workflow/stores/useCodeWorkflowHeaderStore';
import {
    CodeWorkflowLanguage,
    useAutomationWorkflowProjectCodeWorkflowSourceQuery,
    useAutomationWorkflowProjectsQuery,
    usePublishAutomationWorkflowProjectMutation,
    useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon} from 'lucide-react';
import {Link, Navigate, useParams} from 'react-router-dom';
import {toast} from 'sonner';

const AutomationWorkflowProjectCodeWorkflow = () => {
    const {automationWorkflowProjectId} = useParams();

    const dirty = useCodeWorkflowHeaderStore((state) => state.dirty);
    const onSaveClick = useCodeWorkflowHeaderStore((state) => state.onSaveClick);
    const saving = useCodeWorkflowHeaderStore((state) => state.saving);

    const queryClient = useQueryClient();

    const {data: projectsData} = useAutomationWorkflowProjectsQuery();

    const project = projectsData?.automationWorkflowProjects.find(
        (automationWorkflowProject) => automationWorkflowProject.id === automationWorkflowProjectId
    );

    const editableCodeProject = !!project?.codeWorkflowProject && isEditableCodeLanguage(project.codeWorkflowLanguage);

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useAutomationWorkflowProjectCodeWorkflowSourceQuery(
        {automationWorkflowProjectId: automationWorkflowProjectId!},
        {enabled: !!automationWorkflowProjectId && editableCodeProject}
    );

    const updateSourceMutation = useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectCodeWorkflowSource']});
            queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
        },
    });

    const publishMutation = usePublishAutomationWorkflowProjectMutation();

    const source = sourceData?.automationWorkflowProjectCodeWorkflowSource;

    const handlePublish = () =>
        publishMutation.mutate(
            {id: automationWorkflowProjectId!},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                    toast('Published. The change is rolling out to connected users.');
                },
            }
        );

    if (projectsData && !editableCodeProject) {
        return <Navigate replace to="/embedded/automation-workflows" />;
    }

    return (
        <div className="flex size-full flex-col">
            <header className="flex items-center justify-between border-b px-4 py-2">
                <div className="flex items-center gap-2">
                    <Link aria-label="Back to Automation Workflows" to="/embedded/automation-workflows">
                        <ArrowLeftIcon className="size-4" />
                    </Link>

                    <span className="font-semibold">{project?.name}</span>
                </div>

                <div className="flex items-center gap-2">
                    <Button
                        disabled={!dirty || saving}
                        label={saving ? 'Saving...' : 'Save'}
                        onClick={() => onSaveClick?.()}
                        size="sm"
                        variant="outline"
                    />

                    <Button
                        disabled={
                            dirty || saving || publishMutation.isPending || !project?.codeWorkflowDraftPublishable
                        }
                        label="Publish"
                        onClick={handlePublish}
                        size="sm"
                    />
                </div>
            </header>

            <CodeWorkflowSourceEditor
                error={sourceError}
                headerless
                isLoading={sourceLoading}
                isSaving={updateSourceMutation.isPending}
                language={source?.language ?? CodeWorkflowLanguage.Javascript}
                onSave={(content) =>
                    updateSourceMutation.mutateAsync({
                        automationWorkflowProjectId: automationWorkflowProjectId!,
                        content,
                    })
                }
                source={source?.content}
            />
        </div>
    );
};

export default AutomationWorkflowProjectCodeWorkflow;
