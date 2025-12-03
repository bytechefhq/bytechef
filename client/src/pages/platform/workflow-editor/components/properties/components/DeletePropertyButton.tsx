import Button from '@/components/Button/Button';
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
            <Button icon={<XIcon />} onClick={() => onClick()} size="iconXs" variant="destructiveGhost" />
        </TooltipTrigger>

        <TooltipPortal>
            <TooltipContent>Delete {propertyName}</TooltipContent>
        </TooltipPortal>
    </Tooltip>
);

export default DeletePropertyButton;
