import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ListIcon} from 'lucide-react';
import {MouseEvent} from 'react';

interface ProjectDeploymentsButtonProps {
    onClick: (event: MouseEvent<HTMLButtonElement>) => void;
    size?: 'icon' | 'iconSm';
}

const ProjectDeploymentsButton = ({onClick, size = 'icon'}: ProjectDeploymentsButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Button
                aria-label="Project Deployments"
                className="rounded-l-none border-l-0 px-2"
                icon={<ListIcon />}
                onClick={onClick}
                size={size}
                variant="outline"
            />
        </TooltipTrigger>

        <TooltipContent>Project Deployments</TooltipContent>
    </Tooltip>
);

export default ProjectDeploymentsButton;
