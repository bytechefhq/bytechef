import {buttonVariants} from '@/components/ui/button';
import {Skeleton} from '@/components/ui/skeleton';
import {cn} from '@/shared/util/cn-utils';
import {ReactNode} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const SidebarSubtitle = ({title}: {title: string}) => (
    <h4 className="px-2 py-1 pr-4 text-sm font-medium tracking-tight text-muted-foreground">{title}</h4>
);

const SKELETON_ROW_WIDTHS = ['w-3/4', 'w-1/2', 'w-2/3', 'w-3/5', 'w-4/5'];

const LeftSidebarNavSkeleton = ({rows}: {rows: number}) => (
    <div aria-busy="true" aria-label="Loading" data-testid="left-sidebar-nav-skeleton" role="status">
        {Array.from({length: rows}).map((_, rowIndex) => (
            <div className="flex h-9 items-center px-2" key={rowIndex}>
                <Skeleton className={twMerge('h-4', SKELETON_ROW_WIDTHS[rowIndex % SKELETON_ROW_WIDTHS.length])} />
            </div>
        ))}
    </div>
);

export interface LeftSidebarNavProps {
    body: ReactNode;
    title?: string;
    className?: string;
    loading?: boolean;
    loadingRows?: number;
}

const LeftSidebarNav = ({body, className, loading = false, loadingRows = 4, title}: LeftSidebarNavProps) => (
    <div className={twMerge('mb-4 px-2', className)}>
        <div aria-label={title} className="flex space-x-2 lg:flex-col lg:space-x-0">
            {title && <SidebarSubtitle title={title} />}

            {loading ? <LeftSidebarNavSkeleton rows={loadingRows} /> : body}
        </div>
    </div>
);

interface LeftSidebarNavItemProps {
    disabled?: boolean;
    item: {
        current: boolean;
        name: string;
        onItemClick?: (id?: number | string) => void;
        id?: number | string;
    };
    toLink?: string;
    icon?: ReactNode;
}

const LeftSidebarNavItem = ({
    disabled = false,
    icon,
    item: {current, id, name, onItemClick},
    toLink = '',
}: LeftSidebarNavItemProps) => (
    <Link
        aria-current={current ? 'page' : undefined}
        aria-disabled={disabled || undefined}
        className={cn(
            buttonVariants({variant: 'ghost'}),
            current ? 'bg-accent hover:bg-accent' : 'hover:bg-accent',
            'w-full justify-start px-2 font-normal',
            disabled && 'pointer-events-none opacity-50'
        )}
        onClick={(event) => {
            if (disabled) {
                event.preventDefault();

                return;
            }

            if (onItemClick) {
                onItemClick(id);
            }
        }}
        tabIndex={disabled ? -1 : undefined}
        to={toLink}
    >
        {icon}

        <span className={cn('truncate', current && 'font-semibold')}>{name}</span>
    </Link>
);

export {LeftSidebarNav, LeftSidebarNavItem};
