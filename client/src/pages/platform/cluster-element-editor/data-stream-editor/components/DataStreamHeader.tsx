import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {LayoutGridIcon, XIcon} from 'lucide-react';

interface DataStreamHeaderProps {
    onClose?: () => void;
    onToggleEditor?: (showDataStream: boolean) => void;
}

export default function DataStreamHeader({
    onClose,
    onToggleEditor,
}: DataStreamHeaderProps) {
    return (
        <div className="flex items-center justify-between p-4">
            <div className="text-lg font-semibold">Data Stream Editor</div>

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

                            <TooltipContent>Switch to advanced Data Stream editor - workflow canvas</TooltipContent>
                        </Tooltip>
                    </div>
                )}

                {onClose && (
                    <Button icon={<XIcon />} onClick={onClose} size="icon" title="Close the canvas" variant="ghost" />
                )}
            </div>
        </div>
    );
}
