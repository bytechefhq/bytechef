import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {LayoutGridIcon, SparklesIcon, XIcon} from 'lucide-react';

interface AiAgentHeaderProps {
    copilotEnabled?: boolean;
    onClose?: () => void;
    onCopilotClick?: () => void;
    onToggleEditor?: (showAiAgent: boolean) => void;
}

export default function AiAgentHeader({copilotEnabled, onClose, onCopilotClick, onToggleEditor}: AiAgentHeaderProps) {
    return (
        <div className="flex items-center justify-between p-4">
            <div className="text-lg font-semibold">AI Agent Editor</div>

            <div className="flex items-center gap-1">
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
