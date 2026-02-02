import Button from '@/components/Button/Button';
import {DialogClose} from '@/components/ui/dialog';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {usePropertyCodeEditorDialogToolbar} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/hooks';
import {Loader2Icon, PlayIcon, SaveIcon, SparklesIcon, SquareIcon, XIcon} from 'lucide-react';

interface PropertyCodeEditorDialogToolbarProps {
    language: string;
    onChange: (value: string | undefined) => void;
    workflowId: string;
    workflowNodeName: string;
}

const PropertyCodeEditorDialogToolbar = ({
    language,
    onChange,
    workflowId,
    workflowNodeName,
}: PropertyCodeEditorDialogToolbarProps) => {
    const {
        copilotEnabled,
        dirty,
        handleCopilotClick,
        handleRunClick,
        handleSaveClick,
        handleStopClick,
        saving,
        scriptIsRunning,
    } = usePropertyCodeEditorDialogToolbar({
        language,
        onChange,
        workflowId,
        workflowNodeName,
    });

    return (
        <div className="flex flex-row items-center justify-between space-y-0 border-b border-b-border/50 p-3">
            <span className="text-lg font-semibold">Edit Script</span>

            <div className="flex items-center gap-1">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            disabled={!dirty || saving}
                            icon={saving ? <Loader2Icon className="animate-spin" /> : <SaveIcon />}
                            onClick={handleSaveClick}
                            size="icon"
                            type="submit"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>{saving ? 'Saving...' : 'Save current workflow'}</TooltipContent>
                </Tooltip>

                {!scriptIsRunning && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span tabIndex={0}>
                                <Button
                                    disabled={dirty}
                                    icon={<PlayIcon className="text-success" />}
                                    onClick={handleRunClick}
                                    size="icon"
                                    variant="ghost"
                                />
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Run the current workflow</TooltipContent>
                    </Tooltip>
                )}

                {scriptIsRunning && (
                    <Button icon={<SquareIcon />} onClick={handleStopClick} size="icon" variant="destructive" />
                )}

                {copilotEnabled && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className="[&_svg]:size-5"
                                icon={<SparklesIcon />}
                                onClick={handleCopilotClick}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Open Copilot panel</TooltipContent>
                    </Tooltip>
                )}

                <DialogClose asChild>
                    <Button icon={<XIcon />} size="icon" title="Close" variant="ghost" />
                </DialogClose>
            </div>
        </div>
    );
};

export default PropertyCodeEditorDialogToolbar;
