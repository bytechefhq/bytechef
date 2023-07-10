import {Dialog, Transition} from '@headlessui/react';
import {XMarkIcon} from '@heroicons/react/24/outline';
import React, {Fragment, PropsWithChildren, ReactNode, useState} from 'react';
import {twMerge} from 'tailwind-merge';

type SidebarContentLayoutProps = {
    className?: string;
    footer?: ReactNode;
    header: ReactNode;
    leftSidebarBody?: ReactNode;
    leftSidebarHeader?: ReactNode;
    leftSidebarOpen?: boolean;
    leftSidebarWidth?: string;
    rightSidebarBody?: ReactNode;
    rightSidebarHeader?: ReactNode;
    rightSidebarOpen?: boolean;
    rightSidebarWidth?: string;
    rightToolbarBody?: ReactNode;
    rightToolbarOpen?: boolean;
};

const leftSidebarWidths = {
    '64': ['md:w-64', 'md:pl-64'],
    '72': ['md:w-72', 'md:pl-72'],
};

const rightSidebarWidths = {
    '460': 'w-[460px]',
};

const LayoutContainer = ({
    children,
    className,
    footer,
    header,
    leftSidebarBody,
    leftSidebarHeader,
    leftSidebarOpen = true,
    leftSidebarWidth = '64',
    rightSidebarBody,
    rightSidebarHeader,
    rightSidebarOpen = false,
    rightSidebarWidth,
    rightToolbarBody,
    rightToolbarOpen = false,
}: PropsWithChildren<SidebarContentLayoutProps>) => {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    return (
        <>
            <Transition.Root show={sidebarOpen} as={Fragment}>
                <Dialog
                    as="div"
                    className="relative z-40 md:hidden"
                    onClose={setSidebarOpen}
                >
                    <Transition.Child
                        as={Fragment}
                        enter="transition-opacity ease-linear duration-300"
                        enterFrom="opacity-0"
                        enterTo="opacity-100"
                        leave="transition-opacity ease-linear duration-300"
                        leaveFrom="opacity-100"
                        leaveTo="opacity-0"
                    >
                        <div className="fixed inset-0 bg-gray-600" />
                    </Transition.Child>

                    <div className="fixed inset-0 z-40 flex">
                        <Transition.Child
                            as={Fragment}
                            enter="transition ease-in-out duration-300 transform"
                            enterFrom="-translate-x-full"
                            enterTo="translate-x-0"
                            leave="transition ease-in-out duration-300 transform"
                            leaveFrom="translate-x-0"
                            leaveTo="-translate-x-full"
                        >
                            <Dialog.Panel className="relative flex w-full max-w-xs flex-1 flex-col bg-white pb-4 pt-5">
                                <Transition.Child
                                    as={Fragment}
                                    enter="ease-in-out duration-300"
                                    enterFrom="opacity-0"
                                    enterTo="opacity-100"
                                    leave="ease-in-out duration-300"
                                    leaveFrom="opacity-100"
                                    leaveTo="opacity-0"
                                >
                                    <div className="absolute right-0 top-0 -mr-14 p-1">
                                        <button
                                            type="button"
                                            className="flex h-12 w-12 items-center justify-center rounded-full focus:bg-gray-600 focus:outline-none"
                                            onClick={() =>
                                                setSidebarOpen(false)
                                            }
                                        >
                                            <XMarkIcon
                                                className="h-6 w-6 text-white"
                                                aria-hidden="true"
                                            />

                                            <span className="sr-only">
                                                Close sidebar
                                            </span>
                                        </button>
                                    </div>
                                </Transition.Child>

                                <div className="mt-5 h-0 flex-1 overflow-y-auto">
                                    <nav className="flex h-full flex-col">
                                        <div className="space-y-1">
                                            {leftSidebarBody}
                                        </div>
                                    </nav>
                                </div>
                            </Dialog.Panel>
                        </Transition.Child>

                        <div className="w-14 shrink-0" aria-hidden="true">
                            {/* Dummy element to force sidebar to shrink to fit close icon */}
                        </div>
                    </div>
                </Dialog>
            </Transition.Root>

            {leftSidebarOpen && (
                <aside
                    className={twMerge(
                        'hidden md:fixed md:inset-y-0 md:flex md:flex-col',
                        leftSidebarWidths[leftSidebarWidth][0]
                    )}
                >
                    <nav className="flex h-full flex-col">
                        {leftSidebarHeader}

                        <div className="overflow-y-auto">{leftSidebarBody}</div>
                    </nav>
                </aside>
            )}

            <div
                className={twMerge(
                    'flex h-full',
                    leftSidebarOpen && leftSidebarWidths[leftSidebarWidth][1]
                )}
            >
                <main
                    className={twMerge(
                        'flex h-full w-full flex-col border-l',
                        className
                    )}
                >
                    {header}

                    <div className="flex flex-1 overflow-y-auto">
                        {children}
                    </div>

                    {footer}
                </main>

                {rightSidebarOpen && !!rightSidebarBody && (
                    <aside className="hidden border-l lg:flex lg:shrink-0">
                        <div
                            className={twMerge(
                                'flex',
                                rightSidebarWidths[rightSidebarWidth]
                            )}
                        >
                            <div className="flex h-full flex-col">
                                {rightSidebarHeader}

                                <div className="overflow-y-auto">
                                    {rightSidebarBody}
                                </div>
                            </div>
                        </div>
                    </aside>
                )}

                {rightToolbarOpen && !!rightToolbarBody && (
                    <aside className="hidden border-l bg-muted lg:flex lg:shrink-0">
                        <div className="flex w-[60px]">
                            <div className="flex flex-1 flex-col overflow-y-auto">
                                {rightToolbarBody}
                            </div>
                        </div>
                    </aside>
                )}
            </div>
        </>
    );
};

export default LayoutContainer;
