import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useCreateToolSimulationDialog, {
    type ToolSimulationEditDataI,
} from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/hooks/useCreateToolSimulationDialog';
import {InfoIcon, Loader2Icon} from 'lucide-react';

interface CreateToolSimulationDialogProps {
    editData?: ToolSimulationEditDataI;
    onClose: () => void;
    onCreate: (toolName: string, responsePrompt: string, simulationModel?: string) => Promise<void>;
    onUpdate?: (id: string, toolName?: string, responsePrompt?: string, simulationModel?: string) => Promise<void>;
}

const CreateToolSimulationDialog = ({editData, onClose, onCreate, onUpdate}: CreateToolSimulationDialogProps) => {
    const {
        handleSubmit,
        isEditing,
        responsePrompt,
        setResponsePrompt,
        setSimulationModel,
        setToolName,
        simulationModel,
        submitting,
        toolName,
    } = useCreateToolSimulationDialog({editData, onClose, onCreate, onUpdate});

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
            <DialogContent className="max-w-lg">
                <DialogHeader className="flex flex-row items-center justify-between">
                    <DialogTitle>{isEditing ? 'Edit Tool Simulation' : 'Add Tool Simulation'}</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <fieldset className="flex flex-col gap-4 border-0 p-0">
                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="tool-name">Tool Name</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    The exact name of the tool to intercept. When the agent calls this tool during
                                    evaluation, the simulated response is returned instead of executing the real tool.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Input
                            id="tool-name"
                            onChange={(event) => setToolName(event.target.value)}
                            placeholder="e.g. searchOrders, sendEmail"
                            value={toolName}
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="response-prompt">Response Prompt</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    If no simulation model is set, this text is returned verbatim as the tool result. If
                                    a simulation model is set, this is used as instructions for the LLM to generate a
                                    realistic response based on the tool call input.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Textarea
                            id="response-prompt"
                            onChange={(event) => setResponsePrompt(event.target.value)}
                            placeholder="Enter the simulated response or instructions for the LLM"
                            rows={4}
                            value={responsePrompt}
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <div className="flex items-center gap-1">
                            <Label htmlFor="simulation-model">Simulation Model (optional)</Label>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-3.5 text-muted-foreground" />
                                </TooltipTrigger>

                                <TooltipContent className="max-w-64" side="right">
                                    When set, an LLM generates the simulated response using the response prompt as
                                    instructions. Leave empty to return the response prompt text verbatim.
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <Input
                            id="simulation-model"
                            onChange={(event) => setSimulationModel(event.target.value)}
                            placeholder="e.g. gpt-4o, claude-sonnet-4-5-20250514"
                            value={simulationModel}
                        />
                    </div>
                </fieldset>

                <DialogFooter>
                    <Button disabled={submitting} label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!toolName.trim() || !responsePrompt.trim() || submitting}
                        icon={submitting ? <Loader2Icon className="animate-spin" /> : undefined}
                        label={submitting ? 'Saving...' : isEditing ? 'Save' : 'Add'}
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default CreateToolSimulationDialog;
