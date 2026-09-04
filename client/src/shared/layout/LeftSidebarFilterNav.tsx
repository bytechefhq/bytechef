import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ReactNode} from 'react';

export interface LeftSidebarFilterNavItemI {
    current: boolean;
    id: number | string;
    name: string;
    toLink: string;
}

export interface LeftSidebarFilterNavLeadItemI {
    current: boolean;
    name: string;
    toLink?: string;
}

interface LeftSidebarFilterNavProps {
    className?: string;
    /** Shown in place of the rows once the list has arrived and turned out to be empty. */
    emptyMessage?: string;
    /** Rendered on every row, the way tag groups carry a tag icon. */
    icon?: ReactNode;
    items: LeftSidebarFilterNavItemI[];
    /** The unfiltered entry a group opens with, such as "All Categories". */
    leadItem?: LeftSidebarFilterNavLeadItemI;
    loading?: boolean;
    title: string;
}

const LeftSidebarFilterNav = ({
    className,
    emptyMessage,
    icon,
    items,
    leadItem,
    loading = false,
    title,
}: LeftSidebarFilterNavProps) => (
    <LeftSidebarNav
        body={
            <>
                {leadItem && (
                    <LeftSidebarNavItem
                        item={{current: leadItem.current, name: leadItem.name}}
                        toLink={leadItem.toLink ?? ''}
                    />
                )}

                {items.length
                    ? items.map((item) => (
                          <LeftSidebarNavItem
                              icon={icon}
                              item={{current: item.current, id: item.id, name: item.name}}
                              key={item.id}
                              toLink={item.toLink}
                          />
                      ))
                    : emptyMessage && <span className="px-3 text-xs">{emptyMessage}</span>}
            </>
        }
        className={className}
        loading={loading}
        title={title}
    />
);

export default LeftSidebarFilterNav;
