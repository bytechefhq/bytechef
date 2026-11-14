import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, DragEvent, useCallback, useState} from 'react';

interface SelectedFileI {
    file: File;
    status: 'completed' | 'error' | 'pending' | 'uploading';
    statusMessage?: string;
}

export default function useUploadCustomComponentDialog() {
    const [dragging, setDragging] = useState(false);
    const [open, setOpen] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState<SelectedFileI[]>([]);
    const [uploading, setUploading] = useState(false);

    const javaEnabled = useApplicationInfoStore((state) => state.component.customComponent.javaEnabled);

    const queryClient = useQueryClient();

    const acceptedExtensions = javaEnabled ? '.jar,.js,.py' : '.js,.py';

    const acceptedExtensionList = acceptedExtensions.split(',');

    const resetForm = useCallback(() => {
        setDragging(false);
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

            const response = await fetch('/api/platform/internal/custom-components/deploy', {
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

        void queryClient.invalidateQueries({queryKey: ['customComponents']});

        setTimeout(() => {
            setOpen(false);
            resetForm();
        }, 500);
    }, [selectedFiles, uploadFile, queryClient, resetForm]);

    const addFiles = (files: File[]) => {
        const newFiles = files
            .filter((file) => acceptedExtensionList.some((extension) => file.name.toLowerCase().endsWith(extension)))
            .map((file) => ({
                file,
                status: 'pending' as const,
            }));

        setSelectedFiles((prev) => [...prev, ...newFiles]);
    };

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            addFiles(Array.from(event.target.files));
        }
    };

    const handleDragOver = (event: DragEvent<HTMLElement>) => {
        event.preventDefault();

        if (uploading) {
            event.dataTransfer.dropEffect = 'none';

            return;
        }

        event.dataTransfer.dropEffect = 'copy';

        setDragging(true);
    };

    const handleDragLeave = (event: DragEvent<HTMLElement>) => {
        if (event.currentTarget.contains(event.relatedTarget as Node | null)) {
            return;
        }

        setDragging(false);
    };

    const handleDrop = (event: DragEvent<HTMLElement>) => {
        event.preventDefault();

        setDragging(false);

        if (uploading) {
            return;
        }

        addFiles(Array.from(event.dataTransfer.files));
    };

    const removeFile = (index: number) => {
        setSelectedFiles((prev) => prev.filter((_, fileIndex) => fileIndex !== index));
    };

    const canSubmit = selectedFiles.length > 0 && !uploading;

    const handleSubmit = () => {
        if (selectedFiles.length > 0) {
            void uploadFiles();
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
        acceptedExtensions,
        canSubmit,
        dragging,
        formatFileSize,
        handleDragLeave,
        handleDragOver,
        handleDrop,
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
