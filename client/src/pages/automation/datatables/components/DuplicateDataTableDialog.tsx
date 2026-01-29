import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import useDuplicateDataTableDialog from '@/pages/automation/datatables/components/hooks/useDuplicateDataTableDialog';

const DuplicateDataTableDialog = () => {
    const {
        canDuplicate,
        duplicateValue,
        handleClose,
        handleDuplicateSubmit,
        handleDuplicateValueChange,
        handleOpenChange,
        open,
    } = useDuplicateDataTableDialog();

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent onClick={(event) => event.stopPropagation()}>
                <DialogHeader>
                    <DialogTitle>Duplicate table</DialogTitle>

                    <DialogDescription>Enter a name for the duplicated table.</DialogDescription>
                </DialogHeader>

                <Input
                    autoFocus
                    className="my-2"
                    onChange={(event) => handleDuplicateValueChange(event.target.value)}
                    value={duplicateValue}
                />

                <DialogFooter>
                    <Button className="shadow-none" onClick={handleClose} variant="outline">
                        Cancel
                    </Button>

                    <Button className="shadow-none" disabled={!canDuplicate} onClick={handleDuplicateSubmit}>
                        Duplicate
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default DuplicateDataTableDialog;
