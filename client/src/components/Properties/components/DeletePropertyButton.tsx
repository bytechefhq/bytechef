import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {XIcon} from 'lucide-react';

interface DeletePropertyButtonProps {
    handleDeletePropertyClick: (subPropertyName: string, propertyName: string) => void;
    propertyName: string;
    subPropertyName: string;
}

const DeletePropertyButton = ({
    handleDeletePropertyClick,
    propertyName,
    subPropertyName,
}: DeletePropertyButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button
                className="ml-1 self-center"
                onClick={() => handleDeletePropertyClick(subPropertyName!, propertyName!)}
                size="icon"
                variant="ghost"
            >
                <XIcon className="size-8 cursor-pointer p-2 hover:text-red-500" />
            </Button>
        </TooltipTrigger>

        <TooltipContent>Delete property</TooltipContent>
    </Tooltip>
);

export default DeletePropertyButton;
