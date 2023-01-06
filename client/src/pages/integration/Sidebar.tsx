import {InfoCircledIcon, Cross1Icon} from '@radix-ui/react-icons';
import React from 'react';
import {
    Arrow,
    Content,
    Provider,
    Root,
    Trigger,
    Portal,
} from '@radix-ui/react-tooltip';
import Button from 'components/Button/Button';

type SidebarProps = {
    closeSidebar: () => void;
};

const Sidebar = ({closeSidebar}: SidebarProps) => {
    return (
        <Provider>
            <div className="h-full bg-slate-200 dark:bg-gray-800">
                <header className="flex items-center space-x-4 p-4">
                    <h2>Http Client (http-client-1)</h2>

                    <Root>
                        <Trigger asChild>
                            <InfoCircledIcon className="h-4 w-4" />
                        </Trigger>

                        <Portal>
                            <Content
                                className="rounded-lg bg-white p-2 px-4 shadow-sm"
                                sideOffset={5}
                            >
                                {'Information'}

                                <Arrow className="fill-white" />
                            </Content>
                        </Portal>
                    </Root>

                    <Button
                        className="!ml-auto p-2"
                        displayType="icon"
                        onClick={closeSidebar}
                    >
                        <Cross1Icon className="h-4 w-4" />
                    </Button>
                </header>

                <div className="flex h-full flex-col space-y-4 p-4">
                    <div>
                        <label
                            htmlFor="location"
                            className="block text-sm font-medium text-gray-700"
                        >
                            Location
                        </label>

                        <select
                            id="location"
                            name="location"
                            className="mt-1 block w-full rounded-md border-gray-300 py-2 pl-3 pr-10 text-base focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
                            defaultValue="Canada"
                        >
                            <option>United States</option>
                            <option>Canada</option>
                            <option>Mexico</option>
                        </select>
                    </div>

                    <div className="flex space-x-1">
                        <Button label="Description" />

                        <Button label="Auth" />

                        <Button label="Properties" />

                        <Button label="Output" />
                    </div>

                    <div>
                        <label
                            htmlFor="email"
                            className="block text-sm font-medium text-gray-700"
                        >
                            Email
                        </label>

                        <div className="mt-1">
                            <input
                                type="email"
                                name="email"
                                id="email"
                                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                                placeholder="you@example.com"
                            />
                        </div>
                    </div>

                    <div>
                        <label
                            htmlFor="comment"
                            className="block text-sm font-medium text-gray-700"
                        >
                            Add your comment
                        </label>

                        <div className="mt-1">
                            <textarea
                                rows={4}
                                name="comment"
                                id="comment"
                                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                                defaultValue={''}
                            />
                        </div>
                    </div>

                    <div className="mt-auto">
                        <label
                            htmlFor="version"
                            className="block text-sm font-medium text-gray-700"
                        >
                            Version
                        </label>

                        <select
                            id="version"
                            name="version"
                            className="mt-1 block w-full rounded-md border-gray-300 py-2 pl-3 pr-10 text-base focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
                            defaultValue="1.0"
                        >
                            <option>1.0</option>
                            <option>1.1</option>
                            <option>0.9</option>
                        </select>
                    </div>
                </div>
            </div>
        </Provider>
    );
};

export default Sidebar;
