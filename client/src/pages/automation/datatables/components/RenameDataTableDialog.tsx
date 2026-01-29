import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import useRenameDataTableDialog from '@/pages/automation/datatables/components/hooks/useRenameDataTableDialog';

const RenameDataTableDialog = () => {
    const {canRename, handleOpenChange, handleRenameSubmit, handleRenameValueChange, open, renameValue} =
        useRenameDataTableDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent onClick={(event) => event.stopPropagation()}>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Rename Table</DialogTitle>

                        <DialogDescription>Enter a new base name for this table.</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Input
                    autoFocus
                    onChange={(event) => handleRenameValueChange(event.target.value)}
                    placeholder="Enter new table name"
                    value={renameValue}
                />

                <DialogFooter>
                    <Button onClick={() => handleOpenChange(false)} variant="outline">
                        Cancel
                    </Button>

                    <Button disabled={!canRename} onClick={handleRenameSubmit}>
                        Rename
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default RenameDataTableDialog;
