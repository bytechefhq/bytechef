import {ReactNode} from 'react';

interface EmptyListProps {
    icon: ReactNode;
    title: string;
    button?: ReactNode;
    message?: string;
}

const EmptyList = ({
    button,
    icon,
    title,
    message,
}: EmptyListProps): JSX.Element => (
    <div className="text-center">
        <span className="mx-auto inline-block">{icon}</span>

        <h3 className="mt-2 text-sm font-semibold text-gray-900">{title}</h3>

        {message && <p className="mt-1 text-sm text-gray-500">{message}</p>}

        {button && <div className="mt-6">{button}</div>}
    </div>
);

export default EmptyList;
