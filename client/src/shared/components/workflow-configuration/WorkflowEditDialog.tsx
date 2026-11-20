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
import {ReactNode} from 'react';

interface WorkflowEditDialogProps {
    children: ReactNode;
    description?: string;
    footerStartContent?: ReactNode;
    onClose: () => void;
    onSave: () => void;
    open?: boolean;
    outsideContent?: ReactNode;
    saveDisabled?: boolean;
    workflowLabel?: string;
}

const WorkflowEditDialog = ({
    children,
    description,
    footerStartContent,
    onClose,
    onSave,
    open = true,
    outsideContent,
    saveDisabled = false,
    workflowLabel,
}: WorkflowEditDialogProps) => (
    <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
        <DialogContent className="gap-0 p-0" onInteractOutside={(event) => event.preventDefault()}>
            <DialogMain>
                <DialogHeader description={description} title={`Edit ${workflowLabel || ''} Workflow`} />

                <DialogBody>
                    <div className="max-h-dialog-height overflow-y-auto">{children}</div>
                </DialogBody>

                <DialogFooter>
                    {footerStartContent}

                    <DialogCancelButton />

                    <Button disabled={saveDisabled} label="Save" onClick={onSave} type="button" />
                </DialogFooter>
            </DialogMain>

            {outsideContent}
        </DialogContent>
    </Dialog>
);

export default WorkflowEditDialog;
