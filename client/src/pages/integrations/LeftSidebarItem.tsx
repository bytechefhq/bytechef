import React, {ReactNode} from 'react';
import cx from 'classnames';
import {Link} from 'react-router-dom';

export enum Type {
    Category,
    Tag,
}

interface LeftSidebarItemProps {
    item: {
        id?: number;
        name: string;
        type: Type;
        current: boolean;
        onItemClick: (type: Type, id?: number) => void;
    };
    icon?: ReactNode;
}

const LeftSidebarItem = ({
    item: {id, name, type, current, onItemClick},
    icon,
}: LeftSidebarItemProps) => {
    return (
        <Link
            to={
                (id ? '?' : '') +
                (type === Type.Tag && id ? `tagId=${id}` : '') +
                (type === Type.Category && id ? `categoryId=${id}` : '')
            }
            className={cx(
                current
                    ? 'bg-gray-200 text-gray-900'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-600',
                'flex items-center rounded-md px-2 py-2 text-sm font-medium'
            )}
            aria-current={current ? 'page' : undefined}
            onClick={() => onItemClick(type, id)}
        >
            {icon} <span className="truncate">{name}</span>
        </Link>
    );
};

export default LeftSidebarItem;
