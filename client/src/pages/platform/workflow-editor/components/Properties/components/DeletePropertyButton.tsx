import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentType} from '@/types/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface DeletePropertyButtonProps {
    className?: string;
    currentComponent: ComponentType;
    onClick: () => void;
    propertyName: string;
    subPropertyIndex?: number;
    subPropertyName?: string;
}

const DeletePropertyButton = ({className, onClick}: DeletePropertyButtonProps) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <div className={twMerge('group flex items-center justify-center', className)}>
                    <button className="p-1" onClick={() => onClick()}>
                        <XIcon className="size-4 cursor-pointer group-hover:text-red-500" />
                    </button>
                </div>
            </TooltipTrigger>

            <TooltipPortal>
                <TooltipContent>Delete</TooltipContent>
            </TooltipPortal>
        </Tooltip>
    );
};

export default DeletePropertyButton;
