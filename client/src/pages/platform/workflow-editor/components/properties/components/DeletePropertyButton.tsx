import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipPortal, TooltipTrigger} from '@/components/ui/tooltip';
import {XIcon} from 'lucide-react';

interface DeletePropertyButtonProps {
    onClick: () => void;
    propertyName: string;
}

const DeletePropertyButton = ({onClick, propertyName}: DeletePropertyButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button
                aria-label={`Delete ${propertyName}`}
                className="ml-1 size-3.5 rounded-[4px] border border-stroke-destructive-primary p-0"
                icon={<XIcon className="size-3" />}
                onClick={() => onClick()}
                size="iconXxs"
                variant="destructiveGhost"
            />
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>Delete {propertyName}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default DeletePropertyButton;
