import Button from '@/components/Button/Button';
import {Avatar, AvatarImage} from '@/components/ui/avatar';
import {ChangeEvent, useEffect, useRef, useState} from 'react';
import {ControllerRenderProps, FieldPath, FieldValues} from 'react-hook-form';

interface IconFieldProps<T extends FieldValues, K extends FieldPath<T>> {
    field: ControllerRenderProps<T, K>;
    onIconChange?: (value: string) => void;
}

const IconField = <T extends FieldValues, K extends FieldPath<T>>({field, onIconChange}: IconFieldProps<T, K>) => {
    const hiddenInputRef = useRef<HTMLInputElement>(null);

    const [preview, setPreview] = useState<string>();

    // Clean up object URL on unmount or when preview changes to prevent memory leaks
    useEffect(() => {
        return () => {
            if (preview) {
                URL.revokeObjectURL(preview);
            }
        };
    }, [preview]);

    const handleUploadedFile = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.item(0);

        if (file) {
            const iconText = await file.text();

            field.onChange(iconText);
            onIconChange?.(iconText);

            // Revoke previous URL before creating a new one
            if (preview) {
                URL.revokeObjectURL(preview);
            }

            const urlIcon = URL.createObjectURL(file);

            setPreview(urlIcon);
        }
    };

    const onUpload = () => {
        hiddenInputRef.current?.click();
    };

    const uploadButtonLabel = preview ? 'Change icon' : 'Upload icon';

    return (
        <div className="flex items-center space-x-1">
            {preview && (
                <Avatar>
                    <AvatarImage src={preview} />
                </Avatar>
            )}

            <Button onClick={onUpload} size="sm" type="button" variant="secondary">
                {uploadButtonLabel}
            </Button>

            <input
                accept=".svg"
                className="hidden"
                onChange={(e) => handleUploadedFile(e)}
                ref={hiddenInputRef}
                type="file"
                value=""
            />
        </div>
    );
};

export default IconField;
