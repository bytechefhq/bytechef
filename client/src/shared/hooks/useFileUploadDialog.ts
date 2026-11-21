import {UploadFileStatusType, formatFileSize, uploadSelectedFile} from '@/shared/util/file-upload-utils';
import {QueryKey, useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, useCallback, useState} from 'react';

interface SelectedFileI {
    file: File;
    status: UploadFileStatusType;
    statusMessage?: string;
}

interface UseFileUploadDialogProps {
    acceptFile?: (file: File) => boolean;
    fieldName: string;
    invalidateQueryKey: QueryKey;
    onReset?: () => void;
    url: string;
}

export default function useFileUploadDialog({
    acceptFile,
    fieldName,
    invalidateQueryKey,
    onReset,
    url,
}: UseFileUploadDialogProps) {
    const [open, setOpen] = useState(false);
    const [selectedFiles, setSelectedFiles] = useState<SelectedFileI[]>([]);
    const [uploading, setUploading] = useState(false);

    const queryClient = useQueryClient();

    const resetForm = useCallback(() => {
        onReset?.();
        setSelectedFiles([]);
        setUploading(false);
    }, [onReset]);

    const uploadFiles = useCallback(async () => {
        setUploading(true);

        const uploadPromises = selectedFiles.map((selectedFile, index) =>
            uploadSelectedFile({fieldName, file: selectedFile.file, index, setSelectedFiles, url})
        );

        await Promise.all(uploadPromises);

        void queryClient.invalidateQueries({queryKey: invalidateQueryKey});

        setTimeout(() => {
            setOpen(false);
            resetForm();
        }, 500);
    }, [fieldName, invalidateQueryKey, queryClient, resetForm, selectedFiles, url]);

    const addFiles = (files: File[]) => {
        const newFiles = files
            .filter((file) => !acceptFile || acceptFile(file))
            .map((file) => ({
                file,
                status: 'pending' as const,
            }));

        setSelectedFiles((previousFiles) => [...previousFiles, ...newFiles]);
    };

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            addFiles(Array.from(event.target.files));
        }
    };

    const removeFile = (index: number) => {
        setSelectedFiles((previousFiles) => previousFiles.filter((_, fileIndex) => fileIndex !== index));
    };

    const canSubmit = selectedFiles.length > 0 && !uploading;

    const handleSubmit = () => {
        if (selectedFiles.length > 0) {
            void uploadFiles();
        }
    };

    const handleOpenChange = (newOpen: boolean) => {
        setOpen(newOpen);

        if (!newOpen) {
            resetForm();
        }
    };

    return {
        addFiles,
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
