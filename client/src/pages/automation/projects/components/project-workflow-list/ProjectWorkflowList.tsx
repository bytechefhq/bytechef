import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Skeleton} from '@/components/ui/skeleton';
import {useToast} from '@/hooks/use-toast';
import ProjectWorkflowListItem from '@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowListItem';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Project} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {LayoutTemplateIcon, PlusIcon, UploadIcon, WorkflowIcon} from 'lucide-react';
import {ChangeEvent, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';

const ProjectWorkflowList = ({project}: {project: Project}) => {
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const {captureProjectWorkflowCreated, captureProjectWorkflowImported} = useAnalytics();
    const navigate = useNavigate();
    const {toast} = useToast();

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const ff_1041 = useFeatureFlagsStore()('ff-1041');

    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {data: taskDispatcherDefinitions, isLoading: isTaskDispatcherDefinitionsLoading} =
        useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const {data: workflows, isLoading: isProjectWorkflowsLoading} = useGetProjectWorkflowsQuery(
        project.id!,
        !!project.id
    );

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (projectWorkflowId) => {
            captureProjectWorkflowCreated();

            navigate(`/automation/projects/${project.id}/project-workflows/${projectWorkflowId}`);
        },
    });

    const importProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: () => {
            captureProjectWorkflowImported();

            queryClient.invalidateQueries({queryKey: ProjectKeys.project(project.id!)});
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast({
                description: 'Workflow is imported.',
            });
        },
    });

    const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            const file = event.target.files[0];

            /* eslint-disable @typescript-eslint/no-explicit-any */
            const definition = await (typeof (file as any).text === 'function'
                ? (file as Blob).text()
                : new Response(file).text());

            importProjectWorkflowMutation.mutate({
                id: project.id!,
                workflow: {
                    definition,
                },
            });
        }
    };

    return isComponentDefinitionsLoading || isTaskDispatcherDefinitionsLoading || isProjectWorkflowsLoading ? (
        <div className="space-y-3 py-2">
            <Skeleton className="h-5 w-40" />

            {[1, 2].map((value) => (
                <div className="flex items-center space-x-4" key={value}>
                    <Skeleton className="h-4 w-80" />

                    <div className="flex w-60 items-center space-x-1">
                        <Skeleton className="h-6 w-7 rounded-full" />

                        <Skeleton className="size-7 rounded-full" />

                        <Skeleton className="size-7 rounded-full" />
                    </div>

                    <Skeleton className="h-4 flex-1" />
                </div>
            ))}
        </div>
    ) : (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            {workflows && workflows.length > 0 ? (
                <>
                    <h3 className="heading-tertiary flex justify-start pl-2 text-sm">Workflows</h3>

                    <ul className="divide-y divide-gray-100">
                        {workflows
                            .sort((a, b) => a.label!.localeCompare(b.label!))
                            .map((workflow) => {
                                const componentNames = [
                                    ...(workflow.workflowTriggerComponentNames ?? []),
                                    ...(workflow.workflowTaskComponentNames ?? []),
                                ];

                                componentNames?.map((componentName) => {
                                    if (!workflowComponentDefinitions[componentName]) {
                                        workflowComponentDefinitions[componentName] = componentDefinitions?.find(
                                            (componentDefinition) => componentDefinition.name === componentName
                                        );
                                    }

                                    if (!workflowTaskDispatcherDefinitions[componentName]) {
                                        workflowTaskDispatcherDefinitions[componentName] =
                                            taskDispatcherDefinitions?.find(
                                                (taskDispatcherDefinition) =>
                                                    taskDispatcherDefinition.name === componentName
                                            );
                                    }
                                });

                                const filteredComponentNames = componentNames?.filter(
                                    (item, index) => componentNames?.indexOf(item) === index
                                );

                                return (
                                    <ProjectWorkflowListItem
                                        filteredComponentNames={filteredComponentNames}
                                        key={workflow.id}
                                        project={project}
                                        workflow={workflow}
                                        workflowComponentDefinitions={workflowComponentDefinitions}
                                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                                    />
                                );
                            })}
                    </ul>
                </>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={
                            <WorkflowDialog
                                createWorkflowMutation={createProjectWorkflowMutation}
                                projectId={project.id}
                                triggerNode={
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button>Create Workflow</Button>
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem onClick={() => setShowWorkflowDialog(true)}>
                                                <PlusIcon /> From Scratch
                                            </DropdownMenuItem>

                                            {ff_1041 && (
                                                <DropdownMenuItem onClick={() => navigate(`./${project.id}/templates`)}>
                                                    <LayoutTemplateIcon /> From Template
                                                </DropdownMenuItem>
                                            )}

                                            <DropdownMenuItem
                                                onClick={() => {
                                                    if (hiddenFileInputRef.current) {
                                                        hiddenFileInputRef.current.click();
                                                    }
                                                }}
                                            >
                                                <UploadIcon /> Import Workflow
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                }
                                useGetWorkflowQuery={useGetWorkflowQuery}
                            />
                        }
                        icon={<WorkflowIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new workflow."
                        title="No Workflows"
                    />
                </div>
            )}

            {showWorkflowDialog && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    projectId={project.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input
                accept=".json,.yaml,.yml"
                alt="file"
                className="hidden"
                onChange={handleFileChange}
                ref={hiddenFileInputRef}
                type="file"
            />
        </div>
    );
};

export default ProjectWorkflowList;
