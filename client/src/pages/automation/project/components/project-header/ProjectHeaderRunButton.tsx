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
                    className="w-20 bg-surface-brand-primary shadow-none hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
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
