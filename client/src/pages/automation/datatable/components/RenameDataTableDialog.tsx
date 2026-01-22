import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Input} from '@/components/ui/input';

import useRenameDataTableDialog from '../hooks/useRenameDataTableDialog';

const RenameDataTableDialog = () => {
    const {canRename, handleOpenChange, handleRename, handleRenameValueChange, open, renameValue} =
        useRenameDataTableDialog();

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Rename Table</AlertDialogTitle>
                </AlertDialogHeader>

                <div className="py-2">
                    <Input
                        autoFocus
                        onChange={(event) => handleRenameValueChange(event.target.value)}
                        value={renameValue}
                    />
                </div>

                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>

                    <AlertDialogAction disabled={!canRename} onClick={handleRename}>
                        Rename
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default RenameDataTableDialog;
