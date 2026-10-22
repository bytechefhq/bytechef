import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {KeyboardEvent, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface WorkflowsSidebarListItemProps {
    active: boolean;
    components: ReactNode;
    editedTimeAgo?: string;
    label?: string | null;
    menu?: ReactNode;
    onClick: () => void;
}

const WorkflowsSidebarListItem = ({
    active,
    components,
    editedTimeAgo,
    label,
    menu,
    onClick,
}: WorkflowsSidebarListItemProps) => {
    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();

            onClick();
        }
    };

    return (
        <li
            className={twMerge(
                'flex w-full items-center justify-between gap-2 rounded-md border border-transparent pr-1 hover:bg-background',
                active && 'border-stroke-brand-primary bg-background'
            )}
        >
            <div
                aria-current={active || undefined}
                className="flex min-w-0 flex-1 cursor-pointer flex-col gap-3 overflow-hidden rounded-md py-3 pl-3 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                onClick={onClick}
                onKeyDown={handleKeyDown}
                role="button"
                tabIndex={0}
            >
                {components}

                <Tooltip>
                    <TooltipTrigger asChild>
                        <div className="flex flex-col gap-1 text-start">
                            <span className="truncate overflow-hidden text-sm font-medium">{label}</span>

                            {editedTimeAgo && (
                                <div className="flex gap-1 text-xs text-content-neutral-secondary">
                                    <span>Edited</span>

                                    <span>{editedTimeAgo}</span>
                                </div>
                            )}
                        </div>
                    </TooltipTrigger>

                    {label && label.length > 40 && <TooltipContent className="max-w-96">{label}</TooltipContent>}
                </Tooltip>
            </div>

            {menu}
        </li>
    );
};

export default WorkflowsSidebarListItem;
