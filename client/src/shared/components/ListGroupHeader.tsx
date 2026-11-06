import {ReactNode} from 'react';

interface ListGroupHeaderProps {
    action?: ReactNode;
    icon: ReactNode;
    name?: string;
}

const ListGroupHeader = ({action, icon, name}: ListGroupHeaderProps) => (
    <div className="flex items-center gap-2 pt-5 pb-2">
        <span className="text-content-neutral-secondary">{icon}</span>

        <h2 className="truncate text-base font-semibold">{name}</h2>

        {action}
    </div>
);

export default ListGroupHeader;
