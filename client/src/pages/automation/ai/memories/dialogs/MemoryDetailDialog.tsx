import Badge from '@/components/Badge/Badge';
import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import {format} from 'date-fns';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

import {AiAutoMemoryI, getAiAutoMemoryTypeLabel, useAiAutoMemoryDetailQuery} from '../hooks/useAiAutoMemories';

interface MemoryDetailDialogProps {
    memory: AiAutoMemoryI | null;
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

function formatTimestamp(value: string): string {
    try {
        return format(new Date(value), 'PPpp');
    } catch (formatError) {
        console.warn('MemoryDetailDialog: formatTimestamp failed', {
            message: formatError instanceof Error ? formatError.message : String(formatError),
            value,
        });

        return value;
    }
}

const MemoryDetailDialog = ({memory, onClose, open, workspaceId}: MemoryDetailDialogProps) => {
    const {data: memoryDetail, isPending} = useAiAutoMemoryDetailQuery(memory, workspaceId, open);

    return (
        <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
            <DialogContent size="lg">
                <DialogMain>
                    <DialogHeader
                        description={
                            <>
                                {memory ? `Name: ${memory.name}` : 'Loading...'}

                                {memory && ` - Updated ${formatTimestamp(memory.updatedAt)}`}
                            </>
                        }
                        endContent={
                            memory && (
                                <Badge
                                    label={getAiAutoMemoryTypeLabel(memory.memoryType)}
                                    styleType="secondary-outline"
                                />
                            )
                        }
                        title={memory?.title || 'Memory'}
                    />

                    {memory && (
                        <DialogBody>
                            <div className="flex flex-col gap-3">
                                {memory.description && (
                                    <p className="text-sm text-muted-foreground">{memory.description}</p>
                                )}

                                <div className="prose prose-sm max-w-none rounded-md border bg-muted/30 p-4 dark:prose-invert prose-table:block prose-table:overflow-x-auto">
                                    {memoryDetail ? (
                                        <Markdown remarkPlugins={[remarkGfm]}>{memoryDetail.content}</Markdown>
                                    ) : (
                                        <p className="text-sm text-muted-foreground">
                                            {isPending
                                                ? 'Loading memory content...'
                                                : 'This memory is no longer available.'}
                                        </p>
                                    )}
                                </div>
                            </div>
                        </DialogBody>
                    )}
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default MemoryDetailDialog;
