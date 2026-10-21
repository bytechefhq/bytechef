import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ListIcon} from 'lucide-react';

const ProjectDeploymentsButton = ({onClick}: {onClick: () => void}) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button
                aria-label="Project Deployments"
                className="rounded-l-none border-l-0 px-2"
                icon={<ListIcon />}
                onClick={onClick}
                size="icon"
                variant="outline"
            />
        </TooltipTrigger>

        <TooltipContent>Project Deployments</TooltipContent>
    </Tooltip>
);

export default ProjectDeploymentsButton;
