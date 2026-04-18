import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface NoteProps {
    className?: string;
    content: string;
    icon?: ReactNode;
}

export const Note = ({className, content, icon}: NoteProps) => (
    <div
        className={twMerge(
            'relative flex items-center rounded-md border border-stroke-warning-secondary bg-surface-warning-secondary p-4',
            icon && 'gap-2',
            className
        )}
    >
        {icon && icon}

        <p className="text-sm font-medium">{content}</p>
    </div>
);
