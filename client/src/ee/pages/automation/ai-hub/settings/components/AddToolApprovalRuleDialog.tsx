import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {AiHubToolApprovalRuleMode, AiHubToolKind} from '@/shared/middleware/graphql';
import {useState} from 'react';

export interface NewToolApprovalRuleInputI {
    mode: AiHubToolApprovalRuleMode;
    toolKind: AiHubToolKind;
    toolName: string;
}

interface AddToolApprovalRuleDialogProps {
    onOpenChange: (open: boolean) => void;
    onSubmit: (input: NewToolApprovalRuleInputI) => void;
    open: boolean;
}

/**
 * "Add rule" dialog for the Tool Approvals settings: a manually-authored rule on a catalog tool, on top of the
 * built-in defaults. Mode picks whether the rule adds a gate (Require approval) or lifts one (Exempt).
 */
const AddToolApprovalRuleDialog = ({onOpenChange, onSubmit, open}: AddToolApprovalRuleDialogProps) => {
    const [toolName, setToolName] = useState('');
    const [mode, setMode] = useState<AiHubToolApprovalRuleMode>(AiHubToolApprovalRuleMode.Require);

    const saveDisabled = !toolName.trim();

    const handleOpenChange = (nextOpen: boolean) => {
        if (!nextOpen) {
            setToolName('');
            setMode(AiHubToolApprovalRuleMode.Require);
        }

        onOpenChange(nextOpen);
    };

    const handleSave = () => {
        onSubmit({
            mode,
            toolKind: AiHubToolKind.Catalog,
            toolName: toolName.trim(),
        });
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Gate an extra catalog tool beyond the built-in defaults, or exempt one from approval."
                        title="Add rule"
                    />

                    <DialogBody>
                        <fieldset className="flex flex-col gap-4 border-0">
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="tool-approval-rule-tool-name">Tool name</Label>

                                <Input
                                    id="tool-approval-rule-tool-name"
                                    onChange={(event) => setToolName(event.target.value)}
                                    placeholder="sendEmail"
                                    value={toolName}
                                />
                            </div>

                            <div className="flex flex-col gap-2">
                                <Label htmlFor="tool-approval-rule-mode">Mode</Label>

                                <Select
                                    onValueChange={(value) => setMode(value as AiHubToolApprovalRuleMode)}
                                    value={mode}
                                >
                                    <SelectTrigger aria-label="Mode" id="tool-approval-rule-mode">
                                        <SelectValue />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value={AiHubToolApprovalRuleMode.Require}>
                                            Require approval
                                        </SelectItem>

                                        <SelectItem value={AiHubToolApprovalRuleMode.Exempt}>Exempt</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </fieldset>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button disabled={saveDisabled} label="Save" onClick={handleSave} />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AddToolApprovalRuleDialog;
