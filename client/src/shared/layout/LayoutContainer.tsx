import {Dialog, DialogContent} from '@/components/ui/dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {PropsWithChildren, ReactNode, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface SidebarContentLayoutProps {
    className?: string;
    footer?: ReactNode;
    header?: ReactNode;
    leftSidebarBody?: ReactNode;
    leftSidebarClass?: string;
    leftSidebarHeader?: ReactNode;
    leftSidebarOpen?: boolean;
    leftSidebarWidth?: '56' | '64' | '72' | '96' | '112';
    rightSidebarBody?: ReactNode;
    rightSidebarClass?: string;
    rightSidebarHeader?: ReactNode;
    rightSidebarOpen?: boolean;
    rightSidebarWidth?: '96' | '460';
    rightToolbarBody?: ReactNode;
    rightToolbarClass?: string;
    rightToolbarOpen?: boolean;
    topHeader?: ReactNode;
}

const leftSidebarWidths = {
    56: ['lg:w-56', 'lg:pl-56'],
    64: ['lg:w-64', 'lg:pl-64'],
    72: ['lg:w-72', 'lg:pl-72'],
    96: ['lg:w-96', 'lg:pl-96'],
    112: ['lg:w-[432px]', 'lg:pl-[432px]'],
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
    leftSidebarClass,
    leftSidebarHeader,
    leftSidebarOpen = true,
    leftSidebarWidth = '64',
    rightSidebarBody,
    rightSidebarClass,
    rightSidebarHeader,
    rightSidebarOpen = false,
    rightSidebarWidth = '460',
    rightToolbarBody,
    rightToolbarClass,
    rightToolbarOpen = false,
    topHeader,
}: PropsWithChildren<SidebarContentLayoutProps>) => {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    return (
        <div className={twMerge('size-full overflow-auto', className)}>
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
                            <nav className="flex h-full flex-col bg-muted/50">
                                <div className="space-y-1">{leftSidebarBody}</div>
                            </nav>
                        </div>
                    </div>
                </DialogContent>
            </Dialog>

            {leftSidebarOpen && (
                <aside
                    className={twMerge(
                        'hidden lg:flex lg:flex-col border-r border-muted bg-muted/50',
                        'lg:fixed lg:inset-y-0',
                        leftSidebarClass,
                        leftSidebarWidths[leftSidebarWidth][0]
                    )}
                >
                    <nav className="flex h-full flex-col">
                        {leftSidebarHeader}

                        <div className="size-full overflow-y-auto">{leftSidebarBody}</div>
                    </nav>
                </aside>
            )}

            <div
                className={twMerge(
                    'size-full',
                    topHeader && 'flex flex-col',
                    leftSidebarOpen && leftSidebarWidths[leftSidebarWidth][1]
                )}
            >
                {topHeader}

                <div className="flex size-full">
                    <main className="flex size-full flex-col">
                        {header}

                        <div className="flex flex-1 overflow-y-auto">{children}</div>

                        {footer}
                    </main>

                    {rightSidebarOpen && !!rightSidebarBody && (
                        <aside className={twMerge('hidden lg:flex lg:shrink-0', rightSidebarClass)}>
                            <div className={twMerge('flex', rightSidebarWidths[rightSidebarWidth])}>
                                <div className="flex h-full flex-1 flex-col">
                                    {rightSidebarHeader}

                                    {rightSidebarBody}
                                </div>
                            </div>
                        </aside>
                    )}

                    {rightToolbarOpen && !!rightToolbarBody && (
                        <aside className={twMerge('hidden lg:flex lg:shrink-0', rightToolbarClass)}>
                            <div className="flex flex-1 flex-col overflow-y-auto">{rightToolbarBody}</div>
                        </aside>
                    )}
                </div>
            </div>
        </div>
    );
};

export default LayoutContainer;
