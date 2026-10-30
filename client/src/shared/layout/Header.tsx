import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

import LeftSidebarToggle from './LeftSidebarToggle';
import {useLeftSidebarToggle} from './LeftSidebarToggleContext';

interface HeaderProps {
    centerTitle?: boolean;
    className?: string;
    description?: string;
    position?: 'main' | 'sidebar';
    right?: ReactNode;
    title: string | ReactNode;
    titleClassName?: string;
}

const Header = ({
    centerTitle = false,
    className,
    description,
    position = 'sidebar',
    right,
    title,
    titleClassName,
}: HeaderProps) => {
    const {hasLeftSidebar, leftSidebarHidden, leftSidebarOpen} = useLeftSidebarToggle();

    // An empty page has no title to sit beside, so the toggle would float alone. It stays while the sidebar is
    // closed, since it is then the only way back to it.
    const showLeftSidebarToggle = position === 'main' && hasLeftSidebar && (!!title || !leftSidebarOpen);

    return (
        <header className="relative">
            <div
                className={twMerge(
                    'flex items-center justify-between p-4',
                    centerTitle ? '3xl:mx-auto 3xl:w-4/5' : '3xl:w-4/5',
                    className
                )}
            >
                {/* The toggle sits beside the title-and-description block, top-aligned so it stays level with
                    the title however tall the right slot is. Sidebar headers never carry it. A centered header
                    pins it to the sidebar edge instead, so it does not drift into the page with the title. */}

                <div className="flex items-start gap-2 self-start">
                    {showLeftSidebarToggle && (
                        <div
                            className={twMerge(
                                'flex h-header-height items-center',
                                centerTitle && '3xl:absolute 3xl:top-4 3xl:left-4'
                            )}
                        >
                            <LeftSidebarToggle />
                        </div>
                    )}

                    <div className="flex flex-col">
                        <div
                            className={twMerge(
                                'flex h-header-height flex-col justify-center text-lg tracking-tight text-foreground',
                                // A main header standing in for a hidden sidebar shows the page title as that would.
                                position === 'sidebar' || leftSidebarHidden ? 'font-semibold' : '',
                                titleClassName
                            )}
                        >
                            {title}
                        </div>

                        <div className="text-sm text-muted-foreground">{description}</div>
                    </div>
                </div>

                {right && <div>{right}</div>}
            </div>
        </header>
    );
};

export default Header;
