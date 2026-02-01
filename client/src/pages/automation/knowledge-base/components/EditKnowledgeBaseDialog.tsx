import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
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

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            {trigger !== undefined ? (
                <DialogTrigger asChild>{trigger}</DialogTrigger>
            ) : controlledOpen === undefined ? (
                <DialogTrigger asChild>
                    <Button icon={<EditIcon />} size="icon" variant="ghost" />
                </DialogTrigger>
            ) : null}

            <DialogContent className="sm:max-w-[500px]">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{`${knowledgeBase?.id ? 'Edit' : 'Create'} Knowledge Base`}</DialogTitle>

                        <DialogDescription>Update the general settings for this knowledge base.</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4 py-4">
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
                </div>

                <DialogFooter>
                    <Button onClick={handleCancel} variant="ghost">
                        Cancel
                    </Button>

                    <Button disabled={!canSubmit || isPending} onClick={handleSave}>
                        {isPending ? 'Saving...' : 'Save'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default EditKnowledgeBaseDialog;
