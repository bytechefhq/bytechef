import Button from '@/components/Button/Button';
import {Avatar, AvatarImage} from '@/components/ui/avatar';
import {ControllerRenderProps, FieldPath, FieldValues} from 'react-hook-form';

import useIconField from './hooks/useIconField';

interface IconFieldProps<T extends FieldValues, K extends FieldPath<T>> {
    field: ControllerRenderProps<T, K>;
    onIconChange?: (value: string) => void;
}

const IconField = <T extends FieldValues, K extends FieldPath<T>>({field, onIconChange}: IconFieldProps<T, K>) => {
    const {handleUploadedFile, hiddenInputRef, onUpload, preview, uploadButtonLabel} = useIconField({
        field,
        onIconChange,
    });

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
                onChange={(event) => handleUploadedFile(event)}
                ref={hiddenInputRef}
                type="file"
                value=""
            />
        </div>
    );
};

export default IconField;
