import {ReactNode} from 'react';

interface EmptyListProps {
    icon: ReactNode;
    title: string;
    button?: ReactNode;
    message?: string;
}

const EmptyList = ({button, icon, message, title}: EmptyListProps) => (
    <div className="w-full place-self-center px-2 3xl:mx-auto 3xl:w-4/5">
        <div className="text-center">
            <span className="mx-auto inline-block">{icon}</span>

            <h3 className="mt-2 text-sm font-semibold">{title}</h3>

            {message && <p className="mt-1 text-sm text-gray-500">{message}</p>}

            {button && <div className="mt-6">{button}</div>}
        </div>
    </div>
);

export default EmptyList;
