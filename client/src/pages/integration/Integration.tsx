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
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowModel>();
    const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
    const [leftSidebarView, setLeftSidebarView] = useState('components');
    const [rightSlideOverOpen, setRightSlideOverOpen] = useState(false);
    const [view, setView] = useState('designer');
    const [workflows, setWorkflows] = useState<WorkflowModel[]>([]);

    const currentIntegration = useLoaderData() as IntegrationDataType;

    useEffect(() => {
        fetch(`http://localhost:5173/api/workflows`)
            .then((response) => response.json())
            .then((workflows) => {
                setWorkflows(workflows);

                setCurrentWorkflow(
                    workflows.find(
                        (workflow: WorkflowModel) =>
                            workflow.id === currentIntegration.workflowIds[0]
                    )
                );
            });
    }, [currentIntegration.workflowIds]);

    return (
        <SidebarContentLayout
            className="border-l border-gray-100 bg-gray-50"
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

                    <h1 className="mr-6 py-4 pr-4">
                        {currentIntegration.name}
                    </h1>

                    <div className="flex py-4 px-2">
                        <div className="flex rounded-md bg-white">
                            {currentWorkflow && (
                                <Select
                                    defaultValue={currentWorkflow.id}
                                    selectItems={workflows.map(
                                        (workflow: WorkflowModel) => ({
                                            label: workflow.label!,
                                            value: workflow.id!,
                                        })
                                    )}
                                    onValueChange={(value) =>
                                        setCurrentWorkflow(
                                            workflows.find(
                                                (workflow: WorkflowModel) =>
                                                    workflow.id === value
                                            )
                                        )
                                    }
                                />
                            )}

                            <div className="flex border-l border-gray-100 align-middle">
                                <Button
                                    displayType="light"
                                    icon={<PlusIcon className="h-5 w-5" />}
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
            leftSidebarBody={<LeftSidebar view={leftSidebarView} />}
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
                <div className="px-4">
                    <div className="mr-auto space-y-2">
                        <h1>{`Current view: ${view}`}</h1>

                        <h2>{`Current workflow: ${currentWorkflow?.label}`}</h2>

                        <div className="w-1/3">
                            <p>
                                Current Integration:{' '}
                                {JSON.stringify(currentIntegration)}
                            </p>
                        </div>
                    </div>
                </div>

                {rightSlideOverOpen && (
                    <RightSlideOver
                        open={rightSlideOverOpen}
                        closeSidebar={() => setRightSlideOverOpen(false)}
                    />
                )}
            </>
        </SidebarContentLayout>
    );
};

export default Integration;
