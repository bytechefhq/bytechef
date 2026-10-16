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
    const {hasLeftSidebar} = useLeftSidebarToggle();

    return (
        <header className={twMerge('p-4', centerTitle ? '3xl:mx-auto 3xl:w-4/5' : '3xl:w-4/5', className)}>
            <div className="flex w-full items-center justify-between">
                {/* The toggle sits beside the title-and-description block, top-aligned so it stays level with
                    the title however tall the right slot is. Sidebar headers never carry it. */}

                <div className="flex items-start gap-2 self-start">
                    {position === 'main' && hasLeftSidebar && (
                        <div className="flex h-header-height items-center">
                            <LeftSidebarToggle />
                        </div>
                    )}

                    <div className="flex flex-col">
                        <div
                            className={twMerge(
                                'flex h-header-height flex-col justify-center text-lg tracking-tight text-foreground',
                                position === 'sidebar' ? 'font-semibold' : '',
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
