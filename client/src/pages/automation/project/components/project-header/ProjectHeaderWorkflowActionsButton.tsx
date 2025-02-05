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

            {runDisabled ? (
                <TooltipContent className="mr-2 max-w-xs px-2">
                    The workflow cannot be executed. Please set all required workflow input parameters, connections and
                    component properties.
                </TooltipContent>
            ) : (
                <TooltipContent>
                    {workflowIsRunning ? (
                        'Stop the current workflow'
                    ) : (
                        <>
                            {chatTrigger && `Start the chat`}

                            {!chatTrigger && `Run the current workflow`}
                        </>
                    )}
                </TooltipContent>
            )}
        </Tooltip>
    );
};

export default ProjectHeaderWorkflowActionsButton;
