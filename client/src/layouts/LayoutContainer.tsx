import {Dialog, DialogContent} from '@/components/ui/dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {PropsWithChildren, ReactNode, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface SidebarContentLayoutProps {
    className?: string;
    footer?: ReactNode;
    header?: ReactNode;
    leftSidebarBody?: ReactNode;
    leftSidebarHeader?: ReactNode;
    leftSidebarOpen?: boolean;
    leftSidebarWidth?: '56' | '64' | '72' | '96' | '112';
    rightSidebarBody?: ReactNode;
    rightSidebarHeader?: ReactNode;
    rightSidebarOpen?: boolean;
    rightSidebarWidth?: '96' | '460';
    rightToolbarBody?: ReactNode;
    rightToolbarOpen?: boolean;
}

const leftSidebarWidths = {
    56: ['md:w-56', 'md:pl-56'],
    64: ['md:w-64', 'md:pl-64'],
    72: ['md:w-72', 'md:pl-72'],
    96: ['md:w-96', 'md:pl-96'],
    112: ['md:w-[432px]', 'md:pl-[432px]'],
};

const rightSidebarWidths = {
    96: 'w-96',
    460: 'w-[460px]',
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
    rightSidebarWidth = '460',
    rightToolbarBody,
    rightToolbarOpen = false,
}: PropsWithChildren<SidebarContentLayoutProps>) => {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    return (
        <>
            <Dialog open={sidebarOpen}>
                <DialogContent className="h-full sm:max-w-[425px]">
                    <div className="relative">
                        <div className="absolute right-0 p-1">
                            <button
                                className="ml-1 items-center justify-center rounded-full p-2 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
                                onClick={() => setSidebarOpen(false)}
                                type="button"
                            >
                                <Cross2Icon aria-hidden="true" className="size-4" />

                                <span className="sr-only">Close sidebar</span>
                            </button>
                        </div>

                        <div className="absolute inset-0 mt-5 overflow-auto">
                            <nav className="flex h-full flex-col">
                                <div className="space-y-1">{leftSidebarBody}</div>
                            </nav>
                        </div>
                    </div>
                </DialogContent>
            </Dialog>

            {leftSidebarOpen && (
                <aside
                    className={twMerge(
                        'hidden md:fixed md:inset-y-0 md:flex md:flex-col border-r',
                        leftSidebarWidths[leftSidebarWidth][0]
                    )}
                >
                    <nav className="flex h-full flex-col">
                        {leftSidebarHeader}

                        <div className="overflow-y-auto">{leftSidebarBody}</div>
                    </nav>
                </aside>
            )}

            <div className={twMerge('flex h-full w-full', leftSidebarOpen && leftSidebarWidths[leftSidebarWidth][1])}>
                <main className={twMerge('flex h-full w-full flex-col', className)}>
                    {header}

                    <div className="flex flex-1 overflow-y-auto">{children}</div>

                    {footer}
                </main>

                {rightSidebarOpen && !!rightSidebarBody && (
                    <aside className="hidden border-l lg:flex lg:shrink-0">
                        <div className={twMerge('flex', rightSidebarWidths[rightSidebarWidth])}>
                            <div className="flex h-full flex-1 flex-col">
                                {rightSidebarHeader}

                                <div className="overflow-y-auto">{rightSidebarBody}</div>
                            </div>
                        </div>
                    </aside>
                )}

                {rightToolbarOpen && !!rightToolbarBody && (
                    <aside className="hidden border-l bg-muted lg:flex lg:shrink-0">
                        <div className="flex flex-1 flex-col overflow-y-auto">{rightToolbarBody}</div>
                    </aside>
                )}
            </div>
        </>
    );
};

export default LayoutContainer;
