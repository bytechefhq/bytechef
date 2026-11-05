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
    const {hasLeftSidebar, hasLeftSidebarHeader, insideLeftSidebar, leftSidebarHidden, leftSidebarOpen} =
        useLeftSidebarToggle();

    const showLeftSidebarToggle =
        position === 'main' && hasLeftSidebar && (!leftSidebarOpen || (!hasLeftSidebarHeader && !!title));

    const showSidebarHeaderToggle = position === 'sidebar' && insideLeftSidebar && hasLeftSidebar && leftSidebarOpen;

    return (
        <header className="relative">
            <div
                className={twMerge(
                    'flex items-center justify-between p-4',
                    !insideLeftSidebar && (centerTitle ? '3xl:mx-auto 3xl:w-4/5' : '3xl:w-4/5'),
                    className
                )}
            >
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
                                position === 'sidebar' || leftSidebarHidden ? 'font-semibold' : '',
                                titleClassName
                            )}
                        >
                            {title}
                        </div>

                        <div className="text-sm text-muted-foreground">{description}</div>
                    </div>
                </div>

                {showSidebarHeaderToggle ? (
                    <div className="flex items-center gap-1">
                        {right && <div>{right}</div>}

                        <LeftSidebarToggle />
                    </div>
                ) : (
                    right && <div>{right}</div>
                )}
            </div>
        </header>
    );
};

export default Header;
