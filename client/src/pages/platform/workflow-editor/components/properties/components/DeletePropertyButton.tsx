import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {XIcon} from 'lucide-react';

interface DeletePropertyButtonProps {
    onClick: () => void;
    propertyName: string;
}

const DeletePropertyButton = ({onClick, propertyName}: DeletePropertyButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button className="size-auto p-0.5" onClick={() => onClick()} size="icon" variant="ghost">
                <XIcon className="size-4 cursor-pointer text-destructive" />
            </Button>
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>Delete {propertyName}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default DeletePropertyButton;
