import React, {useState} from 'react';
import Button from 'components/Button/Button';
import {
    RocketIcon,
    BookmarkFilledIcon,
    DashboardIcon,
    MagicWandIcon,
    PlusIcon,
    PlayIcon,
    PauseIcon,
} from '@radix-ui/react-icons';
import Sidebar from './Sidebar';
import cx from 'classnames';

const Integration: React.FC = () => {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [view, setView] = useState('designer');
    const [enabled, setEnabled] = useState(false);

    return (
        <div className="h-screen">
            <header className="flex bg-gray-100 dark:bg-gray-800">
                <h1 className="mr-10 p-4">Integration Name</h1>

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
                        >
                            <option>Pipedream</option>

                            <option>Dreampipe</option>

                            <option>Acmeme</option>
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
                <div className="mr-auto p-4">{`Current view: ${view}`}</div>

                {sidebarOpen && (
                    <Sidebar closeSidebar={() => setSidebarOpen(false)} />
                )}

                <div className="flex flex-col items-center bg-gray-100 dark:bg-gray-800">
                    <Button
                        displayType="icon"
                        onClick={() => setSidebarOpen(true)}
                        className="p-4"
                    >
                        <RocketIcon className="h-6 w-6 " />
                    </Button>

                    <Button
                        displayType="icon"
                        onClick={() => setSidebarOpen(true)}
                        className="p-4"
                    >
                        <BookmarkFilledIcon className="h-6 w-6" />
                    </Button>

                    <Button
                        displayType="icon"
                        onClick={() => setSidebarOpen(true)}
                        className="p-4"
                    >
                        <DashboardIcon className="h-6 w-6" />
                    </Button>

                    <Button
                        displayType="icon"
                        onClick={() => setSidebarOpen(true)}
                        className="p-4"
                    >
                        <MagicWandIcon className="h-6 w-6" />
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default Integration;
