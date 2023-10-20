import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import React, {Fragment} from 'react';
import {
    Arrow,
    Content,
    Provider,
    Root,
    Trigger,
    Portal,
} from '@radix-ui/react-tooltip';
import Button from 'components/Button/Button';
import {Dialog, Transition} from '@headlessui/react';
import Select from '../../../components/Select/Select';
import useRightSlideOverStore from '../stores/rightSlideOver.store';

type SidebarProps = {
    open: boolean;
    closeSidebar: () => void;
};

const RightSlideOver = ({open, closeSidebar}: SidebarProps) => {
    const {currentNode} = useRightSlideOverStore();

    return (
        <Transition.Root show={open} as={Fragment}>
            <Dialog as="div" className="relative z-10" onClose={closeSidebar}>
                <div className="fixed inset-0" />

                <div className="fixed inset-0 overflow-hidden">
                    <div className="absolute inset-0 overflow-hidden">
                        <div className="pointer-events-none fixed inset-y-0 right-0 flex max-w-full pl-10">
                            <Transition.Child
                                as={Fragment}
                                enter="transform transition ease-in-out duration-500 sm:duration-700"
                                enterFrom="translate-x-full"
                                enterTo="translate-x-0"
                                leave="transform transition ease-in-out duration-500 sm:duration-700"
                                leaveFrom="translate-x-0"
                                leaveTo="translate-x-full"
                            >
                                <Dialog.Panel className="pointer-events-auto w-screen max-w-md">
                                    <div className="flex h-full flex-col divide-y divide-gray-100 bg-white shadow-xl">
                                        <div className="flex min-h-0 flex-1 flex-col overflow-y-scroll py-6">
                                            <div className="px-4 sm:px-4">
                                                <div className="flex items-start justify-between">
                                                    <Dialog.Title className="text-lg font-medium text-gray-900">
                                                        <div className="flex items-center">
                                                            <span className="mr-2 text-base">
                                                                {
                                                                    currentNode.label
                                                                }
                                                            </span>

                                                            <Provider>
                                                                <Root>
                                                                    <Trigger
                                                                        asChild
                                                                    >
                                                                        <InfoCircledIcon className="h-4 w-4" />
                                                                    </Trigger>
                                                                    <Portal>
                                                                        <Content
                                                                            sideOffset={
                                                                                4
                                                                            }
                                                                            className="inline-flex items-center rounded-md bg-gray-800 px-4 py-2.5 radix-side-bottom:animate-slide-up-fade radix-side-left:animate-slide-right-fade radix-side-right:animate-slide-left-fade radix-side-top:animate-slide-down-fade dark:bg-gray-800"
                                                                        >
                                                                            <Arrow className="fill-current text-gray-800 dark:text-gray-800" />

                                                                            <span className="block text-xs leading-none text-gray-100 dark:text-gray-100">
                                                                                Information
                                                                            </span>
                                                                        </Content>
                                                                    </Portal>
                                                                </Root>
                                                            </Provider>
                                                        </div>
                                                    </Dialog.Title>

                                                    <div className="ml-3 flex h-7 items-center">
                                                        <button
                                                            type="button"
                                                            className="rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
                                                            onClick={() =>
                                                                closeSidebar()
                                                            }
                                                        >
                                                            <span className="sr-only">
                                                                Close panel
                                                            </span>
                                                            <Cross1Icon
                                                                className="h-3 w-3 text-gray-900 hover:cursor-pointer"
                                                                aria-hidden="true"
                                                            />
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                            <div className="relative mt-6 flex-1 px-4 sm:px-4">
                                                <div className="h-full">
                                                    <Provider>
                                                        <div className="flex h-full flex-col space-y-4">
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
                                                                    <option>
                                                                        United
                                                                        States
                                                                    </option>
                                                                    <option>
                                                                        Canada
                                                                    </option>
                                                                    <option>
                                                                        Mexico
                                                                    </option>
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
                                                                    Add your
                                                                    comment
                                                                </label>

                                                                <div className="mt-1">
                                                                    <textarea
                                                                        rows={4}
                                                                        name="comment"
                                                                        id="comment"
                                                                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                                                                        defaultValue={
                                                                            ''
                                                                        }
                                                                    />
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </Provider>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="flex shrink-0 justify-start p-4">
                                            <Select
                                                defaultValue={'2'}
                                                selectItems={[
                                                    {label: 'v 1', value: '1'},
                                                    {label: 'v 2', value: '2'},
                                                    {label: 'v 3', value: '3'},
                                                ]}
                                            />
                                        </div>
                                    </div>
                                </Dialog.Panel>
                            </Transition.Child>
                        </div>
                    </div>
                </div>
            </Dialog>
        </Transition.Root>
    );
};

export default RightSlideOver;
