import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SendIcon} from 'lucide-react';

interface IntegrationListItemPublishMenuItemProps {
    canPublish: boolean;
    onClick: () => void;
}

const IntegrationListItemPublishMenuItem = ({canPublish, onClick}: IntegrationListItemPublishMenuItemProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <span className="block">
                <DropdownMenuItem disabled={!canPublish} icon={<SendIcon />} label="Publish" onSelect={onClick} />
            </span>
        </TooltipTrigger>

        {!canPublish && <TooltipContent>Add a workflow before publishing the integration.</TooltipContent>}
    </Tooltip>
);

export default IntegrationListItemPublishMenuItem;
