import {buttonVariants} from '@/components/ui/button';
import {cn} from '@/lib/utils';
import React, {ReactNode} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const SidebarSubtitle = ({title}: {title: string}): JSX.Element => (
    <h4 className="px-2 py-1 pr-4 text-sm font-medium tracking-tight text-gray-900">
        {title}
    </h4>
);

export interface LeftSidebarProps {
    bottomBody?: ReactNode;
    bottomTitle?: string;
    className?: string;
    topBody: ReactNode;
    topTitle?: string;
}

const LeftSidebarNav = ({
    bottomBody,
    bottomTitle,
    className,
    topBody,
    topTitle,
}: LeftSidebarProps): JSX.Element => {
    return (
        <div className={twMerge('px-2', className)}>
            {topBody && (
                <div
                    className="mb-4 flex space-x-2 lg:flex-col lg:space-x-0 lg:space-y-1"
                    aria-label={topTitle}
                >
                    {topTitle && <SidebarSubtitle title={topTitle} />}

                    {topBody}
                </div>
            )}

            {bottomBody && (
                <div
                    className="mb-4 flex space-x-2 lg:flex-col lg:space-x-0 lg:space-y-1"
                    aria-label={bottomTitle}
                >
                    {bottomTitle && <SidebarSubtitle title={bottomTitle} />}

                    {bottomBody}
                </div>
            )}
        </div>
    );
};

type LeftSidebarNavItemProps = {
    item: {
        current: boolean;
        name: string;
        onItemClick?: (id?: number | string) => void;
        id?: number | string;
    };
    toLink: string;
    icon?: ReactNode;
};

const LeftSidebarNavItem = ({
    icon,
    item: {current, id, name, onItemClick},
    toLink,
}: LeftSidebarNavItemProps) => (
    <Link
        to={toLink}
        className={cn(
            'justify-start',
            buttonVariants({variant: 'ghost'}),
            current
                ? 'bg-muted hover:bg-muted'
                : 'hover:bg-transparent hover:underline'
        )}
        aria-current={current ? 'page' : undefined}
        onClick={() => (onItemClick ? onItemClick(id) : null)}
    >
        {icon}

        <span className="truncate">{name}</span>
    </Link>
);

export {LeftSidebarNav, LeftSidebarNavItem};
