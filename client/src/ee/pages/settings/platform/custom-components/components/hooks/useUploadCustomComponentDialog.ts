import useFileUploadDialog from '@/shared/hooks/useFileUploadDialog';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {DragEvent, useCallback, useState} from 'react';

const INVALIDATE_QUERY_KEY = ['customComponents'];

export default function useUploadCustomComponentDialog() {
    const [dragging, setDragging] = useState(false);

    const javaEnabled = useApplicationInfoStore((state) => state.component.customComponent.javaEnabled);

    const acceptedExtensions = javaEnabled ? '.jar,.js,.py' : '.js,.py';

    const acceptedExtensionList = acceptedExtensions.split(',');

    const resetDragging = useCallback(() => setDragging(false), []);

    const {
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
    } = useFileUploadDialog({
        acceptFile: (file) => acceptedExtensionList.some((extension) => file.name.toLowerCase().endsWith(extension)),
        fieldName: 'componentFile',
        invalidateQueryKey: INVALIDATE_QUERY_KEY,
        onReset: resetDragging,
        url: '/api/platform/internal/custom-components/deploy',
    });

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
        javaEnabled,
        open,
        removeFile,
        selectedFiles,
        setOpen,
        uploading,
    };
}
