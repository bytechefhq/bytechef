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
    uploadButtonLabel: string;
}

export default function useOpenApiSpecificationField<T extends FieldValues, K extends FieldPath<T>>({
    field,
}: UseOpenApiSpecificationFieldProps<T, K>): UseOpenApiSpecificationFieldI {
    const hiddenInputRef = useRef<HTMLInputElement>(null);
    const [name, setName] = useState<string>();

    const handleUploadedFile = useCallback(
        async (event: ChangeEvent<HTMLInputElement>) => {
            const file = event.target.files?.item(0);

            if (file) {
                setName(file.name);

                field.onChange(await file.text());
            }
        },
        [field]
    );

    const onUpload = useCallback(() => {
        hiddenInputRef.current?.click();
    }, []);

    const uploadButtonLabel = name ? 'Change spec' : 'Upload spec';

    return {
        handleUploadedFile,
        hiddenInputRef,
        name,
        onUpload,
        uploadButtonLabel,
    };
}
