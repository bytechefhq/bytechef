import {Avatar, AvatarImage} from '@/components/ui/avatar';
import {Button} from '@/components/ui/button';
import React, {ChangeEvent, useRef, useState} from 'react';
import {FieldValues} from 'react-hook-form';
import {FieldPath} from 'react-hook-form/dist/types';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';

const IconField = <T extends FieldValues, K extends FieldPath<T>>({field}: {field: ControllerRenderProps<T, K>}) => {
    const hiddenInputRef = useRef<HTMLInputElement>(null);

    const [preview, setPreview] = useState<string>();

    const handleUploadedFile = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.item(0);

        if (file) {
            field.onChange(await file.text());

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
