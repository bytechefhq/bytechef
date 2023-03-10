import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';
import {Link} from 'react-router-dom';

type LeftSidebarItemProps = {
    item: {
        current: boolean;
        name: string;
        onItemClick: (id?: number | string) => void;
        id?: number | string;
    };
    toLink: string;
    icon?: ReactNode;
};

const LeftSidebarMenuItem = ({
    icon,
    item: {id, name, current, onItemClick},
    toLink,
}: LeftSidebarItemProps) => (
    <Link
        to={toLink}
        className={twMerge(
            current
                ? 'bg-gray-200 text-gray-900'
                : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-600',
            'flex items-center rounded-md px-2 py-2 text-sm font-medium'
        )}
        aria-current={current ? 'page' : undefined}
        onClick={() => onItemClick(id)}
    >
        {icon}

        <span className="truncate">{name}</span>
    </Link>
);

export default LeftSidebarMenuItem;
