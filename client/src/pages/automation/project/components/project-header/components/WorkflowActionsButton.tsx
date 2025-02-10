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

const WorkflowActionsButton = ({
    chatTrigger,
    onRunClick,
    onStopClick,
    runDisabled,
    workflowIsRunning,
}: ProjectHeaderWorkflowActionsButtonProps) => (
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

        <TooltipContent className="mr-2 max-w-xs px-2">
            {!runDisabled ? (
                <>
                    {workflowIsRunning && 'Stop the current workflow'}

                    {!workflowIsRunning && chatTrigger && 'Start the chat'}

                    {!workflowIsRunning && !chatTrigger && 'Run the current workflow'}
                </>
            ) : (
                'The workflow cannot be executed. Please set all required workflow input parameters, connections and component properties.'
            )}
        </TooltipContent>
    </Tooltip>
);

export default WorkflowActionsButton;
