import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';

import useRenameDataTableColumnDialog from '../hooks/useRenameDataTableColumnDialog';

const RenameDataTableColumnDialog = () => {
    const {canRename, currentName, handleOpenChange, handleRename, handleRenameValueChange, open, renameValue} =
        useRenameDataTableColumnDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Rename Column</DialogTitle>
                </DialogHeader>

                <div className="space-y-3 py-2">
                    <div className="space-y-1">
                        <Label>New name for "{currentName}"</Label>

                        <Input
                            autoFocus
                            onChange={(event) => handleRenameValueChange(event.target.value)}
                            placeholder="Enter new column name"
                            value={renameValue}
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button onClick={() => handleOpenChange(false)} variant="outline">
                        Cancel
                    </Button>

                    <Button disabled={!canRename} onClick={handleRename}>
                        Rename
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default RenameDataTableColumnDialog;
