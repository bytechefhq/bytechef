import Button from '@/components/Button/Button';
import {XIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface NoteProps {
    className?: string;
    content: string;
    icon?: ReactNode;
    onClose?: () => void;
}

export const Note = ({className, content, icon, onClose}: NoteProps) => (
    <div
        className={twMerge(
            'relative flex items-center rounded-md border border-stroke-warning-secondary bg-surface-warning-secondary p-4',
            icon && 'gap-2',
            className
        )}
    >
        {icon}

        <p className="text-sm font-medium">{content}</p>

        {onClose && (
            <Button
                className="ml-auto size-6 shrink-0"
                icon={<XIcon />}
                onClick={onClose}
                size="icon"
                title="Close"
                variant="ghost"
            />
        )}
    </div>
);
