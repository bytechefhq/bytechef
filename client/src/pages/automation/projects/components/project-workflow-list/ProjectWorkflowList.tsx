import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useConvertN8nToWorkflow} from '@/pages/automation/project/hooks/useConverterN8nToWorkflow';
import handleImportN8nWorkflow from '@/pages/automation/project/utils/handleImportN8nWorkflow';
import handleImportWorkflow from '@/pages/automation/project/utils/handleImportWorkflow';
import ProjectWorkflowListItem from '@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowListItem';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useHasEnabledAiProvider} from '@/shared/hooks/useHasEnabledAiProvider';
import {Project} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, LayoutTemplateIcon, LoaderCircleIcon, UploadIcon, WorkflowIcon} from 'lucide-react';
import {useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

const ProjectWorkflowList = ({
    componentDefinitions,
    project,
    queryEnabled,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    project: Project;
    queryEnabled?: boolean;
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const {captureProjectWorkflowCreated, captureProjectWorkflowImported} = useAnalytics();
    const navigate = useNavigate();

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);
    const converterHiddenFileInputRef = useRef<HTMLInputElement>(null);

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const {data: workflows, isLoading: isProjectWorkflowsLoading} = useGetProjectWorkflowsQuery(
        project.id!,
        queryEnabled && !!project.id
    );

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const queryClient = useQueryClient();

    const {convertN8nWorkflow} = useConvertN8nToWorkflow();
    const {hasEnabledAiProvider, isPending: isAiProviderCheckPending} = useHasEnabledAiProvider();

    const importN8nWorkflowDisabled = !isAiProviderCheckPending && !hasEnabledAiProvider;
    const [isImportingN8nWorkflow, setIsImportingN8nWorkflow] = useState(false);

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (response) => {
            captureProjectWorkflowCreated();

            navigate(`/automation/projects/${project.id}/project-workflows/${response.projectWorkflowId}`);
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

            toast('Workflow is imported.');
        },
    });

    return !componentDefinitions || !taskDispatcherDefinitions || isProjectWorkflowsLoading ? (
        <div className="space-y-3 p-3">
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
        <div className="pt-3">
            {workflows && workflows.length > 0 ? (
                <>
                    <h3 className="flex justify-start pl-3 text-sm heading-tertiary">Workflows</h3>

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
                                parentId={project.id}
                                triggerNode={
                                    <ButtonGroup className="mx-auto">
                                        <Button
                                            onClick={(event) => {
                                                event.stopPropagation();

                                                setShowWorkflowDialog(true);
                                            }}
                                        >
                                            Create Workflow
                                        </Button>

                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button
                                                    icon={
                                                        isImportingN8nWorkflow ? (
                                                            <LoaderCircleIcon className="animate-spin" />
                                                        ) : (
                                                            <ChevronDownIcon />
                                                        )
                                                    }
                                                ></Button>
                                            </DropdownMenuTrigger>

                                            <DropdownMenuContent align="end" className="p-0">
                                                <DropdownMenuItem
                                                    className="dropdown-menu-item"
                                                    onClick={(event) => {
                                                        event.stopPropagation();

                                                        navigate(`./${project.id}/templates`);
                                                    }}
                                                >
                                                    <LayoutTemplateIcon /> From Template
                                                </DropdownMenuItem>

                                                <DropdownMenuItem
                                                    className="dropdown-menu-item"
                                                    onClick={(event) => {
                                                        event.stopPropagation();

                                                        if (hiddenFileInputRef.current) {
                                                            hiddenFileInputRef.current.click();
                                                        }
                                                    }}
                                                >
                                                    <UploadIcon /> Import Workflow
                                                </DropdownMenuItem>

                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <span>
                                                            <DropdownMenuItem
                                                                disabled={importN8nWorkflowDisabled}
                                                                onClick={(event) => {
                                                                    event.stopPropagation();

                                                                    if (converterHiddenFileInputRef.current) {
                                                                        converterHiddenFileInputRef.current.click();
                                                                    }
                                                                }}
                                                            >
                                                                <UploadIcon /> Import n8n Workflow
                                                            </DropdownMenuItem>
                                                        </span>
                                                    </TooltipTrigger>

                                                    {importN8nWorkflowDisabled && (
                                                        <TooltipContent>
                                                            Enable an AI provider to import n8n workflows.
                                                        </TooltipContent>
                                                    )}
                                                </Tooltip>
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </ButtonGroup>
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
                    parentId={project.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input
                accept=".json,.yaml,.yml"
                alt="file"
                className="hidden"
                onChange={(event) => handleImportWorkflow(event, project.id!, importProjectWorkflowMutation)}
                ref={hiddenFileInputRef}
                type="file"
            />

            <input
                accept=".json"
                className="hidden"
                onChange={async (event) => {
                    if (!event.target.files?.length) {
                        return;
                    }

                    try {
                        setIsImportingN8nWorkflow(true);
                        await handleImportN8nWorkflow(
                            event,
                            project.id!,
                            importProjectWorkflowMutation,
                            convertN8nWorkflow
                        );
                    } finally {
                        setIsImportingN8nWorkflow(false);

                        if (converterHiddenFileInputRef.current) {
                            converterHiddenFileInputRef.current.value = '';
                        }
                    }
                }}
                ref={converterHiddenFileInputRef}
                type="file"
            />
        </div>
    );
};

export default ProjectWorkflowList;
