import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Input} from '@/components/ui/input';
import {PlusIcon, XIcon} from 'lucide-react';

export type IndexedFieldType = 'NUMERIC' | 'TEXT' | 'TIMESTAMP';

export interface IndexedFieldI {
    name: string;
    type: IndexedFieldType;
}

export interface AvailableFieldI {
    name: string;
    label?: string | null;
    type?: string | null;
}

interface IndexedFieldsEditorPropsI {
    /**
     * When provided and non-empty, the field-name input becomes a `<Select>` over the available fields and the
     * type selector auto-prefills based on the chosen field's Java type. When undefined or empty the editor
     * falls back to free-text input so the wizard still works for sources whose cluster element does not
     * implement `FieldsProvider`.
     */
    availableFields?: AvailableFieldI[];
    fields: IndexedFieldI[];
    onChange: (fields: IndexedFieldI[]) => void;
}

const TYPE_OPTIONS: IndexedFieldType[] = ['NUMERIC', 'TEXT', 'TIMESTAMP'];

const NUMERIC_JAVA_TYPES = new Set(['Long', 'Integer', 'Double', 'Float', 'Short', 'BigDecimal', 'BigInteger']);
const TIMESTAMP_JAVA_TYPES = new Set(['Instant', 'LocalDateTime', 'LocalDate', 'OffsetDateTime', 'ZonedDateTime']);

const inferIndexedFieldType = (javaType?: string | null): IndexedFieldType => {
    if (!javaType) return 'TEXT';

    if (NUMERIC_JAVA_TYPES.has(javaType)) return 'NUMERIC';

    if (TIMESTAMP_JAVA_TYPES.has(javaType)) return 'TIMESTAMP';

    return 'TEXT';
};

const IndexedFieldsEditor = ({availableFields, fields, onChange}: IndexedFieldsEditorPropsI) => {
    const hasAvailableFields = !!availableFields && availableFields.length > 0;

    const handleAdd = () => {
        onChange([...fields, {name: '', type: 'TEXT'}]);
    };

    const handleRemove = (index: number) => {
        onChange(fields.filter((_, fieldIndex) => fieldIndex !== index));
    };

    const handleNameChange = (index: number, name: string) => {
        onChange(fields.map((field, fieldIndex) => (fieldIndex === index ? {...field, name} : field)));
    };

    const handleAvailableFieldSelect = (index: number, name: string) => {
        const matchedField = availableFields?.find((field) => field.name === name);

        const inferredType = inferIndexedFieldType(matchedField?.type);

        onChange(
            fields.map((field, fieldIndex) => (fieldIndex === index ? {...field, name, type: inferredType} : field))
        );
    };

    const handleTypeChange = (index: number, type: IndexedFieldType) => {
        onChange(fields.map((field, fieldIndex) => (fieldIndex === index ? {...field, type} : field)));
    };

    return (
        <fieldset className="space-y-2 border-0 p-0">
            {fields.map((field, index) => (
                <div className="flex items-center gap-2" key={index}>
                    {hasAvailableFields ? (
                        <Select onValueChange={(value) => handleAvailableFieldSelect(index, value)} value={field.name}>
                            <SelectTrigger aria-label={`Field name ${index + 1}`}>
                                <SelectValue placeholder="Select a field..." />
                            </SelectTrigger>

                            <SelectContent>
                                {availableFields!.map((availableField) => (
                                    <SelectItem key={availableField.name} value={availableField.name}>
                                        {availableField.label || availableField.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    ) : (
                        <Input
                            aria-label={`Field name ${index + 1}`}
                            onChange={(event) => handleNameChange(index, event.target.value)}
                            placeholder="field_name"
                            value={field.name}
                        />
                    )}

                    <Select
                        onValueChange={(value) => handleTypeChange(index, value as IndexedFieldType)}
                        value={field.type}
                    >
                        <SelectTrigger aria-label={`Field type ${index + 1}`} className="w-32">
                            <SelectValue />
                        </SelectTrigger>

                        <SelectContent>
                            {TYPE_OPTIONS.map((option) => (
                                <SelectItem key={option} value={option}>
                                    {option}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    <button
                        aria-label={`Remove field ${index + 1}`}
                        className="rounded-md p-1 hover:bg-muted"
                        onClick={() => handleRemove(index)}
                        type="button"
                    >
                        <XIcon className="size-4" />
                    </button>
                </div>
            ))}

            <Button onClick={handleAdd} size="sm" type="button" variant="outline">
                <PlusIcon className="mr-2 size-4" />
                Add field
            </Button>
        </fieldset>
    );
};

export default IndexedFieldsEditor;
