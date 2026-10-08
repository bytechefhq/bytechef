import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SendIcon} from 'lucide-react';

interface ProjectListItemPublishMenuItemProps {
    hasWorkflows: boolean;
    onClick: () => void;
}

const ProjectListItemPublishMenuItem = ({hasWorkflows, onClick}: ProjectListItemPublishMenuItemProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <span className="block">
                <DropdownMenuItem
                    aria-label="Publish Project"
                    disabled={!hasWorkflows}
                    icon={<SendIcon />}
                    label="Publish"
                    onSelect={onClick}
                />
            </span>
        </TooltipTrigger>

        {!hasWorkflows && <TooltipContent>Add a workflow before publishing the project.</TooltipContent>}
    </Tooltip>
);

export default ProjectListItemPublishMenuItem;
