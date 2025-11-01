import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface EmptyListProps {
    button?: ReactNode;
    className?: string;
    icon: ReactNode;
    title: string;
    message?: string;
}

const EmptyList = ({button, className, icon, message, title}: EmptyListProps) => (
    <div className={twMerge('w-full place-self-center px-2 3xl:mx-auto 3xl:w-4/5', className)}>
        <div className="text-center">
            <span className="mx-auto inline-block">{icon}</span>

            <h3 className="mt-2 text-sm font-semibold">{title}</h3>

            {message && <p className="mt-1 text-sm text-gray-500">{message}</p>}

            {button && <div className="mt-6">{button}</div>}
        </div>
    </div>
);

export default EmptyList;
