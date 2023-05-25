import {Close} from '@radix-ui/react-dialog';
import Button from 'components/Button/Button';
import Dialog from 'components/Dialog/Dialog';
import Input from 'components/Input/Input';
import {useState} from 'react';

type EditNodeDialogProps = {
    visible?: boolean;
    onClose?: () => void;
};

const EditNodeDialog = ({onClose, visible = false}: EditNodeDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    return (
        <Dialog
            description="Use this to edit a workflow node."
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title="Edit Workflow Node"
        >
            <Input label="Name" name="node" />

            <div className="mt-8 flex justify-end space-x-1">
                <Close asChild>
                    <Button
                        displayType="lightBorder"
                        label="Cancel"
                        type="button"
                    />
                </Close>

                <Button label="Save" type="submit" />
            </div>
        </Dialog>
    );
};

export default EditNodeDialog;
