import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
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
            <DialogContent size="md">
                <DialogMain>
                    <DialogHeader
                        description="Modify the text content of this chunk. Changes will be re-embedded in the vector store."
                        title="Edit Chunk"
                    />

                    <DialogBody>
                        <div className="space-y-2">
                            <Label htmlFor="content">Content</Label>

                            <Textarea
                                className="resize-none"
                                id="content"
                                onChange={(event) => handleContentChange(event.target.value)}
                                rows={10}
                                value={content}
                            />
                        </div>
                    </DialogBody>

                    <DialogFooter className="pt-2">
                        <Button onClick={handleClose} variant="outline">
                            Cancel
                        </Button>

                        <Button disabled={isPending} onClick={handleSave}>
                            {isPending ? 'Saving...' : 'Save Changes'}
                        </Button>
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default KnowledgeBaseDocumentChunkEditDialog;
