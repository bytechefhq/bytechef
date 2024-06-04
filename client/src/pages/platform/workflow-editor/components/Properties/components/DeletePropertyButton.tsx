import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface DeletePropertyButtonProps {
    className?: string;
    onClick: () => void;
    propertyName: string;
}

const DeletePropertyButton = ({className, onClick, propertyName}: DeletePropertyButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <div className={twMerge('group flex items-center justify-center', className)}>
                <button className="p-1" onClick={() => onClick()}>
                    <XIcon className="size-4 cursor-pointer group-hover:text-red-500" />
                </button>
            </div>
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>Delete {propertyName}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default DeletePropertyButton;
