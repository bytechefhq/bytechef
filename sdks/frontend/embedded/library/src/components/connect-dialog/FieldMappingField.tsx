import {useEffect, useMemo, useState} from 'react';

import styles from './styles.module.css';
import {BoundFieldMappingConfigType, FieldMappingValueType, OptionType} from './types';

interface FieldMappingFieldProps {
    config: BoundFieldMappingConfigType;
    label: string;
    onChange: (value: FieldMappingValueType) => void;
    required?: boolean;
    value?: FieldMappingValueType;
}

interface RowType {
    custom: boolean;
    label: string;
    value: string;
}

const FieldMappingField = ({config, label, onChange, required, value}: FieldMappingFieldProps) => {
    const [objectType, setObjectType] = useState<string>(value?.objectType ?? '');
    const [objectTypeOptions, setObjectTypeOptions] = useState<OptionType[]>([]);
    const [fetchedIntegrationFieldOptions, setFetchedIntegrationFieldOptions] = useState<OptionType[]>([]);
    const [fetchedForObjectType, setFetchedForObjectType] = useState<string>('');
    const [mappings, setMappings] = useState<Record<string, string>>(() =>
        Object.fromEntries((value?.mappings ?? []).map((row) => [row.applicationField.value, row.integrationField]))
    );

    const [rows, setRows] = useState<RowType[]>(() => {
        const sourceFields = config.applicationFields.fields;
        const visible = config.applicationFields.defaultFields
            ? sourceFields.filter((field) => config.applicationFields.defaultFields!.includes(field.value))
            : sourceFields;

        return visible.map((field) => ({custom: false, label: field.label, value: field.value}));
    });

    const integrationFieldOptions = useMemo<OptionType[]>(
        () => (objectType && fetchedForObjectType === objectType ? fetchedIntegrationFieldOptions : []),
        [fetchedForObjectType, fetchedIntegrationFieldOptions, objectType]
    );

    // The label can contain spaces or other characters that are invalid in an HTML `id`, which would break
    // `<label htmlFor>` association; sanitize it to a stable, valid base and suffix rows with their index to keep ids
    // unique even when two sanitized field values would otherwise collide.
    const fieldIdBase = useMemo(() => label.replace(/[^a-zA-Z0-9_-]+/g, '-') || 'field-mapping', [label]);

    useEffect(() => {
        let cancelled = false;

        void config.objectTypes
            .get({search: undefined})
            .catch(() => [] as OptionType[])
            .then((options) => {
                if (!cancelled) {
                    setObjectTypeOptions(options);
                }
            });

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (!objectType) {
            return;
        }

        let cancelled = false;

        void config.integrationFields
            .get({objectType, search: undefined})
            .catch(() => [] as OptionType[])
            .then((options) => {
                if (!cancelled) {
                    setFetchedIntegrationFieldOptions(options);
                    setFetchedForObjectType(objectType);
                }
            });

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [objectType]);

    const emit = (nextObjectType: string, nextMappings: Record<string, string>, nextRows: RowType[]) => {
        onChange({
            mappings: nextRows
                .filter((row) => nextMappings[row.value])
                .map((row) => ({
                    applicationField: {custom: row.custom, label: row.label, value: row.value},
                    integrationField: nextMappings[row.value],
                })),
            objectType: nextObjectType,
        });
    };

    const handleObjectTypeChange = (next: string) => {
        setObjectType(next);

        emit(next, mappings, rows);
    };

    const handleRowChange = (fieldValue: string, integrationField: string) => {
        const nextMappings = {...mappings, [fieldValue]: integrationField};

        setMappings(nextMappings);

        emit(objectType, nextMappings, rows);
    };

    const handleRemoveRow = (fieldValue: string) => {
        const nextRows = rows.filter((row) => row.value !== fieldValue);
        const nextMappings = {...mappings};

        delete nextMappings[fieldValue];

        setRows(nextRows);
        setMappings(nextMappings);

        emit(objectType, nextMappings, nextRows);
    };

    const handleCreateField = () => {
        const fieldLabel = window.prompt('New field name')?.trim();

        if (!fieldLabel) {
            return;
        }

        // Reject duplicates so a repeated name cannot overwrite an existing row/mapping or collide on a DOM id.
        if (rows.some((row) => row.value === fieldLabel)) {
            return;
        }

        const nextRows = [...rows, {custom: true, label: fieldLabel, value: fieldLabel}];

        setRows(nextRows);

        emit(objectType, mappings, nextRows);
    };

    return (
        <fieldset className={styles.workflowInputsContainer}>
            <label>
                {label}

                {required && <span className={styles.requiredIndicator}>*</span>}
            </label>

            <fieldset className={styles.dialogInputField}>
                <label htmlFor={`${fieldIdBase}-objectType`}>Object type</label>

                <select
                    id={`${fieldIdBase}-objectType`}
                    onChange={(event) => handleObjectTypeChange(event.target.value)}
                    value={objectType}
                >
                    <option value="">Select object type</option>

                    {objectTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>
            </fieldset>

            {rows.map((row, index) => (
                <fieldset className={styles.dialogInputField} key={row.value}>
                    <label htmlFor={`${fieldIdBase}-row-${index}`}>{row.label}</label>

                    <select
                        disabled={!objectType}
                        id={`${fieldIdBase}-row-${index}`}
                        onChange={(event) => handleRowChange(row.value, event.target.value)}
                        value={mappings[row.value] ?? ''}
                    >
                        <option value="">{objectType ? 'Select field' : 'Select object type first'}</option>

                        {integrationFieldOptions.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </select>

                    {config.applicationFields.userCanRemoveMappings && (
                        <button onClick={() => handleRemoveRow(row.value)} type="button">
                            Remove
                        </button>
                    )}
                </fieldset>
            ))}

            {config.applicationFields.userCanCreateFields && (
                <button onClick={handleCreateField} type="button">
                    Add custom field
                </button>
            )}
        </fieldset>
    );
};

export default FieldMappingField;
