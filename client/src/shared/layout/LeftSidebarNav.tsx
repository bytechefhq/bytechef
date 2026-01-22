import {buttonVariants} from '@/components/ui/button';
import {cn} from '@/shared/util/cn-utils';
import {ReactNode} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const SidebarSubtitle = ({title}: {title: string}) => (
    <h4 className="px-2 py-1 pr-4 text-sm font-medium tracking-tight text-muted-foreground">{title}</h4>
);

export interface LeftSidebarNavProps {
    body: ReactNode;
    title?: string;
    className?: string;
}

const LeftSidebarNav = ({body, className, title}: LeftSidebarNavProps) => (
    <div className={twMerge('mb-4 px-2', className)}>
        <div aria-label={title} className="flex space-x-2 lg:flex-col lg:space-x-0">
            {title && <SidebarSubtitle title={title} />}

            {body}
        </div>
    </div>
);

interface LeftSidebarNavItemProps {
    item: {
        current: boolean;
        name: string;
        onItemClick?: (id?: number | string) => void;
        id?: number | string;
    };
    toLink?: string;
    icon?: ReactNode;
}

const LeftSidebarNavItem = ({icon, item: {current, id, name, onItemClick}, toLink = ''}: LeftSidebarNavItemProps) => (
    <Link
        aria-current={current ? 'page' : undefined}
        className={cn(
            buttonVariants({variant: 'ghost'}),
            current ? 'bg-accent hover:bg-accent' : 'hover:bg-accent',
            'w-full justify-start px-2 font-normal'
        )}
        onClick={() => (onItemClick ? onItemClick(id) : null)}
        to={toLink}
    >
        {icon}

        <span className={cn('truncate', current && 'font-semibold')}>{name}</span>
    </Link>
);

export {LeftSidebarNav, LeftSidebarNavItem};
