import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Input} from '@/components/ui/input';
import useRenameDataTableAlertDialog from '@/pages/automation/datatables/components/hooks/useRenameDataTableAlertDialog';

const RenameDataTableAlertDialog = () => {
    const {canRename, handleClose, handleOpenChange, handleRenameSubmit, handleRenameValueChange, open, renameValue} =
        useRenameDataTableAlertDialog();

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={open}>
            <AlertDialogContent onClick={(event) => event.stopPropagation()}>
                <AlertDialogHeader>
                    <AlertDialogTitle>Rename Table</AlertDialogTitle>

                    <AlertDialogDescription>Enter a new name for this data table.</AlertDialogDescription>
                </AlertDialogHeader>

                <div className="py-2">
                    <Input
                        autoFocus
                        onChange={(event) => handleRenameValueChange(event.target.value)}
                        value={renameValue}
                    />
                </div>

                <AlertDialogFooter>
                    <AlertDialogCancel className="shadow-none" onClick={handleClose}>
                        Cancel
                    </AlertDialogCancel>

                    <AlertDialogAction className="shadow-none" disabled={!canRename} onClick={handleRenameSubmit}>
                        Rename
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default RenameDataTableAlertDialog;
