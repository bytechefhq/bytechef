import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface ListGroupHeaderProps {
    action?: ReactNode;
    className?: string;
    icon: ReactNode;
    name?: string;
}

const ListGroupHeader = ({action, className, icon, name}: ListGroupHeaderProps) => (
    <div className={twMerge('flex items-center gap-2 pt-5 pb-2', className)}>
        <span className="text-content-neutral-secondary">{icon}</span>

        <h2 className="truncate text-base font-semibold">{name}</h2>

        {action}
    </div>
);

export default ListGroupHeader;
