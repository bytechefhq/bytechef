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

import useRenameDataTableColumnDialog from '../hooks/useRenameDataTableColumnDialog';

const RenameDataTableColumnDialog = () => {
    const {canRename, currentName, handleOpenChange, handleRenameSubmit, handleRenameValueChange, open, renameValue} =
        useRenameDataTableColumnDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader description="Enter a new name for this column." title="Rename Column" />

                    <DialogBody>
                        <div className="flex flex-col gap-1.5">
                            <Label>New name for "{currentName}"</Label>

                            <Input
                                autoFocus
                                onChange={(event) => handleRenameValueChange(event.target.value)}
                                placeholder="Enter new column name"
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

export default RenameDataTableColumnDialog;
