import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {MouseEvent, ReactNode} from 'react';
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
    const handleCardClick = (event: MouseEvent<HTMLLIElement>) => {
        if (!event.currentTarget.contains(event.target as Node)) {
            return;
        }

        onClick();
    };

    return (
        <li
            className={twMerge(
                'w-full cursor-pointer rounded-md border border-transparent py-3 pr-1 pl-3 hover:bg-background',
                active && 'border-stroke-brand-primary bg-background'
            )}
            onClick={handleCardClick}
        >
            <div className="flex items-center justify-between gap-2">
                <div className="flex min-w-0 flex-1 flex-col gap-3 overflow-hidden">
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
            </div>
        </li>
    );
};

export default WorkflowsSidebarListItem;
