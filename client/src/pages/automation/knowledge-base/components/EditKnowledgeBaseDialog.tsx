import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useEditKnowledgeBaseDialog from '@/pages/automation/knowledge-base/components/hooks/useEditKnowledgeBaseDialog';
import {KnowledgeBase} from '@/shared/middleware/graphql';
import {EditIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface EditKnowledgeBaseDialogProps {
    knowledgeBase: KnowledgeBase;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    trigger?: ReactNode;
}

const EditKnowledgeBaseDialog = ({
    knowledgeBase,
    onOpenChange,
    open: controlledOpen,
    trigger,
}: EditKnowledgeBaseDialogProps) => {
    const {
        canSubmit,
        description,
        handleCancel,
        handleDescriptionChange,
        handleNameChange,
        handleOpenChange,
        handleSave,
        isPending,
        name,
        open,
    } = useEditKnowledgeBaseDialog({knowledgeBase, onOpenChange, open: controlledOpen});

    const renderTrigger = () => {
        if (trigger !== undefined) {
            return <DialogTrigger asChild>{trigger}</DialogTrigger>;
        }

        if (controlledOpen === undefined) {
            return (
                <DialogTrigger asChild>
                    <Button icon={<EditIcon />} size="icon" variant="ghost" />
                </DialogTrigger>
            );
        }

        return null;
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            {renderTrigger()}

            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Update the general settings for this knowledge base."
                        title={`${knowledgeBase?.id ? 'Edit' : 'Create'} Knowledge Base`}
                    />

                    <DialogBody>
                        <fieldset className="space-y-4 border-0 py-4">
                            <div className="space-y-2">
                                <Label htmlFor="kb-name">Name</Label>

                                <Input
                                    id="kb-name"
                                    onChange={(event) => handleNameChange(event.target.value)}
                                    placeholder="Knowledge base name"
                                    value={name}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="kb-description">Description</Label>

                                <Textarea
                                    id="kb-description"
                                    onChange={(event) => handleDescriptionChange(event.target.value)}
                                    placeholder="Describe this knowledge base (optional)"
                                    rows={3}
                                    value={description}
                                />
                            </div>
                        </fieldset>
                    </DialogBody>

                    <DialogFooter>
                        <Button onClick={handleCancel} variant="ghost">
                            Cancel
                        </Button>

                        <Button disabled={!canSubmit || isPending} onClick={handleSave}>
                            {isPending ? 'Saving...' : 'Save'}
                        </Button>
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default EditKnowledgeBaseDialog;
