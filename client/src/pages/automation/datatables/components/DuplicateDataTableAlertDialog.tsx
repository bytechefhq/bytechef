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
import useDuplicateDataTableAlertDialog from '@/pages/automation/datatables/components/hooks/useDuplicateDataTableAlertDialog';

const DuplicateDataTableAlertDialog = () => {
    const {
        canDuplicate,
        duplicateValue,
        handleClose,
        handleDuplicateSubmit,
        handleDuplicateValueChange,
        handleOpenChange,
        open,
    } = useDuplicateDataTableAlertDialog();

    return (
        <AlertDialog onOpenChange={handleOpenChange} open={open}>
            <AlertDialogContent onClick={(event) => event.stopPropagation()}>
                <AlertDialogHeader>
                    <AlertDialogTitle>Duplicate table</AlertDialogTitle>

                    <AlertDialogDescription>Enter a name for the duplicated table.</AlertDialogDescription>
                </AlertDialogHeader>

                <Input
                    autoFocus
                    className="my-2"
                    onChange={(event) => handleDuplicateValueChange(event.target.value)}
                    value={duplicateValue}
                />

                <AlertDialogFooter>
                    <AlertDialogCancel className="shadow-none" onClick={handleClose}>
                        Cancel
                    </AlertDialogCancel>

                    <AlertDialogAction className="shadow-none" disabled={!canDuplicate} onClick={handleDuplicateSubmit}>
                        Duplicate
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default DuplicateDataTableAlertDialog;
