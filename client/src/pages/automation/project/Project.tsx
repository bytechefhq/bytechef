import React, {useEffect, useState} from 'react';
import {useLoaderData, useNavigate, useParams} from 'react-router-dom';
import Button from 'components/Button/Button';
import {PlusIcon} from '@radix-ui/react-icons';
import LeftSidebar from './components/LeftSidebar';
import LayoutContainer from '../../../layouts/LayoutContainer/LayoutContainer';
import ToggleGroup, {
    IToggleItem,
} from '../../../components/ToggleGroup/ToggleGroup';
import Select from '../../../components/Select/Select';
import {WorkflowModel} from '../../../middleware/workflow';
import {
    ArrowLeftOnRectangleIcon,
    ArrowRightOnRectangleIcon,
} from '@heroicons/react/24/solid';
import WorkflowEditor from './WorkflowEditor';
import Input from 'components/Input/Input';
import {ProjectModel} from '../../../middleware/project';
import useLeftSidebarStore from './stores/useLeftSidebarStore';
import {useGetComponentDefinitionsQuery} from '../../../queries/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '../../../queries/taskDispatcherDefinitions.queries';
import {
    ProjectKeys,
    useGetProjectQuery,
    useGetProjectWorkflowsQuery,
} from '../../../queries/projects.queries';
import WorkflowDialog from '../../../components/WorkflowDialog/WorkflowDialog';
import {useCreateProjectWorkflowRequestMutation} from '../../../mutations/projects.mutations';
import {useQueryClient} from '@tanstack/react-query';
import getCreatedWorkflow from 'utils/getCreatedWorkflow';

const headerToggleItems: IToggleItem[] = [
    {
        value: 'designer',
        label: 'Designer',
    },
    {
        value: 'editor',
        label: 'Editor',
    },
];

const Project: React.FC = () => {
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowModel>({});
    const [view, setView] = useState('designer');
    const [filter, setFilter] = useState('');
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const {leftSidebarOpen, setLeftSidebarOpen} = useLeftSidebarStore();

    const {projectId, workflowId} = useParams();
    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const {data: project} = useGetProjectQuery(
        parseInt(projectId!),
        useLoaderData() as ProjectModel
    );

    const {
        data: components,
        isLoading: componentsLoading,
        error: componentsError,
    } = useGetComponentDefinitionsQuery();

    const {
        data: flowControls,
        isLoading: flowControlsLoading,
        error: flowControlsError,
    } = useGetTaskDispatcherDefinitionsQuery();

    const {
        data: projectWorkflows,
        isLoading: projectWorkflowsLoading,
        error: projectWorkflowsError,
    } = useGetProjectWorkflowsQuery(project?.id as number);

    const createWorkflowMutation = useCreateProjectWorkflowRequestMutation({
        onSuccess: (project) => {
            queryClient.invalidateQueries(
                ProjectKeys.projectWorkflows(parseInt(projectId!))
            );

            if (projectWorkflows) {
                const createdWorkflow = getCreatedWorkflow(
                    projectWorkflows,
                    project
                );

                if (createdWorkflow) {
                    setCurrentWorkflow(createdWorkflow);
                }
            }

            setShowWorkflowDialog(false);
        },
    });

    useEffect(() => {
        if (currentWorkflow.id) {
            navigate(
                `/automation/projects/${projectId}/workflow/${currentWorkflow.id}`
            );
        }
    }, [currentWorkflow, navigate, projectId]);

    if (
        !componentsLoading &&
        !flowControlsLoading &&
        !projectWorkflowsLoading &&
        !componentsError &&
        !flowControlsError &&
        !projectWorkflowsError
    ) {
        return (
            <LayoutContainer
                bodyClassName="border-l border-gray-200 bg-gray-100"
                header={
                    <header className="flex items-center">
                        <Button
                            className="p-4"
                            icon={
                                leftSidebarOpen ? (
                                    <ArrowLeftOnRectangleIcon className="h-6 w-6" />
                                ) : (
                                    <ArrowRightOnRectangleIcon className="h-6 w-6" />
                                )
                            }
                            onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                            displayType="icon"
                        />

                        <h1 className="mr-6 py-4 pr-4">{project?.name}</h1>

                        <div className="my-4 mx-2 flex rounded-md bg-white">
                            {currentWorkflow && (
                                <Select
                                    defaultValue={workflowId}
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
                                    options={projectWorkflows.map(
                                        (workflow) => ({
                                            label: workflow.label!,
                                            value: workflow.id!,
                                        })
                                    )}
                                    value={currentWorkflow.id!}
                                />
                            )}

                            <div className="flex border-l border-gray-100 align-middle">
                                <Button
                                    displayType="light"
                                    icon={<PlusIcon className="h-5 w-5" />}
                                    size="small"
                                    onClick={() => setShowWorkflowDialog(true)}
                                />
                            </div>
                        </div>

                        {showWorkflowDialog && !!projectId && (
                            <WorkflowDialog
                                createWorkflowRequestMutation={
                                    createWorkflowMutation
                                }
                                id={projectId}
                                visible
                            />
                        )}

                        <div>
                            <ToggleGroup
                                defaultValue="designer"
                                toggleItems={headerToggleItems}
                                onValueChange={(value) => setView(value)}
                            />
                        </div>
                    </header>
                }
                leftSidebarHeader={
                    <Input
                        fieldsetClassName="p-4 mb-0"
                        name="workflowElementsFilter"
                        onChange={(event) => setFilter(event.target.value)}
                        placeholder="Filter workflow nodes"
                        value={filter}
                    />
                }
                leftSidebarBody={
                    <LeftSidebar
                        data={{components, flowControls}}
                        filter={filter}
                    />
                }
                leftSidebarOpen={leftSidebarOpen}
            >
                <WorkflowEditor />
            </LayoutContainer>
        );
    } else {
        return <h1>Loading current view: {view}</h1>;
    }
};

export default Project;
