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
                icon={<XIcon />}
                onClick={() => onClick()}
                size="iconXs"
                variant="destructiveGhost"
            />
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>Delete {propertyName}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default DeletePropertyButton;
