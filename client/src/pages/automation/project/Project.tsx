import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {RightSidebar} from '@/layouts/RightSidebar';
import {ProjectModel, WorkflowModel} from '@/middleware/helios/configuration';
import {useCreateProjectWorkflowRequestMutation} from '@/mutations/projects.mutations';
import {useNodeDetailsDialogStore} from '@/pages/automation/project/stores/useNodeDetailsDialogStore';
import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {
    ProjectKeys,
    useGetProjectQuery,
    useGetProjectWorkflowsQuery,
} from '@/queries/projects.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {
    ArrowLeftOnRectangleIcon,
    ArrowRightOnRectangleIcon,
} from '@heroicons/react/24/solid';
import {useQueryClient} from '@tanstack/react-query';
import {Code2, Play, Puzzle} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useLoaderData, useNavigate, useParams} from 'react-router-dom';

import PageLoader from '../../../components/PageLoader/PageLoader';
import WorkflowDialog from '../../../components/WorkflowDialog/WorkflowDialog';
import LayoutContainer from '../../../layouts/LayoutContainer';
import WorkflowEditor from './Workflow';
import ComponentSidebar from './components/ComponentSidebar';
import useLeftSidebarStore from './stores/useLeftSidebarStore';

const Project = () => {
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowModel>({});
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [filter, setFilter] = useState('');

    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();
    const {leftSidebarOpen, setLeftSidebarOpen} = useLeftSidebarStore();
    const {setNodeDetailsDialogOpen} = useNodeDetailsDialogStore();

    const {projectId, workflowId} = useParams();
    const navigate = useNavigate();

    const rightSidebarNavigation: {
        name: string;
        icon: React.ForwardRefExoticComponent<
            Omit<React.SVGProps<SVGSVGElement>, 'ref'>
        >;
        onClick?: () => void;
    }[] = [
        {
            icon: Puzzle,
            name: 'Components & Control Flows',
            onClick: () => {
                setNodeDetailsDialogOpen(false);
                setRightSidebarOpen(!rightSidebarOpen);
            },
        },
        {
            icon: Code2,
            name: 'Workflow Code Editor',
        },
    ];

    const queryClient = useQueryClient();

    const {data: project} = useGetProjectQuery(
        parseInt(projectId!),
        useLoaderData() as ProjectModel
    );

    const {
        data: components,
        error: componentsError,
        isLoading: componentsLoading,
    } = useGetComponentDefinitionsQuery({actionDefinitions: true});

    const {
        data: flowControls,
        error: flowControlsError,
        isLoading: flowControlsLoading,
    } = useGetTaskDispatcherDefinitionsQuery();

    const {
        data: projectWorkflows,
        error: projectWorkflowsError,
        isLoading: projectWorkflowsLoading,
    } = useGetProjectWorkflowsQuery(project?.id as number);

    const createProjectWorkflowRequestMutation =
        useCreateProjectWorkflowRequestMutation({
            onSuccess: (workflow) => {
                queryClient.invalidateQueries(
                    ProjectKeys.projectWorkflows(parseInt(projectId!))
                );

                setCurrentWorkflow(workflow);
            },
        });

    useEffect(() => {
        if (currentWorkflow.id) {
            navigate(
                `/automation/projects/${projectId}/workflow/${currentWorkflow.id}`
            );
        }

        setNodeDetailsDialogOpen(false);
    }, [currentWorkflow, navigate, projectId, setNodeDetailsDialogOpen]);

    return (
        <PageLoader
            errors={[componentsError, flowControlsError, projectWorkflowsError]}
            loading={
                componentsLoading ||
                flowControlsLoading ||
                projectWorkflowsLoading
            }
        >
            <LayoutContainer
                className="bg-muted dark:bg-background"
                header={
                    <header className="flex items-center">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="ml-1"
                                    onClick={() =>
                                        setLeftSidebarOpen(!leftSidebarOpen)
                                    }
                                >
                                    {leftSidebarOpen ? (
                                        <ArrowLeftOnRectangleIcon className="h-6 w-6" />
                                    ) : (
                                        <ArrowRightOnRectangleIcon className="h-6 w-6" />
                                    )}
                                </Button>
                            </TooltipTrigger>

                            <TooltipContent>
                                {leftSidebarOpen
                                    ? 'Hide Debug Panel'
                                    : 'Show Debug Panel'}
                            </TooltipContent>
                        </Tooltip>

                        <h1 className="mr-6 py-4 pr-4">{project?.name}</h1>

                        <div className="mx-2 my-4 flex rounded-md">
                            {currentWorkflow && !!projectWorkflows && (
                                <Select
                                    defaultValue={workflowId}
                                    name="projectWorkflowSelect"
                                    onValueChange={(value) => {
                                        setCurrentWorkflow(
                                            projectWorkflows.find(
                                                (workflow: WorkflowModel) =>
                                                    workflow.id === value
                                            )!
                                        );

                                        navigate(
                                            `/automation/projects/${projectId}/workflow/${value}`
                                        );
                                    }}
                                    value={currentWorkflow.id || workflowId}
                                >
                                    <SelectTrigger className="mr-0.5 bg-white">
                                        <SelectValue placeholder="Select a workflow" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectGroup>
                                            <SelectLabel>Workflows</SelectLabel>

                                            {projectWorkflows.map(
                                                (workflow) => (
                                                    <SelectItem
                                                        key={workflow.id!}
                                                        value={workflow.id!}
                                                    >
                                                        {workflow.label!}
                                                    </SelectItem>
                                                )
                                            )}
                                        </SelectGroup>
                                    </SelectContent>
                                </Select>
                            )}

                            {!!projectId && (
                                <WorkflowDialog
                                    createWorkflowRequestMutation={
                                        createProjectWorkflowRequestMutation
                                    }
                                    parentId={+projectId}
                                />
                            )}
                        </div>

                        <div className="flex flex-1 justify-end">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        variant="outline"
                                        className="mr-1 border-green-500 text-green-500 hover:border-green-300 hover:bg-green-300"
                                        onClick={() => setLeftSidebarOpen(true)}
                                    >
                                        <Play className="h-5" /> Test
                                    </Button>
                                </TooltipTrigger>

                                <TooltipContent>
                                    Test current workflow
                                </TooltipContent>
                            </Tooltip>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button className="mr-4">Publish</Button>
                                </TooltipTrigger>

                                <TooltipContent>
                                    Publish current workflow
                                </TooltipContent>
                            </Tooltip>
                        </div>
                    </header>
                }
                leftSidebarHeader={<></>}
                leftSidebarBody={<></>}
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="96"
                rightSidebarBody={
                    <>
                        {components && !!flowControls && (
                            <ComponentSidebar
                                data={{components, flowControls}}
                                filter={filter}
                            />
                        )}
                    </>
                }
                rightSidebarHeader={
                    <div className="px-3 py-4">
                        <Input
                            className="mb-0 px-3 py-4"
                            name="workflowElementsFilter"
                            onChange={(event) => setFilter(event.target.value)}
                            placeholder="Filter workflow nodes"
                            value={filter}
                        />
                    </div>
                }
                rightSidebarOpen={rightSidebarOpen}
                rightSidebarWidth="96"
                rightToolbarBody={
                    <>
                        <RightSidebar navigation={rightSidebarNavigation} />
                    </>
                }
                rightToolbarOpen={true}
            >
                {components && !!flowControls && (
                    <WorkflowEditor
                        components={components}
                        flowControls={flowControls}
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default Project;
