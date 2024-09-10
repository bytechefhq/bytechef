import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PlayIcon} from 'lucide-react';

const ProjectHeaderRunButton = ({onRunClick, runDisabled}: {onRunClick: () => void; runDisabled: boolean}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-gray-200"
                    disabled={runDisabled}
                    onClick={() => onRunClick()}
                    size="icon"
                    variant="ghost"
                >
                    <PlayIcon className="h-5 text-success" />
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
