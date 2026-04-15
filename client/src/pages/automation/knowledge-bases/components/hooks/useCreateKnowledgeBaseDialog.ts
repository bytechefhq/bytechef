import {useCreateKnowledgeBaseMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getCookie} from '@/shared/util/cookie-utils';
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

    const uploadFile = async (knowledgeBaseId: string, file: File, index: number) => {
        setSelectedFiles((prev) => {
            const copy = [...prev];
            copy[index] = {...copy[index], status: 'uploading'};

            return copy;
        });

        try {
            const formData = new FormData();

            formData.append('file', file);

            const response = await fetch(`/api/automation/internal/knowledge-bases/${knowledgeBaseId}/documents`, {
                body: formData,
                headers: {
                    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                },
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Upload failed: ${response.statusText}`);
            }

            setSelectedFiles((prev) => {
                const copy = [...prev];
                copy[index] = {
                    ...copy[index],
                    status: 'completed',
                    statusMessage: 'Uploaded successfully',
                };

                return copy;
            });
        } catch (error) {
            setSelectedFiles((prev) => {
                const copy = [...prev];
                copy[index] = {
                    ...copy[index],
                    status: 'error',
                    statusMessage: error instanceof Error ? error.message : 'Upload failed',
                };

                return copy;
            });
        }
    };

    const uploadFiles = async (knowledgeBaseId: string, files: SelectedFileI[]) => {
        setUploading(true);

        await Promise.all(files.map((selectedFile, index) => uploadFile(knowledgeBaseId, selectedFile.file, index)));

        queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

        setTimeout(() => {
            setOpen(false);
            resetForm();
        }, 500);
    };

    const createMutation = useCreateKnowledgeBaseMutation({
        onSuccess: (data) => {
            const knowledgeBaseId = data.createKnowledgeBase?.id;

            if (selectedFiles.length > 0 && knowledgeBaseId) {
                uploadFiles(knowledgeBaseId, selectedFiles);
            } else {
                queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
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

    const formatFileSize = (bytes: number) => {
        if (bytes === 0) {
            return '0 Bytes';
        }

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
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
