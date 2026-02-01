import {useCreateKnowledgeBaseMutation} from '@/shared/middleware/graphql';
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

    const startSimulatedUpload = () => {
        setUploading(true);

        selectedFiles.forEach((_, index) => {
            setSelectedFiles((prev) => {
                const copy = [...prev];
                copy[index] = {...copy[index], status: 'uploading'};

                return copy;
            });

            setTimeout(() => {
                setSelectedFiles((prev) => {
                    const copy = [...prev];
                    copy[index] = {...copy[index], status: 'processing', statusMessage: 'Processing document...'};

                    return copy;
                });
            }, 1000);

            setTimeout(() => {
                setSelectedFiles((prev) => {
                    const copy = [...prev];
                    copy[index] = {
                        ...copy[index],
                        status: 'completed',
                        statusMessage: 'Document processed successfully',
                    };

                    return copy;
                });

                if (index === selectedFiles.length - 1) {
                    setTimeout(() => {
                        queryClient.invalidateQueries({queryKey: ['knowledgeBases']});
                        setOpen(false);
                        resetForm();
                    }, 500);
                }
            }, 3000);
        });
    };

    const createMutation = useCreateKnowledgeBaseMutation({
        onSuccess: () => {
            if (selectedFiles.length > 0) {
                startSimulatedUpload();
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
