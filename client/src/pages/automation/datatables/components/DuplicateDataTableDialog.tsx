import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
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
                <DialogMain>
                    <DialogHeader description="Enter a name for the duplicated table." title="Duplicate table" />

                    <DialogBody>
                        <Input
                            autoFocus
                            onChange={(event) => handleDuplicateValueChange(event.target.value)}
                            value={duplicateValue}
                        />
                    </DialogBody>

                    <DialogFooter>
                        <Button className="shadow-none" label="Cancel" onClick={handleClose} variant="outline" />

                        <Button
                            className="shadow-none"
                            disabled={!canDuplicate}
                            label="Duplicate"
                            onClick={handleDuplicateSubmit}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default DuplicateDataTableDialog;
