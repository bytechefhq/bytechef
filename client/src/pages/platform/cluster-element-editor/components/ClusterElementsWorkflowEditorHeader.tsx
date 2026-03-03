import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PlayIcon, SparklesIcon, TextInitialIcon} from 'lucide-react';

interface ClusterElementsWorkflowEditorHeaderProps {
    copilotEnabled: boolean;
    onCopilotClick: () => void;
    onTestClick: () => void;
    onToggleEditor: (showAiAgent: boolean) => void;
    showTestButton: boolean;
    showToggleEditor: boolean;
    toggleEditorLabel?: string;
}

const ClusterElementsWorkflowEditorHeader = ({
    copilotEnabled,
    onCopilotClick,
    onTestClick,
    onToggleEditor,
    showTestButton,
    showToggleEditor,
    toggleEditorLabel = 'Switch to simple editor',
}: ClusterElementsWorkflowEditorHeaderProps) => {
    return (
        <div className="flex items-center justify-end p-4">
            <div className="flex items-center gap-1">
                {showToggleEditor && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                icon={<TextInitialIcon />}
                                onClick={() => onToggleEditor(true)}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>{toggleEditorLabel}</TooltipContent>
                    </Tooltip>
                )}

                {showTestButton && (
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
                )}

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
