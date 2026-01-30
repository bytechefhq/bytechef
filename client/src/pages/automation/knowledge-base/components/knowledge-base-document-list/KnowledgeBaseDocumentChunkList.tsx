import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import KnowledgeBaseDocumentChunkEditDialog from '@/pages/automation/knowledge-base/components/KnowledgeBaseDocumentChunkEditDialog';
import KnowledgeBaseDocumentChunkDeleteDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkDeleteDialog';
import useKnowledgeBaseDocumentChunkList from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkList';
import {KnowledgeBaseDocumentChunk} from '@/shared/middleware/graphql';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseDocumentChunkListProps {
    chunks: KnowledgeBaseDocumentChunk[];
    documentName: string;
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentChunkList = ({
    chunks,
    documentName,
    knowledgeBaseId,
}: KnowledgeBaseDocumentChunkListProps) => {
    const {
        chunksToDelete,
        deleteMutation,
        editingChunk,
        handleClearSelection,
        handleCloseDeleteDialog,
        handleCloseEditDialog,
        handleConfirmDelete,
        handleDeleteChunk,
        handleDeleteSelectedChunks,
        handleSelectChunk,
        handleStartEditing,
        handleUpdateChunk,
        handleUpdateEditingChunkContent,
        selectedChunks,
        showDeleteDialog,
        updateMutation,
    } = useKnowledgeBaseDocumentChunkList({knowledgeBaseId});

    if (chunks.length === 0) {
        return (
            <div className="ml-7 border-l border-border/50 py-2 pl-4">
                <p className="text-sm text-muted-foreground">No chunks available for this document.</p>
            </div>
        );
    }

    return (
        <div className="ml-7 space-y-2 border-l border-border/50 py-2 pl-4">
            {selectedChunks.length > 0 && (
                <div className="flex items-center justify-between rounded-lg border border-blue-200 bg-blue-50 p-3">
                    <span className="text-sm font-medium text-blue-900">{selectedChunks.length} chunk(s) selected</span>

                    <div className="flex space-x-2">
                        {selectedChunks.length >= 1 && (
                            <Button onClick={handleDeleteSelectedChunks} size="sm" variant="destructive">
                                <Trash2Icon className="mr-2 size-4" />
                                Delete Selected
                            </Button>
                        )}

                        <Button onClick={handleClearSelection} size="sm" variant="outline">
                            Clear Selection
                        </Button>
                    </div>
                </div>
            )}

            {chunks.map((chunk, index) => (
                <div className="rounded-lg border border-gray-200 bg-white p-4" key={chunk.id}>
                    <div className="mb-2 flex items-center justify-between">
                        <div className="flex items-center space-x-3">
                            <Checkbox
                                checked={selectedChunks.includes(chunk.id)}
                                onCheckedChange={() => handleSelectChunk(chunk.id)}
                            />

                            <div className="flex items-center space-x-2 text-sm text-gray-500">
                                <span className="font-medium">{documentName}</span>

                                <span>•</span>

                                <span>Chunk {index + 1}</span>
                            </div>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="More Chunk Actions"
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" className="p-0">
                                <DropdownMenuItem
                                    className="dropdown-menu-item"
                                    onClick={() => handleStartEditing(chunk)}
                                >
                                    <EditIcon /> Edit
                                </DropdownMenuItem>

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    onClick={() => handleDeleteChunk(chunk.id)}
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>

                    <p className="ml-7 text-sm text-gray-700">{chunk.content}</p>

                    {chunk.metadata && (
                        <div className="ml-7 mt-2 text-xs text-gray-400">
                            <span className="font-medium">Metadata: </span>

                            {JSON.stringify(chunk.metadata)}
                        </div>
                    )}
                </div>
            ))}

            <KnowledgeBaseDocumentChunkEditDialog
                editingChunk={editingChunk ? {content: editingChunk.content ?? '', id: editingChunk.id} : null}
                isPending={updateMutation.isPending}
                onClose={handleCloseEditDialog}
                onContentChange={handleUpdateEditingChunkContent}
                onSave={handleUpdateChunk}
            />

            <KnowledgeBaseDocumentChunkDeleteDialog
                chunkCount={chunksToDelete.length}
                isPending={deleteMutation.isPending}
                onClose={handleCloseDeleteDialog}
                onConfirm={handleConfirmDelete}
                open={showDeleteDialog}
            />
        </div>
    );
};

export default KnowledgeBaseDocumentChunkList;
