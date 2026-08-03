import {Dialog, DialogContent} from '@/components/ui/dialog';
import {XIcon} from 'lucide-react';
import {PropsWithChildren, ReactNode, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface SidebarContentLayoutProps {
    className?: string;
    footer?: ReactNode;
    header?: ReactNode;
    leftSidebarAnimate?: boolean;
    leftSidebarBody?: ReactNode;
    leftSidebarClass?: string;
    leftSidebarHeader?: ReactNode;
    leftSidebarOnMouseEnter?: () => void;
    leftSidebarOnMouseLeave?: () => void;
    leftSidebarOpen?: boolean;
    /** Float the left sidebar OVER the content instead of reserving space for it. The aside still slides
     * in (as when `leftSidebarOpen`), but no content padding is applied, so nothing reflows. Used for the
     * AI Hub tasks-sidebar hover "peek". Independent of `leftSidebarOpen` so a page can overlay the
     * sidebar while its docked (space-reserving) form stays closed. */
    leftSidebarOverlay?: boolean;
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
    leftSidebarAnimate = true,
    leftSidebarBody,
    leftSidebarClass,
    leftSidebarHeader,
    leftSidebarOnMouseEnter,
    leftSidebarOnMouseLeave,
    leftSidebarOpen = true,
    leftSidebarOverlay = false,
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

    // Keep the left sidebar mounted for 300ms after it closes so it can slide out (translate) before
    // unmounting, mirroring the slide-in on open. Pages that never toggle leftSidebarOpen keep this
    // permanently true, so they see no behavior change — only the transform/padding transitions, which
    // are no-ops while the value is static.
    // The aside is shown either docked (reserves space via content padding) or as a floating overlay
    // (the AI Hub peek). Mount + slide track this combined flag; only the content padding below stays
    // keyed on `leftSidebarOpen` so an overlay never reflows the content.
    const asideVisible = leftSidebarOpen || leftSidebarOverlay;

    const [leftAsideMounted, setLeftAsideMounted] = useState(asideVisible);

    useEffect(() => {
        if (asideVisible) {
            setLeftAsideMounted(true);

            return;
        }

        // Sidebar closed. When animation is OFF (e.g. AI Hub's instant collapse to the rail), KEEP the
        // off-screen aside mounted at `-translate-x-full` rather than unmounting it. That way the next
        // animated reopen slides it in from the left edge instead of popping in from a fresh mount (a fresh
        // mount has no prior position to transition from). When animation is ON, unmount after the
        // slide-out completes as before.
        if (!leftSidebarAnimate) {
            return;
        }

        const timerId = setTimeout(() => setLeftAsideMounted(false), 300);

        return () => clearTimeout(timerId);
    }, [asideVisible, leftSidebarAnimate]);

    return (
        <div className={twMerge('size-full overflow-auto', className)}>
            <Dialog open={sidebarOpen}>
                <DialogContent className="h-full sm:max-w-[425px]">
                    <div className="relative">
                        <div className="absolute right-0 p-1">
                            <button
                                className="ml-1 items-center justify-center rounded-full p-2 focus:ring-2 focus:ring-stroke-onsurface-primary-hover focus:outline-hidden focus:ring-inset"
                                onClick={() => setSidebarOpen(false)}
                                type="button"
                            >
                                <XIcon aria-hidden="true" className="size-4" />

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

            {leftAsideMounted && (
                <aside
                    className={twMerge(
                        'hidden border-r border-r-border/50 bg-muted/50 lg:flex lg:flex-col',
                        'lg:fixed lg:inset-y-0',
                        leftSidebarAnimate && 'transition-transform duration-300 ease-in-out',
                        asideVisible ? 'lg:translate-x-0' : 'lg:-translate-x-full',
                        // Floating peek: lift above the content and cast a shadow so it reads as an overlay
                        // rather than part of the flow (it reserves no space). The docked sidebar's
                        // `bg-muted/50` is semi-transparent — fine over the opaque page, but it would let
                        // the content bleed through when floating, so force an OPAQUE background here.
                        leftSidebarOverlay && 'z-40 bg-muted shadow-lg',
                        leftSidebarClass,
                        leftSidebarWidths[leftSidebarWidth][0]
                    )}
                    onMouseEnter={leftSidebarOnMouseEnter}
                    onMouseLeave={leftSidebarOnMouseLeave}
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
                    leftSidebarAnimate && 'transition-[padding] duration-300 ease-in-out',
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
