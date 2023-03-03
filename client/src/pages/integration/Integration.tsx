import React, {useEffect, useState} from 'react';
import {useLoaderData, useParams} from 'react-router-dom';
import Button from 'components/Button/Button';
import {
    RocketIcon,
    BookmarkFilledIcon,
    DashboardIcon,
    MagicWandIcon,
    PlusIcon,
} from '@radix-ui/react-icons';
import LeftSidebar from './components/LeftSidebar';
import RightSlideOver from './components/RightSlideOver';
import LayoutContainer from '../../layouts/LayoutContainer/LayoutContainer';
import ToggleGroup, {
    ToggleItem,
} from '../../components/ToggleGroup/ToggleGroup';
import Select from '../../components/Select/Select';
import {WorkflowModel} from '../../middleware/workflow';
import {
    ArrowLeftOnRectangleIcon,
    ArrowRightOnRectangleIcon,
} from '@heroicons/react/24/solid';
import WorkflowEditor from './WorkflowEditor';
import Input from 'components/Input/Input';
import {IntegrationModel} from '../../middleware/integration';
import useRightSlideOverStore from './stores/useRightSlideOverStore';
import useLeftSidebarStore from './stores/useLeftSidebarStore';
import {useGetComponentDefinitionsQuery} from '../../queries/componentDefinitions';
import {useGetTaskDispatcherDefinitionsQuery} from '../../queries/taskDispatcherDefinitions';
import {
    useGetIntegrationQuery,
    useGetIntegrationWorkflowsQuery,
} from '../../queries/integrations';
import WorkflowDialog from 'pages/integrations/WorkflowDialog';

const headerToggleItems: ToggleItem[] = [
    {
        value: 'designer',
        label: 'Designer',
    },
    {
        value: 'editor',
        label: 'Editor',
    },
];

const Integration: React.FC = () => {
    const {integrationId} = useParams<{integrationId: string}>();
    const {rightSlideOverOpen, setRightSlideOverOpen} =
        useRightSlideOverStore();
    const {leftSidebarOpen, setLeftSidebarOpen} = useLeftSidebarStore();
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowModel>({});
    const [view, setView] = useState('designer');
    const [filter, setFilter] = useState('');

    const {data: integration} = useGetIntegrationQuery(
        parseInt(integrationId!),
        useLoaderData() as IntegrationModel
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
        data: integrationWorkflows,
        isLoading: integrationWorkflowsLoading,
        error: integrationWorkflowsError,
    } = useGetIntegrationWorkflowsQuery(integration?.id as number);

    useEffect(() => {
        if (!integrationWorkflowsLoading && !integrationWorkflowsError) {
            setCurrentWorkflow(integrationWorkflows[0]);
        }
    }, [
        integrationWorkflows,
        integrationWorkflowsError,
        integrationWorkflowsLoading,
        integration,
    ]);

    const [showWorkflowDialog, setShowWorkflowDialog] =
        useState<boolean>(false);

    return (
        <>
            {!componentsLoading &&
            !flowControlsLoading &&
            !integrationWorkflowsLoading &&
            !componentsError &&
            !flowControlsError &&
            !integrationWorkflowsError ? (
                <LayoutContainer
                    className="border-l border-gray-200 bg-gray-100"
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
                                onClick={() =>
                                    setLeftSidebarOpen(!leftSidebarOpen)
                                }
                                displayType="icon"
                            />

                            <h1 className="mr-6 py-4 pr-4">
                                {integration?.name}
                            </h1>

                            <div className="flex py-4 px-2">
                                <div className="flex rounded-md bg-white">
                                    {currentWorkflow && (
                                        <Select
                                            defaultValue={
                                                integrationWorkflows[0].id
                                            }
                                            options={integrationWorkflows.map(
                                                (workflow: WorkflowModel) => ({
                                                    label: workflow.label!,
                                                    value: workflow.id!,
                                                })
                                            )}
                                            onValueChange={(value: string) => {
                                                setCurrentWorkflow(
                                                    integrationWorkflows.find(
                                                        (
                                                            workflow: WorkflowModel
                                                        ) =>
                                                            workflow.id ===
                                                            value
                                                    )!
                                                );
                                            }}
                                        />
                                    )}

                                    <div className="flex border-l border-gray-100 align-middle">
                                        <Button
                                            displayType="light"
                                            icon={
                                                <PlusIcon className="h-5 w-5" />
                                            }
                                            size="small"
                                            onClick={() =>
                                                setShowWorkflowDialog(true)
                                            }
                                        />
                                    </div>
                                </div>
                            </div>

                            {showWorkflowDialog && (
                                <WorkflowDialog visible version={undefined} />
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
                        <>
                            <Input
                                fieldsetClassName="p-4 mb-0"
                                name="workflowElementsFilter"
                                onChange={(event) =>
                                    setFilter(event.target.value)
                                }
                                placeholder="Filter workflow nodes"
                                value={filter}
                            />
                        </>
                    }
                    leftSidebarBody={
                        <LeftSidebar
                            data={{components, flowControls}}
                            filter={filter}
                        />
                    }
                    leftSidebarOpen={leftSidebarOpen}
                    rightToolbarBody={
                        <div className="flex flex-col items-center divide-y-8 py-4">
                            <Button
                                displayType="icon"
                                onClick={() => setRightSlideOverOpen(true)}
                            >
                                <RocketIcon className="h-6 w-6" />
                            </Button>

                            <Button
                                displayType="icon"
                                onClick={() => setRightSlideOverOpen(true)}
                            >
                                <BookmarkFilledIcon className="h-6 w-6" />
                            </Button>

                            <Button
                                displayType="icon"
                                onClick={() => setRightSlideOverOpen(true)}
                            >
                                <DashboardIcon className="h-6 w-6" />
                            </Button>

                            <Button
                                displayType="icon"
                                onClick={() => setRightSlideOverOpen(true)}
                            >
                                <MagicWandIcon className="h-6 w-6" />
                            </Button>
                        </div>
                    }
                >
                    <>
                        <WorkflowEditor />

                        {rightSlideOverOpen && (
                            <RightSlideOver
                                open={rightSlideOverOpen}
                                closeSidebar={() =>
                                    setRightSlideOverOpen(false)
                                }
                            />
                        )}
                    </>
                </LayoutContainer>
            ) : (
                <h1>Loading current view: {view}</h1>
            )}
        </>
    );
};

export default Integration;
