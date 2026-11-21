import {useCreateKnowledgeBaseMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {formatFileSize, uploadSelectedFile} from '@/shared/util/file-upload-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, useState} from 'react';

interface SelectedFileI {
    documentId?: string;
    file: File;
    status: 'completed' | 'error' | 'pending' | 'processing' | 'uploading';
    statusMessage?: string;
}

interface UseCreateKnowledgeBaseDialogProps {
    workspaceId: string;
}

export default function useCreateKnowledgeBaseDialog({workspaceId}: UseCreateKnowledgeBaseDialogProps) {
    const [open, setOpen] = useState(false);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [minChunkSizeChars, setMinChunkSizeChars] = useState('1');
    const [maxChunkSize, setMaxChunkSize] = useState('1024');
    const [overlapSize, setOverlapSize] = useState('200');
    const [selectedFiles, setSelectedFiles] = useState<SelectedFileI[]>([]);
    const [uploading, setUploading] = useState(false);

    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const resetForm = () => {
        setName('');
        setDescription('');
        setMinChunkSizeChars('1');
        setMaxChunkSize('1024');
        setOverlapSize('200');
        setSelectedFiles([]);
        setUploading(false);
    };

    const uploadFile = (knowledgeBaseId: string, file: File, index: number) =>
        uploadSelectedFile({
            fieldName: 'file',
            file,
            index,
            setSelectedFiles,
            url: `/api/automation/internal/knowledge-bases/${knowledgeBaseId}/documents`,
        });

    const uploadFiles = async (knowledgeBaseId: string, files: SelectedFileI[]) => {
        setUploading(true);

        await Promise.all(files.map((selectedFile, index) => uploadFile(knowledgeBaseId, selectedFile.file, index)));

        void queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

        setTimeout(() => {
            setOpen(false);
            resetForm();
        }, 500);
    };

    const createMutation = useCreateKnowledgeBaseMutation({
        onSuccess: (data) => {
            const knowledgeBaseId = data.createKnowledgeBase?.id;

            if (selectedFiles.length > 0 && knowledgeBaseId) {
                void uploadFiles(knowledgeBaseId, selectedFiles);
            } else {
                void queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
                setOpen(false);
                resetForm();
            }
        },
    });

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            const newFiles = Array.from(event.target.files).map((file) => ({
                file,
                status: 'pending' as const,
            }));

            setSelectedFiles((prev) => [...prev, ...newFiles]);
        }
    };

    const removeFile = (index: number) => {
        setSelectedFiles((prev) => prev.filter((_, fileIndex) => fileIndex !== index));
    };

    const canSubmit = name.trim().length > 0;

    const handleSubmit = () => {
        createMutation.mutate({
            environmentId: String(environmentId),
            knowledgeBase: {
                description: description.trim() || undefined,
                maxChunkSize: parseInt(maxChunkSize),
                minChunkSizeChars: parseInt(minChunkSizeChars),
                name: name.trim(),
                overlap: parseInt(overlapSize),
            },
            workspaceId,
        });
    };

    const handleOpenChange = (newOpen: boolean) => {
        setOpen(newOpen);

        if (!newOpen) {
            resetForm();
        }
    };

    return {
        canSubmit,
        createMutation,
        description,
        formatFileSize,
        handleFileChange,
        handleOpenChange,
        handleSubmit,
        maxChunkSize,
        minChunkSizeChars,
        name,
        open,
        overlapSize,
        removeFile,
        selectedFiles,
        setDescription,
        setMaxChunkSize,
        setMinChunkSizeChars,
        setName,
        setOpen,
        setOverlapSize,
        uploading,
    };
}
