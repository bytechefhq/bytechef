import {ChangeEvent, useCallback, useRef, useState} from 'react';
import {ControllerRenderProps, FieldPath, FieldValues} from 'react-hook-form';

interface UseOpenApiSpecificationFieldProps<T extends FieldValues, K extends FieldPath<T>> {
    field: ControllerRenderProps<T, K>;
}

interface UseOpenApiSpecificationFieldI {
    handleUploadedFile: (event: ChangeEvent<HTMLInputElement>) => Promise<void>;
    hiddenInputRef: React.RefObject<HTMLInputElement>;
    name: string | undefined;
    onUpload: () => void;
    preview: string | undefined;
    uploadButtonLabel: string;
}

export default function useOpenApiSpecificationField<T extends FieldValues, K extends FieldPath<T>>({
    field,
}: UseOpenApiSpecificationFieldProps<T, K>): UseOpenApiSpecificationFieldI {
    const hiddenInputRef = useRef<HTMLInputElement>(null);
    const [name, setName] = useState<string>();
    const [preview, setPreview] = useState<string>();

    const handleUploadedFile = useCallback(
        async (event: ChangeEvent<HTMLInputElement>) => {
            const file = event.target.files?.item(0);

            if (file) {
                setName(file.name);

                field.onChange(await file.text());

                const urlIcon = URL.createObjectURL(file);

                setPreview(urlIcon);
            }
        },
        [field]
    );

    const onUpload = useCallback(() => {
        hiddenInputRef.current?.click();
    }, []);

    const uploadButtonLabel = preview ? 'Change spec' : 'Upload spec';

    return {
        handleUploadedFile,
        hiddenInputRef,
        name,
        onUpload,
        preview,
        uploadButtonLabel,
    };
}
