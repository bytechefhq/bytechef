import Button from '@/components/Button/Button';
import isEditableCodeLanguage from '@/ee/pages/embedded/automations/utils/isEditableCodeLanguage';
import CodeWorkflowSourceEditor from '@/pages/platform/code-workflow/CodeWorkflowSourceEditor';
import useCodeWorkflowHeaderStore from '@/pages/platform/code-workflow/stores/useCodeWorkflowHeaderStore';
import {
    CodeWorkflowLanguage,
    useAutomationProjectCodeWorkflowSourceQuery,
    useAutomationProjectsQuery,
    usePublishAutomationProjectMutation,
    useUpdateAutomationProjectCodeWorkflowSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon} from 'lucide-react';
import {Link, Navigate, useParams} from 'react-router-dom';
import {toast} from 'sonner';

const AutomationProjectCodeWorkflow = () => {
    const {automationProjectId} = useParams();

    const dirty = useCodeWorkflowHeaderStore((state) => state.dirty);
    const onSaveClick = useCodeWorkflowHeaderStore((state) => state.onSaveClick);
    const saving = useCodeWorkflowHeaderStore((state) => state.saving);

    const queryClient = useQueryClient();

    const {data: projectsData} = useAutomationProjectsQuery();

    const project = projectsData?.automationProjects.find(
        (automationProject) => automationProject.id === automationProjectId
    );

    const editableCodeProject = !!project?.codeWorkflowProject && isEditableCodeLanguage(project.codeWorkflowLanguage);

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useAutomationProjectCodeWorkflowSourceQuery(
        {automationProjectId: automationProjectId!},
        {enabled: !!automationProjectId && editableCodeProject}
    );

    const updateSourceMutation = useUpdateAutomationProjectCodeWorkflowSourceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['automationProjectCodeWorkflowSource']});
            queryClient.invalidateQueries({queryKey: ['automationProjects']});
        },
    });

    const publishMutation = usePublishAutomationProjectMutation();

    const source = sourceData?.automationProjectCodeWorkflowSource;

    const handlePublish = () =>
        publishMutation.mutate(
            {id: automationProjectId!},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationProjects']});

                    toast('Published. The change is rolling out to connected users.');
                },
            }
        );

    if (projectsData && !editableCodeProject) {
        return <Navigate replace to="/embedded/automations" />;
    }

    return (
        <div className="flex size-full flex-col">
            <header className="flex items-center justify-between border-b px-4 py-2">
                <div className="flex items-center gap-2">
                    <Link aria-label="Back to Automation Workflows" to="/embedded/automations">
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
                        automationProjectId: automationProjectId!,
                        content,
                    })
                }
                source={source?.content}
            />
        </div>
    );
};

export default AutomationProjectCodeWorkflow;
