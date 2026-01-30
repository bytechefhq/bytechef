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

interface KnowledgeBaseDocumentChunkDeleteDialogProps {
    chunkCount: number;
    isPending: boolean;
    onClose: () => void;
    onConfirm: () => void;
    open: boolean;
}

const KnowledgeBaseDocumentChunkDeleteDialog = ({
    chunkCount,
    isPending,
    onClose,
    onConfirm,
    open,
}: KnowledgeBaseDocumentChunkDeleteDialogProps) => {
    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        {`This action cannot be undone. This will permanently delete ${chunkCount === 1 ? 'this chunk' : `${chunkCount} chunks`}.`}
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel className="shadow-none" disabled={isPending} onClick={onClose}>
                        Cancel
                    </AlertDialogCancel>

                    <AlertDialogAction
                        className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                        disabled={isPending}
                        onClick={onConfirm}
                    >
                        {isPending ? 'Deleting...' : 'Delete'}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default KnowledgeBaseDocumentChunkDeleteDialog;
