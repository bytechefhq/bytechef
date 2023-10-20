import React, {useEffect, useState} from 'react';
import {useLoaderData} from 'react-router-dom';
import Button from 'components/Button/Button';
import {
    RocketIcon,
    BookmarkFilledIcon,
    DashboardIcon,
    MagicWandIcon,
    PlusIcon,
} from '@radix-ui/react-icons';
import LeftSidebar from './LeftSidebar';
import RightSlideOver from './RightSlideOver';
import SidebarContentLayout from '../../components/Layouts/SidebarContentLayout';
import ToggleGroup, {
    ToggleItem,
} from '../../components/ToggleGroup/ToggleGroup';
import Select from '../../components/Select/Select';
import {WorkflowModel} from '../../data-access/workflow';
import {
    ArrowLeftOnRectangleIcon,
    ArrowRightOnRectangleIcon,
} from '@heroicons/react/24/solid';
import WorkflowEditor from './WorkflowEditor';
import {
    useGetComponentsQuery,
    useGetFlowControlsQuery,
    useGetIntegrationWorkflowsQuery,
} from 'queries/integration.queries';

interface IntegrationDataType {
    category: string;
    createdBy: string;
    createdDate: string;
    id: number;
    name: string;
    description: string;
    lastModifiedBy: string;
    lastModifiedDate: string;
    tags: string[];
    workflowIds: string[];
}

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

const sidebarToggleItems: ToggleItem[] = [
    {
        value: 'components',
        label: 'Components',
    },
    {
        value: 'flow-controls',
        label: 'Flow Controls',
    },
];

const Integration: React.FC = () => {
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowModel>({});
    const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
    const [leftSidebarView, setLeftSidebarView] = useState('components');
    const [rightSlideOverOpen, setRightSlideOverOpen] = useState(false);
    const [view, setView] = useState('designer');

    const currentIntegration = useLoaderData() as IntegrationDataType;

    const {
        data: components,
        isLoading: componentsLoading,
        error: componentsError,
    } = useGetComponentsQuery();

    const {
        data: flowControls,
        isLoading: flowControlsLoading,
        error: flowControlsError,
    } = useGetFlowControlsQuery();

    const {
        data: integrationWorkflows,
        isLoading: integrationWorkflowsLoading,
        error: integrationWorkflowsError,
    } = useGetIntegrationWorkflowsQuery({id: currentIntegration.id});

    useEffect(() => {
        if (!integrationWorkflowsLoading && !integrationWorkflowsError) {
            setCurrentWorkflow(integrationWorkflows[0]);
        }
    }, [
        integrationWorkflows,
        integrationWorkflowsError,
        integrationWorkflowsLoading,
        currentIntegration,
    ]);

    return (
        <>
            {!componentsLoading &&
            !flowControlsLoading &&
            !integrationWorkflowsLoading &&
            !componentsError &&
            !flowControlsError &&
            !integrationWorkflowsError ? (
                <SidebarContentLayout
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
                                {currentIntegration.name}
                            </h1>

                            <div className="flex py-4 px-2">
                                <div className="flex rounded-md bg-white">
                                    {currentWorkflow && (
                                        <Select
                                            defaultValue={
                                                integrationWorkflows[0].id
                                            }
                                            selectItems={integrationWorkflows.map(
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
                                        />
                                    </div>
                                </div>
                            </div>

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
                        <ToggleGroup
                            defaultValue="components"
                            toggleItems={sidebarToggleItems}
                            onValueChange={(value) => setLeftSidebarView(value)}
                        />
                    }
                    leftSidebarBody={
                        <LeftSidebar
                            view={leftSidebarView}
                            data={{components, flowControls}}
                        />
                    }
                    leftSidebarOpen={leftSidebarOpen}
                    rightToolbarBody={
                        <div className="flex flex-col items-center divide-y-8 py-4">
                            <Button
                                displayType="icon"
                                onClick={() => setRightSlideOverOpen(true)}
                            >
                                <RocketIcon className="h-6 w-6 " />
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
                        <WorkflowEditor data={{components, flowControls}} />

                        {rightSlideOverOpen && (
                            <RightSlideOver
                                open={rightSlideOverOpen}
                                closeSidebar={() =>
                                    setRightSlideOverOpen(false)
                                }
                            />
                        )}
                    </>
                </SidebarContentLayout>
            ) : (
                <h1>Loading current view: {view}</h1>
            )}
        </>
    );
};

export default Integration;
