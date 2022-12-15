import React, {Fragment, PropsWithChildren, ReactNode, useState} from 'react';
import {Dialog, Transition} from '@headlessui/react';
import {XMarkIcon} from '@heroicons/react/24/outline';
import PageHeader from '../PageHeader/PageHeader';

type Props = {
    sidebar?: ReactNode;
    title: string;
    subTitle: string; //TODO define function
    topRight?: ReactNode;
};

export const SidebarContentLayout: React.FC<PropsWithChildren<Props>> = ({
    title,
    subTitle,
    topRight,
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

            {/* Static sidebar for desktop */}
            <aside className="hidden md:fixed md:inset-y-0 md:flex md:w-72 md:flex-col">
                {/* Sidebar component, swap this element with another sidebar if you like */}
                <nav className="flex h-full flex-col border-gray-200 bg-gray-50 px-4 dark:bg-gray-700">
                    <PageHeader title={title} bold={true} />

                    <div>{sidebar}</div>
                </nav>
            </aside>

            {/* Content area */}
            <div className="h-full md:pl-72">
                <div className="flex h-full flex-col px-6">
                    {/*<div className="sticky top-0 z-10 flex h-16 shrink-0 bg-white">*/}
                    {/*  <button*/}
                    {/*    type="button"*/}
                    {/*    className="border-gray-200 px-4 text-gray-500 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-blue-500 md:hidden"*/}
                    {/*    onClick={() => setSidebarOpen(true)}*/}
                    {/*  >*/}
                    {/*    <span className="sr-only">Open sidebar</span>*/}
                    {/*    <Bars3BottomLeftIcon className="h-6 w-6" aria-hidden="true" />*/}
                    {/*  </button>*/}
                    {/*</div>*/}

                    <PageHeader title={subTitle} right={topRight} />

                    <main className="flex flex-1 overflow-y-auto">
                        {/* Primary column */}

                        <section
                            aria-labelledby="primary-heading"
                            className="flex h-full min-w-0 flex-1 flex-col lg:order-last"
                        >
                            {children}
                        </section>

                        {/*Secondary column (hidden on smaller screens)*/}
                        {/*<aside className="hidden lg:order-first lg:block lg:flex-shrink-0">*/}
                        {/*  <div className="relative flex h-full w-96 flex-col overflow-y-auto border-l border-r border-gray-50 bg-white">*/}
                        {/*    /!* Your content *!/*/}
                        {/*    hh*/}
                        {/*  </div>*/}
                        {/*</aside>*/}
                    </main>
                </div>
            </div>
        </>
    );
};
