import Button from '@/components/Button/Button';
import {XIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface NoteProps {
    className?: string;
    content: string;
    icon?: ReactNode;
    onDismiss?: () => void;
}

export const Note = ({className, content, icon, onDismiss}: NoteProps) => (
    <div
        className={twMerge(
            'relative flex items-center rounded-md border border-stroke-warning-secondary bg-surface-warning-secondary p-4',
            icon && 'gap-2',
            className
        )}
    >
        {icon && icon}

        <p className="text-sm font-medium">{content}</p>

        {onDismiss && (
            <Button
                aria-label="Dismiss"
                className="ml-auto shrink-0"
                icon={<XIcon />}
                onClick={onDismiss}
                size="iconSm"
                variant="ghost"
            />
        )}
    </div>
);
