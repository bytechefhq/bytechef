import {getCookie} from '@/shared/util/cookie-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, useCallback, useState} from 'react';

interface SelectedFileI {
    file: File;
    status: 'completed' | 'error' | 'pending' | 'uploading';
    statusMessage?: string;
}

interface UseUploadKnowledgeBaseDocumentDialogProps {
    knowledgeBaseId: string;
}

export default function useUploadKnowledgeBaseDocumentDialog({
    knowledgeBaseId,
}: UseUploadKnowledgeBaseDocumentDialogProps) {
    const [open, setOpen] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState<SelectedFileI[]>([]);
    const [uploading, setUploading] = useState(false);

    const queryClient = useQueryClient();

    const resetForm = useCallback(() => {
        setSelectedFiles([]);
        setUploading(false);
    }, []);

    const uploadFile = useCallback(
        async (file: File, index: number) => {
            setSelectedFiles((prev) => {
                const updatedFiles = [...prev];

                updatedFiles[index] = {...updatedFiles[index], status: 'uploading'};

                return updatedFiles;
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
                    const updatedFiles = [...prev];

                    updatedFiles[index] = {
                        ...updatedFiles[index],
                        status: 'completed',
                        statusMessage: 'Uploaded successfully',
                    };

                    return updatedFiles;
                });
            } catch (error) {
                setSelectedFiles((prev) => {
                    const updatedFiles = [...prev];

                    updatedFiles[index] = {
                        ...updatedFiles[index],
                        status: 'error',
                        statusMessage: error instanceof Error ? error.message : 'Upload failed',
                    };

                    return updatedFiles;
                });
            }
        },
        [knowledgeBaseId]
    );

    const uploadFiles = useCallback(async () => {
        setUploading(true);

        const uploadPromises = selectedFiles.map((selectedFile, index) => uploadFile(selectedFile.file, index));

        await Promise.all(uploadPromises);

        queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});

        setTimeout(() => {
            setOpen(false);
            resetForm();
        }, 500);
    }, [selectedFiles, uploadFile, queryClient, knowledgeBaseId, resetForm]);

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

    const canSubmit = selectedFiles.length > 0 && !uploading;

    const handleSubmit = () => {
        if (selectedFiles.length > 0) {
            uploadFiles();
        }
    };

    const formatFileSize = (bytes: number) => {
        if (bytes === 0) {
            return '0 Bytes';
        }

        const bytesPerUnit = 1024;
        const sizeUnits = ['Bytes', 'KB', 'MB', 'GB'];
        const unitIndex = Math.floor(Math.log(bytes) / Math.log(bytesPerUnit));
        const sizeInUnit = bytes / Math.pow(bytesPerUnit, unitIndex);
        const formattedSize = parseFloat(sizeInUnit.toFixed(1));

        return formattedSize + ' ' + sizeUnits[unitIndex];
    };

    const handleOpenChange = (newOpen: boolean) => {
        setOpen(newOpen);

        if (!newOpen) {
            resetForm();
        }
    };

    return {
        canSubmit,
        formatFileSize,
        handleFileChange,
        handleOpenChange,
        handleSubmit,
        open,
        removeFile,
        selectedFiles,
        setOpen,
        uploading,
    };
}
