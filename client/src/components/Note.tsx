import {XIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

import {Button} from './ui/button';

export const Note = ({content, icon}: {content: string; icon?: ReactNode}) => (
    <div className={twMerge('relative flex items-center rounded-md bg-surface-warning-secondary p-4', icon && 'gap-2')}>
        {icon && icon}

        <p className="text-sm font-medium">{content}</p>

        <Button className="group absolute right-0 top-0 hover:bg-transparent" size="icon" variant="ghost">
            <XIcon className="size-4 text-content-neutral-secondary group-hover:text-content-destructive" />
        </Button>
    </div>
);
