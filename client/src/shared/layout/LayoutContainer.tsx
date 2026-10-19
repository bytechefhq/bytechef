import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {useIsMobile} from '@/shared/hooks/useIsMobile';
import {PropsWithChildren, ReactNode, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import LeftSidebarToggleContext, {useLeftSidebarToggle} from './LeftSidebarToggleContext';

interface SidebarContentLayoutProps {
    className?: string;
    footer?: ReactNode;
    header?: ReactNode;
    leftSidebarBody?: ReactNode;
    leftSidebarClass?: string;
    leftSidebarHeader?: ReactNode;
    leftSidebarHidden?: boolean;
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
    leftSidebarHidden = false,
    leftSidebarOpen,
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
    // Uncontrolled by default so the header's toggle works on any page with a sidebar without that page
    // owning the state. A page that passes leftSidebarOpen keeps full control.
    const [uncontrolledLeftSidebarOpen, setUncontrolledLeftSidebarOpen] = useState(true);
    const [mobileLeftSidebarOpen, setMobileLeftSidebarOpen] = useState(false);

    const parentLeftSidebarToggle = useLeftSidebarToggle();

    const isMobile = useIsMobile();

    const isLeftSidebarControlled = leftSidebarOpen !== undefined;

    // Hidden is for a page with nothing in it to filter (an empty list's first run). Unlike closed, it skips the
    // slide-out: the page only learns it is empty once loaded, and animating then would jolt the layout.
    const leftSidebarIsOpen =
        !leftSidebarHidden && (isLeftSidebarControlled ? leftSidebarOpen : uncontrolledLeftSidebarOpen);

    const [leftAsideMounted, setLeftAsideMounted] = useState(leftSidebarIsOpen);

    const ownLeftSidebarToggle = useMemo(
        () => ({
            hasLeftSidebar: !!leftSidebarBody && !isLeftSidebarControlled && !leftSidebarHidden,
            leftSidebarHidden,
            leftSidebarOpen: isMobile ? mobileLeftSidebarOpen : leftSidebarIsOpen,
            toggleLeftSidebar: () =>
                isMobile ? setMobileLeftSidebarOpen((open) => !open) : setUncontrolledLeftSidebarOpen((open) => !open),
        }),
        [
            isLeftSidebarControlled,
            isMobile,
            leftSidebarBody,
            leftSidebarHidden,
            leftSidebarIsOpen,
            mobileLeftSidebarOpen,
        ]
    );

    // A LayoutContainer with no sidebar of its own must not shadow an outer one's toggle: every Settings page
    // renders into the Settings layout's Outlet with its own sidebar-less LayoutContainer.
    const leftSidebarToggle = leftSidebarBody ? ownLeftSidebarToggle : parentLeftSidebarToggle;

    // Keep the left sidebar mounted for 300ms after it closes so it can slide out before unmounting.
    useEffect(() => {
        if (leftSidebarIsOpen) {
            setLeftAsideMounted(true);

            return;
        }

        const timerId = setTimeout(() => setLeftAsideMounted(false), 300);

        return () => clearTimeout(timerId);
    }, [leftSidebarIsOpen]);

    const leftSidebarNav = (
        <nav className="flex h-full flex-col">
            {leftSidebarHeader}

            <div className="size-full overflow-y-auto px-2 pb-4">{leftSidebarBody}</div>
        </nav>
    );

    return (
        <LeftSidebarToggleContext.Provider value={leftSidebarToggle}>
            <div className={twMerge('size-full overflow-auto', className)}>
                {leftAsideMounted && !leftSidebarHidden && (
                    <aside
                        className={twMerge(
                            'hidden border-r border-r-border/50 bg-muted/50 lg:flex lg:flex-col',
                            'lg:fixed lg:inset-y-0',
                            'transition-transform duration-300 ease-in-out',
                            leftSidebarIsOpen ? 'lg:translate-x-0' : 'lg:-translate-x-full',
                            leftSidebarClass,
                            leftSidebarWidths[leftSidebarWidth][0]
                        )}
                    >
                        {leftSidebarNav}
                    </aside>
                )}

                {isMobile && ownLeftSidebarToggle.hasLeftSidebar && (
                    <Sheet onOpenChange={setMobileLeftSidebarOpen} open={mobileLeftSidebarOpen}>
                        <SheetContent className="w-72 bg-muted p-0" side="left">
                            <SheetHeader className="sr-only">
                                <SheetTitle>Sidebar</SheetTitle>

                                <SheetDescription>Displays the page sidebar.</SheetDescription>
                            </SheetHeader>

                            {leftSidebarNav}
                        </SheetContent>
                    </Sheet>
                )}

                <div
                    className={twMerge(
                        'size-full',
                        !leftSidebarHidden && 'transition-[padding] duration-300 ease-in-out',
                        topHeader && 'flex flex-col',
                        leftSidebarIsOpen && leftSidebarWidths[leftSidebarWidth][1]
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
        </LeftSidebarToggleContext.Provider>
    );
};

export default LayoutContainer;
