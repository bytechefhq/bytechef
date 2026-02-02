import {getCookie} from '@/shared/util/cookie-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, useCallback, useState} from 'react';

interface SelectedFileI {
    file: File;
    status: 'completed' | 'error' | 'pending' | 'uploading';
    statusMessage?: string;
}

export default function useUploadCustomComponentDialog() {
    const [open, setOpen] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState<SelectedFileI[]>([]);
    const [uploading, setUploading] = useState(false);

    const queryClient = useQueryClient();

    const resetForm = useCallback(() => {
        setSelectedFiles([]);
        setUploading(false);
    }, []);

    const uploadFile = useCallback(async (file: File, index: number) => {
        setSelectedFiles((prev) => {
            const copy = [...prev];

            copy[index] = {...copy[index], status: 'uploading'};

            return copy;
        });

        try {
            const formData = new FormData();

            formData.append('componentFile', file);

            const response = await fetch('/api/platform/v1/custom-components/deploy', {
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
    }, []);

    const uploadFiles = useCallback(async () => {
        setUploading(true);

        const uploadPromises = selectedFiles.map((selectedFile, index) => uploadFile(selectedFile.file, index));

        await Promise.all(uploadPromises);

        queryClient.invalidateQueries({queryKey: ['customComponents']});

        setTimeout(() => {
            setOpen(false);
            resetForm();
        }, 500);
    }, [selectedFiles, uploadFile, queryClient, resetForm]);

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

        return parseFloat((bytes / Math.pow(bytesPerUnit, unitIndex)).toFixed(1)) + ' ' + sizeUnits[unitIndex];
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
