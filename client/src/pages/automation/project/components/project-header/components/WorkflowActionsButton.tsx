import Button from '@/components/Button/Button';
import {ButtonGroup, ButtonGroupSeparator} from '@/components/ui/button-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {MessageCircleMoreIcon, PlayIcon, SquareIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface ProjectHeaderWorkflowActionsButtonProps {
    chatTrigger: boolean;
    leadingAction?: ReactNode;
    onRunClick: () => void;
    onStopClick: () => void;
    runDisabled: boolean;
    workflowIsRunning: boolean;
}

const WorkflowActionsButton = ({
    chatTrigger,
    leadingAction,
    onRunClick,
    onStopClick,
    runDisabled,
    workflowIsRunning,
}: ProjectHeaderWorkflowActionsButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <div className="mx-2 flex items-center">
                {/* A code workflow's Save joins this group, mirroring the workflow JSON editor's save+run pair. */}

                {leadingAction && !workflowIsRunning ? (
                    <ButtonGroup>
                        {leadingAction}

                        <ButtonGroupSeparator className="bg-stroke-brand-secondary" />

                        <Button
                            className="w-20 rounded-l-none"
                            disabled={runDisabled}
                            icon={chatTrigger ? <MessageCircleMoreIcon /> : <PlayIcon />}
                            label={chatTrigger ? 'Chat' : 'Test'}
                            onClick={() => onRunClick()}
                        />
                    </ButtonGroup>
                ) : (
                    <div className="w-20">
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
