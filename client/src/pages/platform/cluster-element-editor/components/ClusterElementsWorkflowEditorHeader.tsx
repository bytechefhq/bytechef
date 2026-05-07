import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ExternalLinkIcon, FlaskConicalIcon, PlayIcon, SparklesIcon, TextInitialIcon, ZapIcon} from 'lucide-react';

interface ClusterElementsWorkflowEditorHeaderProps {
    copilotEnabled: boolean;
    onCopilotClick: () => void;
    onEvalsClick?: () => void;
    onTestClick: () => void;
    onToggleEditor: (showAiAgent: boolean) => void;
    showSkills?: boolean;
    showTestButton: boolean;
    showToggleEditor: boolean;
    toggleEditorLabel?: string;
}

const ClusterElementsWorkflowEditorHeader = ({
    copilotEnabled,
    onCopilotClick,
    onEvalsClick,
    onTestClick,
    onToggleEditor,
    showSkills,
    showTestButton,
    showToggleEditor,
    toggleEditorLabel = 'Switch to simple editor',
}: ClusterElementsWorkflowEditorHeaderProps) => {
    return (
        <div className="relative z-10 flex items-center justify-end px-4 py-6">
            <div className="flex items-center gap-1 rounded-lg bg-white/70 backdrop-blur-sm">
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

                {showSkills && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <a
                                className="relative inline-flex size-9 items-center justify-center rounded-md text-content-neutral-secondary hover:bg-surface-neutral-primary"
                                href="/automation/ai/skills"
                                rel="noreferrer"
                                target="_blank"
                            >
                                <ZapIcon className="size-5" />

                                <ExternalLinkIcon
                                    aria-hidden
                                    className="absolute right-0.5 top-0.5 size-2.5 text-content-neutral-secondary/70"
                                />
                            </a>
                        </TooltipTrigger>

                        <TooltipContent>Manage AI Skills</TooltipContent>
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
