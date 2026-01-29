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
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useKnowledgeBaseDocumentChunkEditDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkEditDialog';

interface KnowledgeBaseDocumentChunkEditDialogProps {
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentChunkEditDialog = ({knowledgeBaseId}: KnowledgeBaseDocumentChunkEditDialogProps) => {
    const {content, handleClose, handleContentChange, handleOpenChange, handleSave, isPending, open} =
        useKnowledgeBaseDocumentChunkEditDialog({knowledgeBaseId});

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent className="sm:max-w-[600px]">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Edit Chunk</DialogTitle>

                        <DialogDescription>
                            Modify the text content of this chunk. Changes will be re-embedded in the vector store.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="content">Content</Label>

                        <Textarea
                            id="content"
                            onChange={(event) => handleContentChange(event.target.value)}
                            rows={10}
                            value={content}
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button onClick={handleClose} variant="outline">
                        Cancel
                    </Button>

                    <Button disabled={isPending} onClick={handleSave}>
                        {isPending ? 'Saving...' : 'Save Changes'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default KnowledgeBaseDocumentChunkEditDialog;
