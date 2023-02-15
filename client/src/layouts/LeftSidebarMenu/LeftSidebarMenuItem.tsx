import React, {ReactNode} from 'react';
import cx from 'classnames';
import {Link} from 'react-router-dom';

interface LeftSidebarItemProps {
    item: {
        id?: number | string;
        name: string;
        current: boolean;
        onItemClick: (id?: number | string) => void;
    };
    icon?: ReactNode;
    toLink: string;
}

const LeftSidebarMenuItem = ({
    item: {id, name, current, onItemClick},
    icon,
    toLink,
}: LeftSidebarItemProps) => {
    return (
        <Link
            to={toLink}
            className={cx(
                current
                    ? 'bg-gray-200 text-gray-900'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-600',
                'flex items-center rounded-md px-2 py-2 text-sm font-medium'
            )}
            aria-current={current ? 'page' : undefined}
            onClick={() => onItemClick(id)}
        >
            {icon} <span className="truncate">{name}</span>
        </Link>
    );
};

export default LeftSidebarMenuItem;
