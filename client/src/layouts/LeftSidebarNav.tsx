import {buttonVariants} from '@/components/ui/button';
import {cn} from '@/lib/utils';
import {ReactNode} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const SidebarSubtitle = ({title}: {title: string}) => (
    <h4 className="px-2 py-1 pr-4 text-sm font-medium tracking-tight">{title}</h4>
);

export interface LeftSidebarNavProps {
    body?: ReactNode;
    title?: string;
    className?: string;
}

const LeftSidebarNav = ({body, className, title}: LeftSidebarNavProps) => (
    <div className={twMerge('px-2', className)}>
        {body && (
            <div aria-label={title} className="mb-4 flex space-x-2 lg:flex-col lg:space-x-0">
                {title && <SidebarSubtitle title={title} />}

                {body}
            </div>
        )}
    </div>
);

interface LeftSidebarNavItemProps {
    item: {
        filterData: boolean;
        name: string;
        onItemClick?: (id?: number | string) => void;
        id?: number | string;
    };
    toLink?: string;
    icon?: ReactNode;
}

const LeftSidebarNavItem = ({
    icon,
    item: {filterData, id, name, onItemClick},
    toLink = '',
}: LeftSidebarNavItemProps) => (
    <Link
        aria-current={filterData ? 'page' : undefined}
        className={cn(
            buttonVariants({variant: 'ghost'}),
            filterData ? 'bg-muted hover:bg-muted' : 'hover:bg-muted',
            'justify-start'
        )}
        onClick={() => (onItemClick ? onItemClick(id) : null)}
        to={toLink}
    >
        {icon}

        <span className="truncate">{name}</span>
    </Link>
);

export {LeftSidebarNav, LeftSidebarNavItem};
