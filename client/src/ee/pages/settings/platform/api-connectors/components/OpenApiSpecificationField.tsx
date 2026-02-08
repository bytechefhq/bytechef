import Button from '@/components/Button/Button';
import {ControllerRenderProps, FieldPath, FieldValues} from 'react-hook-form';

import useOpenApiSpecificationField from './hooks/useOpenApiSpecificationField';

const OpenApiSpecificationField = <T extends FieldValues, K extends FieldPath<T>>({
    field,
}: {
    field: ControllerRenderProps<T, K>;
}) => {
    const {handleUploadedFile, hiddenInputRef, name, onUpload, uploadButtonLabel} = useOpenApiSpecificationField({
        field,
    });

    return (
        <div className="flex items-center space-x-4">
            {name && <span className="text-sm">{name}</span>}

            <Button onClick={onUpload} size="sm" type="button" variant="secondary">
                {uploadButtonLabel}
            </Button>

            <input
                accept=".yml,.yaml"
                className="hidden"
                onChange={(event) => handleUploadedFile(event)}
                ref={hiddenInputRef}
                type="file"
                value=""
            />
        </div>
    );
};

export default OpenApiSpecificationField;
