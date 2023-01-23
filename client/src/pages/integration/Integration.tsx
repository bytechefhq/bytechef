import React, {useEffect, useState} from 'react';
import {useLoaderData} from 'react-router-dom';
import Button from 'components/Button/Button';
import {
    RocketIcon,
    BookmarkFilledIcon,
    DashboardIcon,
    MagicWandIcon,
    PlusIcon,
    PlayIcon,
    PauseIcon,
    TextAlignLeftIcon,
    Cross1Icon,
} from '@radix-ui/react-icons';
import LeftSidebar from './LeftSidebar';
import cx from 'classnames';
import RightSidebar from './RightSidebar';

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

export interface WorkflowType {
    createdBy: string;
    createdDate: string;
    format: string;
    id: string;
    inputs: object[];
    label: string;
    lastModifiedBy: string;
    lastModifiedDate: string;
    outputs: object[];
    sourceType: string;
    retry: number;
    tasks: object[];
}

const Integration: React.FC = () => {
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowType>();
    const [enabled, setEnabled] = useState(false);
    const [leftSidebarOpen, setLeftSidebarOpen] = useState(false);
    const [rightSidebarOpen, setRightSidebarOpen] = useState(false);
    const [view, setView] = useState('designer');
    const [workflows, setWorkflows] = useState([]);

    const currentIntegration = useLoaderData() as IntegrationDataType;

    useEffect(() => {
        fetch(`http://localhost:5173/api/workflows`)
            .then((response) => response.json())
            .then((workflows) => {
                setWorkflows(workflows);

                setCurrentWorkflow(
                    workflows.find(
                        (workflow: WorkflowType) =>
                            workflow.id === currentIntegration.workflowIds[0]
                    )
                );
            });
    }, [currentIntegration.workflowIds]);

    return (
        <div className="flex h-screen">
            {leftSidebarOpen && <LeftSidebar />}

            <div className="flex w-full flex-col">
                <header className="flex bg-gray-100 dark:bg-gray-800">
                    <Button
                        className="p-4"
                        icon={
                            leftSidebarOpen ? (
                                <Cross1Icon className="h-6 w-6" />
                            ) : (
                                <TextAlignLeftIcon className="h-8 w-8" />
                            )
                        }
                        onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
                        displayType="icon"
                    />

                    <h1 className="mr-10 p-4">{currentIntegration.name}</h1>

                    <div className="flex p-4">
                        <label
                            htmlFor="workflow"
                            className="flex items-center p-2 text-sm font-medium text-gray-700"
                        >
                            Workflow
                        </label>

                        <div className="flex">
                            <select
                                id="workflow"
                                name="workflow"
                                className="mt-1 mr-1 block w-full rounded-md border-gray-300 py-2 pl-3 pr-10 text-base focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
                                defaultValue="Canada"
                                onChange={(event) =>
                                    setCurrentWorkflow(
                                        workflows.find(
                                            (workflow: WorkflowType) =>
                                                workflow.id ===
                                                event.target.value
                                        )
                                    )
                                }
                            >
                                {workflows.map((workflow: WorkflowType) => (
                                    <option
                                        key={workflow.id}
                                        value={workflow.id}
                                    >
                                        {workflow.label}
                                    </option>
                                ))}
                            </select>

                            <Button
                                displayType="icon"
                                icon={<PlusIcon />}
                                size="small"
                            />
                        </div>
                    </div>

                    <div className="flex p-4">
                        <Button
                            className={cx(
                                '!rounded-r-none !shadow-none',
                                view === 'designer' && '!bg-gray-700'
                            )}
                            label="Designer"
                            onClick={() => setView('designer')}
                        />

                        <Button
                            className={cx(
                                '!rounded-l-none !shadow-none',
                                view === 'editor' && '!bg-gray-700'
                            )}
                            label="Editor"
                            onClick={() => setView('editor')}
                        />
                    </div>

                    <div className="ml-auto p-4">
                        <Button
                            className={cx(
                                '!bg-green-500 transition-all hover:!bg-rose-500',
                                enabled && '!bg-rose-500 hover:!bg-green-500'
                            )}
                            icon={enabled ? <PauseIcon /> : <PlayIcon />}
                            iconPosition="left"
                            label={enabled ? 'Disable' : 'Enable'}
                            onClick={() => setEnabled(!enabled)}
                        />
                    </div>
                </header>

                <div className="flex h-full">
                    <div className="mr-auto space-y-2 p-4">
                        <h1>{`Current view: ${view}`}</h1>

                        <h2>{`Current workflow: ${currentWorkflow?.label}`}</h2>

                        <div className="w-1/3">
                            <p>
                                Current Integration:{' '}
                                {JSON.stringify(currentIntegration)}
                            </p>
                        </div>
                    </div>

                    {rightSidebarOpen && (
                        <RightSidebar
                            closeSidebar={() => setRightSidebarOpen(false)}
                        />
                    )}

                    <div className="flex flex-col items-center bg-gray-100 dark:bg-gray-800">
                        <Button
                            displayType="icon"
                            onClick={() => setRightSidebarOpen(true)}
                            className="p-4"
                        >
                            <RocketIcon className="h-6 w-6 " />
                        </Button>

                        <Button
                            displayType="icon"
                            onClick={() => setRightSidebarOpen(true)}
                            className="p-4"
                        >
                            <BookmarkFilledIcon className="h-6 w-6" />
                        </Button>

                        <Button
                            displayType="icon"
                            onClick={() => setRightSidebarOpen(true)}
                            className="p-4"
                        >
                            <DashboardIcon className="h-6 w-6" />
                        </Button>

                        <Button
                            displayType="icon"
                            onClick={() => setRightSidebarOpen(true)}
                            className="p-4"
                        >
                            <MagicWandIcon className="h-6 w-6" />
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Integration;
