import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {LayoutGridIcon, SparklesIcon, XIcon, ZapIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface AiAgentHeaderProps {
    copilotEnabled?: boolean;
    onClose?: () => void;
    onCopilotClick?: () => void;
    onSkillsClick?: () => void;
    onToggleEditor?: (showAiAgent: boolean) => void;
    skillsPanelOpen?: boolean;
    subtitle?: string;
    title?: string;
}

export default function AiAgentHeader({
    copilotEnabled,
    onClose,
    onCopilotClick,
    onSkillsClick,
    onToggleEditor,
    skillsPanelOpen,
    subtitle,
    title = 'AI Agent Editor',
}: AiAgentHeaderProps) {
    return (
        <div className="relative z-10 flex h-[68px] shrink-0 items-center justify-between border-b border-b-border/50 px-4">
            <div>
                {title && <div className="text-lg font-semibold">{title}</div>}

                {subtitle && <p className="text-sm text-gray-500">{subtitle}</p>}
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

                {onSkillsClick && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className={twMerge('[&_svg]:size-5', skillsPanelOpen && 'bg-surface-neutral-primary')}
                                icon={<ZapIcon />}
                                onClick={onSkillsClick}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Agent Skills</TooltipContent>
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
