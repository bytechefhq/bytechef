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
import {Input} from '@/components/Input/Input';
import {Label} from '@/components/ui/label';
import useRenameDataTableDialog from '@/pages/automation/datatables/components/hooks/useRenameDataTableDialog';

const RenameDataTableDialog = () => {
    const {canRename, handleOpenChange, handleRenameSubmit, handleRenameValueChange, open, renameValue} =
        useRenameDataTableDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent onClick={(event) => event.stopPropagation()}>
                <DialogMain>
                    <DialogHeader description="Enter a new base name for this table." title="Rename Table" />

                    <DialogBody>
                        <div className="flex flex-col gap-1.5">
                            <Label>New name</Label>

                            <Input
                                autoFocus
                                onChange={(event) => handleRenameValueChange(event.target.value)}
                                placeholder="Enter new table name"
                                value={renameValue}
                            />
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button disabled={!canRename} label="Rename" onClick={handleRenameSubmit} />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default RenameDataTableDialog;
