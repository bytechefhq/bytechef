import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {FlaskConicalIcon, PlayIcon, SparklesIcon, TextInitialIcon, ZapIcon} from 'lucide-react';

interface ClusterElementsWorkflowEditorHeaderProps {
    copilotEnabled: boolean;
    onCopilotClick: () => void;
    onEvalsClick?: () => void;
    onSkillsClick?: () => void;
    onTestClick: () => void;
    onToggleEditor: (showAiAgent: boolean) => void;
    showTestButton: boolean;
    showToggleEditor: boolean;
    toggleEditorLabel?: string;
}

const ClusterElementsWorkflowEditorHeader = ({
    copilotEnabled,
    onCopilotClick,
    onEvalsClick,
    onSkillsClick,
    onTestClick,
    onToggleEditor,
    showTestButton,
    showToggleEditor,
    toggleEditorLabel = 'Switch to simple editor',
}: ClusterElementsWorkflowEditorHeaderProps) => {
    return (
        <div className="relative z-10 flex items-center justify-end px-4 py-5">
            <div className="flex items-center gap-1 rounded-lg bg-white/70 p-1 backdrop-blur-sm">
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

                {onSkillsClick && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className="[&_svg]:size-5"
                                icon={<ZapIcon />}
                                onClick={onSkillsClick}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Agent Skills</TooltipContent>
                    </Tooltip>
                )}

                {onEvalsClick && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className="[&_svg]:size-5"
                                icon={<FlaskConicalIcon />}
                                onClick={onEvalsClick}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Agent Evals</TooltipContent>
                    </Tooltip>
                )}

                {showTestButton && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className="[&_svg]:size-5"
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
