import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {MessageCircleMoreIcon, PlayIcon} from 'lucide-react';

interface ProjectHeaderRunButtonProps {
    chatTrigger: boolean;
    onRunClick: () => void;
    runDisabled: boolean;
}

const ProjectHeaderRunButton = ({chatTrigger, onRunClick, runDisabled}: ProjectHeaderRunButtonProps) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-background/70 [&_svg]:size-5"
                    disabled={runDisabled}
                    onClick={() => onRunClick()}
                    size="icon"
                    variant="ghost"
                >
                    {chatTrigger ? <MessageCircleMoreIcon /> : <PlayIcon className="text-success" />}
                </Button>
            </TooltipTrigger>

            <TooltipContent>
                {runDisabled
                    ? `The workflow cannot be executed. Please set all required workflow input parameters, connections and component properties.`
                    : `Run the current workflow`}
            </TooltipContent>
        </Tooltip>
    );
};

export default ProjectHeaderRunButton;
