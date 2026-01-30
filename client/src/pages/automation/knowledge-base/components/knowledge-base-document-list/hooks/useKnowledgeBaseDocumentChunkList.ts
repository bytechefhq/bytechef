import {
    KnowledgeBaseDocumentChunk,
    useDeleteKnowledgeBaseDocumentChunkMutation,
    useUpdateKnowledgeBaseDocumentChunkMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface UseKnowledgeBaseDocumentChunkListProps {
    knowledgeBaseId: string;
}

export default function useKnowledgeBaseDocumentChunkList({knowledgeBaseId}: UseKnowledgeBaseDocumentChunkListProps) {
    const queryClient = useQueryClient();

    const [selectedChunks, setSelectedChunks] = useState<string[]>([]);
    const [editingChunk, setEditingChunk] = useState<KnowledgeBaseDocumentChunk | null>(null);
    const [chunksToDelete, setChunksToDelete] = useState<string[]>([]);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const updateMutation = useUpdateKnowledgeBaseDocumentChunkMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});
            setEditingChunk(null);
        },
    });

    const deleteMutation = useDeleteKnowledgeBaseDocumentChunkMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});
            setSelectedChunks([]);
        },
    });

    const handleSelectChunk = (chunkId: string) => {
        setSelectedChunks((prev) =>
            prev.includes(chunkId) ? prev.filter((id) => id !== chunkId) : [...prev, chunkId]
        );
    };

    const handleDeleteChunk = (chunkId: string) => {
        setChunksToDelete([chunkId]);
        setShowDeleteDialog(true);
    };

    const handleDeleteSelectedChunks = () => {
        if (selectedChunks.length === 0) {
            return;
        }

        setChunksToDelete([...selectedChunks]);
        setShowDeleteDialog(true);
    };

    const handleConfirmDelete = () => {
        chunksToDelete.forEach((chunkId) => {
            deleteMutation.mutate({id: chunkId});
        });

        setShowDeleteDialog(false);
        setChunksToDelete([]);
    };

    const handleCloseDeleteDialog = () => {
        setShowDeleteDialog(false);
        setChunksToDelete([]);
    };

    const handleUpdateChunk = () => {
        if (!editingChunk) {
            return;
        }

        updateMutation.mutate({
            id: editingChunk.id,
            knowledgeBaseDocumentChunk: {
                content: editingChunk.content ?? '',
            },
        });
    };

    const handleStartEditing = (chunk: KnowledgeBaseDocumentChunk) => {
        setEditingChunk(chunk);
    };

    const handleClearSelection = () => {
        setSelectedChunks([]);
    };

    const handleCloseEditDialog = () => {
        setEditingChunk(null);
    };

    const handleUpdateEditingChunkContent = (content: string) => {
        if (editingChunk) {
            setEditingChunk({...editingChunk, content});
        }
    };

    return {
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
    };
}
