import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {useUpdateAutomationInputsMutation} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {toInputValueText} from '@/ee/pages/embedded/automation-hub/utils/inputValue';
import {AutomationWorkflowProjectWorkflowInput} from '@/ee/shared/middleware/embedded/public';
import {useState} from 'react';

const INPUT_TYPE_ATTRIBUTES: Record<string, string> = {
    DATE: 'date',
    DATE_TIME: 'datetime-local',
    EMAIL: 'email',
    INTEGER: 'number',
    NUMBER: 'number',
    TIME: 'time',
    URL: 'url',
};

interface AutomationInputsDialogProps {
    inputValues: Record<string, unknown>;
    inputs: AutomationWorkflowProjectWorkflowInput[];
    label: string;
    onClose: () => void;
    workflowUuid: string;
}

const AutomationInputsDialog = ({inputValues, inputs, label, onClose, workflowUuid}: AutomationInputsDialogProps) => {
    const [values, setValues] = useState<Record<string, unknown>>(inputValues);
    const [error, setError] = useState<string>();

    const {isPending, mutateAsync: updateInputs} = useUpdateAutomationInputsMutation();

    const missingRequired = inputs.some(
        (input) => input.required && toInputValueText(values[input.name ?? '']).trim() === ''
    );

    const handleSave = async () => {
        setError(undefined);

        try {
            await updateInputs({inputs: values, workflowUuid});

            onClose();
        } catch {
            setError('Your changes could not be saved. Please try again.');
        }
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="sm:max-w-md" showCloseButton>
                <DialogHeader>
                    <DialogTitle>{label} settings</DialogTitle>

                    <DialogDescription>Change the details this automation runs with.</DialogDescription>
                </DialogHeader>

                <div className="flex flex-col gap-4">
                    {inputs.map((input) => (
                        <div className="flex flex-col gap-1.5" key={input.name}>
                            <Label htmlFor={`automation-input-${input.name}`}>
                                {input.label || input.name}

                                {input.required && <span className="ml-0.5 text-destructive">*</span>}
                            </Label>

                            <Input
                                id={`automation-input-${input.name}`}
                                onChange={(event) =>
                                    setValues((current) => ({...current, [input.name ?? '']: event.target.value}))
                                }
                                type={INPUT_TYPE_ATTRIBUTES[input.type ?? ''] ?? 'text'}
                                value={toInputValueText(values[input.name ?? ''])}
                            />
                        </div>
                    ))}

                    {error && <p className="text-sm text-destructive">{error}</p>}
                </div>

                <DialogFooter>
                    <Button disabled={isPending} label="Cancel" onClick={onClose} variant="outline" />

                    <Button disabled={isPending || missingRequired} label="Save" onClick={handleSave} />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AutomationInputsDialog;
