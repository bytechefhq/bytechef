import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {MessageCircleMoreIcon, PlayIcon, SquareIcon} from 'lucide-react';

interface ProjectHeaderWorkflowActionsButtonProps {
    chatTrigger: boolean;
    onRunClick: () => void;
    onStopClick: () => void;
    runDisabled: boolean;
    workflowIsRunning: boolean;
}

const ProjectHeaderWorkflowActionsButton = ({
    chatTrigger,
    onRunClick,
    onStopClick,
    runDisabled,
    workflowIsRunning,
}: ProjectHeaderWorkflowActionsButtonProps) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <div className="w-20">
                    {workflowIsRunning ? (
                        <Button className="w-full shadow-none" onClick={onStopClick} variant="destructive">
                            <SquareIcon /> Stop
                        </Button>
                    ) : (
                        <Button
                            className="w-full bg-surface-brand-primary shadow-none hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed disabled:pointer-events-auto"
                            disabled={runDisabled}
                            onClick={() => onRunClick()}
                        >
                            {chatTrigger ? (
                                <>
                                    <MessageCircleMoreIcon /> Chat
                                </>
                            ) : (
                                <>
                                    <PlayIcon /> Run
                                </>
                            )}
                        </Button>
                    )}
                </div>
            </TooltipTrigger>

            {workflowIsRunning && <TooltipContent>Stop the current workflow</TooltipContent>}

            {!workflowIsRunning && (
                <TooltipContent className="max-w-sm px-2">
                    {runDisabled
                        ? `The workflow cannot be executed. Please set all required workflow input parameters, connections and
                    component properties.`
                        : `Run the current workflow`}
                </TooltipContent>
            )}
        </Tooltip>
    );
};

export default ProjectHeaderWorkflowActionsButton;
