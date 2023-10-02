import {ProjectModel, WorkflowModel} from '@/middleware/helios/configuration';
import {useCreateProjectWorkflowRequestMutation} from '@/mutations/projects.mutations';
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
import React, {useEffect, useState} from 'react';
import {useLoaderData, useNavigate, useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import Button from '../../../components/Button/Button';
import Input from '../../../components/Input/Input';
import PageLoader from '../../../components/PageLoader/PageLoader';
import Select from '../../../components/Select/Select';
import ToggleGroup, {
    IToggleItem,
} from '../../../components/ToggleGroup/ToggleGroup';
import WorkflowDialog from '../../../components/WorkflowDialog/WorkflowDialog';
import LayoutContainer from '../../../layouts/LayoutContainer';
import WorkflowEditor from './Workflow';
import LeftSidebar from './components/LeftSidebar';
import useLeftSidebarStore from './stores/useLeftSidebarStore';

const headerToggleItems: IToggleItem[] = [
    {
        label: 'Designer',
        value: 'designer',
    },
    {
        label: 'Editor',
        value: 'editor',
    },
];

const Project: React.FC = () => {
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowModel>({});
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [view, setView] = useState('designer');
    const [filter, setFilter] = useState('');

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

    const createWorkflowMutation = useCreateProjectWorkflowRequestMutation({
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
    }, [currentWorkflow, navigate, projectId]);

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

                        <div className="mx-2 my-4 flex rounded-md border border-gray-100 bg-white">
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
                                    options={projectWorkflows.map(
                                        (workflow) => ({
                                            label: workflow.label!,
                                            value: workflow.id!,
                                        })
                                    )}
                                    value={currentWorkflow.id || workflowId}
                                />
                            )}

                            <div className="flex border-l border-gray-100 align-middle">
                                {!!projectId && (
                                    <WorkflowDialog
                                        createWorkflowRequestMutation={
                                            createWorkflowMutation
                                        }
                                        id={projectId}
                                    />
                                )}
                            </div>
                        </div>

                        <ToggleGroup
                            containerClassName={twMerge(
                                'ml-[14%]',
                                leftSidebarOpen && 'ml-[96px]'
                            )}
                            defaultValue="designer"
                            toggleItems={headerToggleItems}
                            onValueChange={(value) => setView(value)}
                        />
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
                    <>
                        {components && !!flowControls && (
                            <LeftSidebar
                                data={{components, flowControls}}
                                filter={filter}
                            />
                        )}
                    </>
                }
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="72"
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
