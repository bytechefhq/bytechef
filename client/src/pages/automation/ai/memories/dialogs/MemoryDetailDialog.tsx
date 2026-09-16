import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {format} from 'date-fns';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

import {AiAutoMemoryI} from '../hooks/useAiAutoMemories';

interface MemoryDetailDialogProps {
    memory: AiAutoMemoryI | null;
    onClose: () => void;
    open: boolean;
}

function formatTimestamp(value: string): string {
    try {
        return format(new Date(value), 'PPpp');
    } catch (formatError) {
        // A server-side regression that emits a malformed timestamp causes the dialog to render the raw
        // string with zero ops signal. Log so the offending value is recoverable from the console.
        console.warn('MemoryDetailDialog: formatTimestamp failed', {
            message: formatError instanceof Error ? formatError.message : String(formatError),
            value,
        });

        return value;
    }
}

const MemoryDetailDialog = ({memory, onClose, open}: MemoryDetailDialogProps) => (
    <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
        <DialogContent className="max-h-[85vh] overflow-hidden sm:max-w-3xl">
            <DialogHeader>
                <DialogTitle className="flex items-center gap-2">
                    {memory?.title || 'Memory'}

                    {memory && <Badge label={memory.memoryType} styleType="secondary-outline" />}
                </DialogTitle>

                <DialogDescription>
                    {memory ? `Name: ${memory.name}` : 'Loading...'}

                    {memory && ` - Updated ${formatTimestamp(memory.updatedAt)}`}
                </DialogDescription>
            </DialogHeader>

            {memory && (
                <div className="flex flex-col gap-3 overflow-y-auto">
                    {memory.description && <p className="text-sm text-muted-foreground">{memory.description}</p>}

                    <div className="prose prose-sm max-w-none overflow-y-auto rounded-md border bg-muted/30 p-4 dark:prose-invert prose-table:block prose-table:overflow-x-auto">
                        {/* remarkGfm: react-markdown is CommonMark-only, and memory content written by the
                            model routinely contains pipe tables. */}

                        <Markdown remarkPlugins={[remarkGfm]}>{memory.content}</Markdown>
                    </div>
                </div>
            )}

            <DialogFooter>
                <Button label="Close" onClick={onClose} variant="outline" />
            </DialogFooter>
        </DialogContent>
    </Dialog>
);

export default MemoryDetailDialog;
