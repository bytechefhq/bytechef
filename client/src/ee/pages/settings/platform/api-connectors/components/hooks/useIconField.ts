import {ChangeEvent, useCallback, useEffect, useRef, useState} from 'react';
import {ControllerRenderProps, FieldPath, FieldValues} from 'react-hook-form';

interface UseIconFieldProps<T extends FieldValues, K extends FieldPath<T>> {
    field: ControllerRenderProps<T, K>;
    onIconChange?: (value: string) => void;
}

interface UseIconFieldI {
    handleUploadedFile: (event: ChangeEvent<HTMLInputElement>) => Promise<void>;
    hiddenInputRef: React.RefObject<HTMLInputElement>;
    onUpload: () => void;
    preview: string | undefined;
    uploadButtonLabel: string;
}

export default function useIconField<T extends FieldValues, K extends FieldPath<T>>({
    field,
    onIconChange,
}: UseIconFieldProps<T, K>): UseIconFieldI {
    const hiddenInputRef = useRef<HTMLInputElement>(null);
    const [preview, setPreview] = useState<string>();

    useEffect(() => {
        return () => {
            if (preview) {
                URL.revokeObjectURL(preview);
            }
        };
    }, [preview]);

    const handleUploadedFile = useCallback(
        async (event: ChangeEvent<HTMLInputElement>) => {
            const file = event.target.files?.item(0);

            if (file) {
                const iconText = await file.text();

                field.onChange(iconText);
                onIconChange?.(iconText);

                if (preview) {
                    URL.revokeObjectURL(preview);
                }

                const urlIcon = URL.createObjectURL(file);

                setPreview(urlIcon);
            }
        },
        [field, onIconChange, preview]
    );

    const onUpload = useCallback(() => {
        hiddenInputRef.current?.click();
    }, []);

    const uploadButtonLabel = preview ? 'Change icon' : 'Upload icon';

    return {
        handleUploadedFile,
        hiddenInputRef,
        onUpload,
        preview,
        uploadButtonLabel,
    };
}
