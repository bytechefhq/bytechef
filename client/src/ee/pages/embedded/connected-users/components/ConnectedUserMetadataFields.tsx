import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {PlusIcon, Trash2Icon} from 'lucide-react';
import {type Control, Controller, type FieldValues, type Path, get, useFieldArray} from 'react-hook-form';

export interface MetadataRowI {
    key: string;
    value: string;
}

export const metadataRowsToRecord = (metadataRows: MetadataRowI[] | undefined): Record<string, string> => {
    const metadata: Record<string, string> = {};

    for (const metadataRow of metadataRows ?? []) {
        if (metadataRow.key) {
            metadata[metadataRow.key] = metadataRow.value ?? '';
        }
    }

    return metadata;
};

const validateMetadataKey = (
    key: string | undefined,
    metadataRows: MetadataRowI[] | undefined,
    index: number
): string | true => {
    if (!key) {
        return metadataRows?.[index]?.value ? 'Key is required' : true;
    }

    const duplicate = (metadataRows ?? [])
        .slice(0, index)
        .some((previousMetadataRow) => previousMetadataRow.key === key);

    return duplicate ? 'Duplicate key' : true;
};

export const recordToMetadataRows = (metadata: Record<string, string> | undefined): MetadataRowI[] =>
    Object.entries(metadata ?? {}).map(([key, value]) => ({key, value}));

interface ConnectedUserMetadataFieldsProps<TFieldValues extends FieldValues> {
    control: Control<TFieldValues>;
    name: Path<TFieldValues>;
}

const ConnectedUserMetadataFields = <TFieldValues extends FieldValues>({
    control,
    name,
}: ConnectedUserMetadataFieldsProps<TFieldValues>) => {
    const {append, fields, remove} = useFieldArray({control, name: name as never});

    return (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <legend className="mb-1 text-sm font-medium">Metadata</legend>

            {fields.map((field, index) => (
                <div className="flex items-start gap-2" key={field.id}>
                    <Controller
                        control={control}
                        name={`${name}.${index}.key` as Path<TFieldValues>}
                        render={({field: keyField, fieldState}) => (
                            <div className="flex flex-1 flex-col gap-1">
                                <Input
                                    aria-invalid={!!fieldState.error}
                                    aria-label={`Metadata key ${index + 1}`}
                                    placeholder="Key"
                                    {...keyField}
                                />

                                {fieldState.error && (
                                    <p className="text-sm text-destructive">{fieldState.error.message}</p>
                                )}
                            </div>
                        )}
                        rules={{
                            deps: [`${name}.${index}.value` as Path<TFieldValues>],
                            validate: (key, formValues) => validateMetadataKey(key, get(formValues, name), index),
                        }}
                    />

                    <Controller
                        control={control}
                        name={`${name}.${index}.value` as Path<TFieldValues>}
                        render={({field: valueField}) => (
                            <div className="flex-1">
                                <Input aria-label={`Metadata value ${index + 1}`} placeholder="Value" {...valueField} />
                            </div>
                        )}
                    />

                    <Button
                        aria-label={`Remove metadata ${index + 1}`}
                        icon={<Trash2Icon />}
                        onClick={() => remove(index)}
                        size="icon"
                        type="button"
                        variant="ghost"
                    />
                </div>
            ))}

            <div>
                <Button
                    icon={<PlusIcon />}
                    label="Add metadata"
                    onClick={() => append({key: '', value: ''} as never)}
                    type="button"
                    variant="outline"
                />
            </div>
        </fieldset>
    );
};

export default ConnectedUserMetadataFields;
