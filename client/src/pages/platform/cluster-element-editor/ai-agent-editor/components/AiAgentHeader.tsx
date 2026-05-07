import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ExternalLinkIcon, FlaskConicalIcon, LayoutGridIcon, SparklesIcon, XIcon, ZapIcon} from 'lucide-react';

interface AiAgentHeaderProps {
    copilotEnabled?: boolean;
    onClose?: () => void;
    onCopilotClick?: () => void;
    onEvalsClick?: () => void;
    onToggleEditor?: (showAiAgent: boolean) => void;
    showSkills?: boolean;
    subtitle?: string;
    title?: string;
}

export default function AiAgentHeader({
    copilotEnabled,
    onClose,
    onCopilotClick,
    onEvalsClick,
    onToggleEditor,
    showSkills,
    subtitle,
    title = 'AI Agent Editor',
}: AiAgentHeaderProps) {
    return (
        <div className="relative z-10 flex h-[68px] shrink-0 items-center justify-between border-b border-b-border/50 px-4">
            <div>
                {title && <div className="text-lg font-semibold">{title}</div>}

                {subtitle && <p className="text-sm text-content-neutral-secondary">{subtitle}</p>}
            </div>

            <div className="flex shrink-0 items-center gap-1">
                {onToggleEditor && (
                    <div className="flex items-center gap-1 rounded-md p-0.5">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    className="bg-surface-neutral-primary"
                                    icon={<LayoutGridIcon />}
                                    label="Advanced"
                                    onClick={() => onToggleEditor(false)}
                                    size="default"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Switch to advanced AI Agent editor - workflow canvas</TooltipContent>
                        </Tooltip>
                    </div>
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

                {copilotEnabled && onCopilotClick && (
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

                {onClose && (
                    <Button icon={<XIcon />} onClick={onClose} size="icon" title="Close the canvas" variant="ghost" />
                )}
            </div>
        </div>
    );
}
