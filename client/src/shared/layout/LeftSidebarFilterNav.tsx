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

/**
 * One filter group of a left sidebar: an optional unfiltered lead entry, a row per value, and an empty message
 * when the list arrives empty. Every list page's rail is built from these, which is why the group owns the
 * markup rather than each page repeating it.
 *
 * Callers decide what a row means — its label, its link and whether it is the current filter — and pass rows
 * already in that shape.
 */
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
