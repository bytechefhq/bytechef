import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PlayIcon, SparklesIcon, TextInitialIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface ClusterElementsWorkflowEditorHeaderProps {
    className?: string;
    copilotEnabled: boolean;
    onCopilotClick: () => void;
    onTestClick: () => void;
    onToggleEditor: (showAiAgent: boolean) => void;
}

const ClusterElementsWorkflowEditorHeader = ({
    className,
    copilotEnabled,
    onCopilotClick,
    onTestClick,
    onToggleEditor,
}: ClusterElementsWorkflowEditorHeaderProps) => {
    return (
        <div className={twMerge('flex items-center justify-end p-4', className)}>
            <div className="flex items-center gap-1">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            icon={<TextInitialIcon />}
                            onClick={() => onToggleEditor(true)}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Switch to AI Agent editor</TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            className="hover:bg-background/70 [&_svg]:size-5"
                            icon={<PlayIcon className="text-content-brand-primary" />}
                            onClick={onTestClick}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Test agent</TooltipContent>
                </Tooltip>

                {copilotEnabled && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className="[&_svg]:size-5"
                                icon={<SparklesIcon />}
                                onClick={onCopilotClick}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Open Copilot panel</TooltipContent>
                    </Tooltip>
                )}
            </div>
        </div>
    );
};

export default ClusterElementsWorkflowEditorHeader;
