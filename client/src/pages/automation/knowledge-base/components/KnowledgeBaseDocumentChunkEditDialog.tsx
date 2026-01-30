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

interface EditingChunkI {
    id: string;
    content: string;
}

interface Props {
    editingChunk: EditingChunkI | null;
    isPending: boolean;
    onClose: () => void;
    onContentChange: (content: string) => void;
    onSave: () => void;
}

const KnowledgeBaseDocumentChunkEditDialog = ({editingChunk, isPending, onClose, onContentChange, onSave}: Props) => {
    if (!editingChunk) {
        return null;
    }

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={true}>
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
                            onChange={(event) => onContentChange(event.target.value)}
                            rows={10}
                            value={editingChunk.content}
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button onClick={onClose} variant="outline">
                        Cancel
                    </Button>

                    <Button disabled={isPending} onClick={onSave}>
                        {isPending ? 'Saving...' : 'Save Changes'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default KnowledgeBaseDocumentChunkEditDialog;
