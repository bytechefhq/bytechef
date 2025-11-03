import Button from '@/components/Button/Button';
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
            <div className="mx-2 w-20">
                {workflowIsRunning ? (
                    <Button
                        className="w-full"
                        icon={<SquareIcon />}
                        label="Stop"
                        onClick={onStopClick}
                        variant="destructive"
                    />
                ) : (
                    <Button
                        className="w-full"
                        disabled={runDisabled}
                        icon={chatTrigger ? <MessageCircleMoreIcon /> : <PlayIcon />}
                        label={chatTrigger ? 'Chat' : 'Test'}
                        onClick={() => onRunClick()}
                    />
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
