import React, {Fragment, PropsWithChildren, ReactNode, useState} from 'react';
import {Dialog, Transition} from '@headlessui/react';
import {XMarkIcon} from '@heroicons/react/24/outline';
import PageHeader from '../PageHeader/PageHeader';

type Props = {
    headerProps: {
        subTitle: string;
        buttonLabel?: string;
        buttonOnClick?: () => void;
        right?: ReactNode;
    };
    sidebar?: ReactNode;
    title: string;
    topRight?: ReactNode;
};

const SidebarContentLayout: React.FC<PropsWithChildren<Props>> = ({
    headerProps,
    title,
    sidebar,
    children,
}) => {
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
                            <Dialog.Panel className="relative flex w-full max-w-xs flex-1 flex-col bg-white pt-5 pb-4">
                                <Transition.Child
                                    as={Fragment}
                                    enter="ease-in-out duration-300"
                                    enterFrom="opacity-0"
                                    enterTo="opacity-100"
                                    leave="ease-in-out duration-300"
                                    leaveFrom="opacity-100"
                                    leaveTo="opacity-0"
                                >
                                    <div className="absolute top-0 right-0 -mr-14 p-1">
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
                                            {sidebar}
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

            <aside className="hidden md:fixed md:inset-y-0 md:flex md:w-64 md:flex-col">
                <nav className="flex h-full flex-col border-l border-gray-100 bg-gray-50 px-2 dark:border-l dark:border-l-gray-700 dark:bg-gray-800">
                    <h1
                        aria-labelledby="primary-heading"
                        className="py-4 px-2 pr-4 text-xl font-semibold tracking-tight text-gray-900 dark:text-gray-200"
                    >
                        {title}
                    </h1>

                    <div>{sidebar}</div>
                </nav>
            </aside>

            <div className="h-full md:pl-64">
                <div className="flex h-full flex-col px-4">
                    <PageHeader
                        buttonLabel={headerProps.buttonLabel}
                        buttonOnClick={headerProps.buttonOnClick}
                        right={headerProps.right}
                        title={headerProps.subTitle}
                    />

                    <main className="flex flex-1 overflow-y-auto">
                        <section className="flex h-full min-w-0 flex-1 flex-col lg:order-last">
                            {children}
                        </section>
                    </main>
                </div>
            </div>
        </>
    );
};

export default SidebarContentLayout;
