import Button from '@/components/Button/Button';
import Dialog from '@/components/Dialog/Dialog';
import {WorkflowModel} from '@/middleware/helios/configuration';
import {Close} from '@radix-ui/react-dialog';
import {useState} from 'react';

import {InstanceDialogWorkflowListItem} from './InstanceDialogWorkflowsStep';

interface ProjectInstanceEditWorkflowDialogProps {
    workflow: WorkflowModel;
    showTrigger?: boolean;
    visible?: boolean;
    onClose?: () => void;
}

export const ProjectInstanceEditWorkflowDialog = ({
    onClose,
    visible = false,
    workflow,
}: ProjectInstanceEditWorkflowDialogProps) => {
    const [isOpen, setIsOpen] = useState(visible);

    function closeDialog() {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    return (
        <Dialog
            isOpen={isOpen}
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            title={`${
                workflow?.id ? 'Edit' : 'Create'
            } ${workflow?.label} Workflow`}
        >
            <div className="flex flex-col">
                <div className="mt-4 flex flex-col ">
                    <InstanceDialogWorkflowListItem
                        key={workflow.id!}
                        workflow={workflow}
                        label="Enable"
                    />

                    <div className="mt-4 flex w-full justify-end space-x-2 self-end">
                        <Close asChild>
                            <Button displayType="lightBorder" label="Cancel" />
                        </Close>

                        <Button
                            label="Save"
                            onClick={() => console.log('TODO')}
                        />
                    </div>
                </div>
            </div>
        </Dialog>
    );
};
