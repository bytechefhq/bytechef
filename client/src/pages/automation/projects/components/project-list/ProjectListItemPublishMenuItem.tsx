import {DropdownMenuItem} from '@/components/ui/dropdown-menu';
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
                    className="dropdown-menu-item"
                    disabled={!hasWorkflows}
                    onSelect={onClick}
                >
                    <SendIcon /> Publish
                </DropdownMenuItem>
            </span>
        </TooltipTrigger>

        {!hasWorkflows && <TooltipContent>Add a workflow before publishing the project.</TooltipContent>}
    </Tooltip>
);

export default ProjectListItemPublishMenuItem;
