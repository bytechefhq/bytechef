import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

export const Note = ({content, icon}: {content: string; icon?: ReactNode}) => (
    <div className={twMerge('relative flex items-center rounded-md bg-surface-warning-secondary p-4', icon && 'gap-2')}>
        {icon && icon}

        <p className="text-sm font-medium">{content}</p>
    </div>
);
