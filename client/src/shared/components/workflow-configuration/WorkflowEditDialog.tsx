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
import {FormEvent, ReactNode} from 'react';

interface WorkflowEditDialogProps {
    children: ReactNode;
    description?: string;
    footerStartContent?: ReactNode;
    onClose: () => void;
    onSave: () => void;
    open?: boolean;
    outsideContent?: ReactNode;
    saveDisabled?: boolean;
    submitOnEnter?: boolean;
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
    submitOnEnter = false,
    workflowLabel,
}: WorkflowEditDialogProps) => {
    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!saveDisabled) {
            onSave();
        }
    };

    const dialogContent = (
        <>
            <DialogHeader description={description} title={`Edit ${workflowLabel || ''} Workflow`} />

            <DialogBody>
                <div className="max-h-dialog-height overflow-y-auto">{children}</div>
            </DialogBody>

            <DialogFooter>
                {footerStartContent}

                <DialogCancelButton />

                <Button
                    disabled={saveDisabled}
                    label="Save"
                    onClick={submitOnEnter ? undefined : onSave}
                    type={submitOnEnter ? 'submit' : 'button'}
                />
            </DialogFooter>
        </>
    );

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent className="gap-0 p-0" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    {submitOnEnter ? (
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
                            {dialogContent}
                        </form>
                    ) : (
                        dialogContent
                    )}
                </DialogMain>

                {outsideContent}
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowEditDialog;
