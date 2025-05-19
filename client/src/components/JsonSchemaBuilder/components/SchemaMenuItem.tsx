import SchemaInput from '@/components/JsonSchemaBuilder/components/SchemaInput';
import {Label} from '@/components/ui/label';
import React, {useMemo} from 'react';
import {useTranslation} from 'react-i18next';
import Select from 'react-select';
import CreatableSelect from 'react-select/creatable';

import {
    deleteSchemaField,
    findOption,
    getSchemaField,
    hasSchemaProperties,
    isSchemaObject,
    optionsToStrings,
    schemaPropertiesAsOptions,
    schemaRequiredPropertiesAsOptions,
    setSchemaField,
    stringsToOptions,
    translateLabels,
} from '../utils/helpers';
import {SchemaMenuOptionType, SchemaRecordType} from '../utils/types';

import '../../CreatableSelect/CreatableSelect.css';

import {Button} from '@/components/ui/button';
import {Toggle} from '@/components/ui/toggle';
import {TrashIcon} from 'lucide-react';

interface ItemProps {
    onDelete: () => void;
    children: React.ReactNode;
}

const Item = ({children, onDelete}: ItemProps) => (
    <div className="flex w-full items-end justify-between gap-2">
        <div className="min-w-0 flex-1">{children}</div>

        <Button
            className="shrink-0 text-content-destructive/50 hover:bg-surface-destructive-secondary hover:text-content-destructive"
            onClick={onDelete}
            size="icon"
            variant="ghost"
        >
            <TrashIcon />
        </Button>
    </div>
);

interface ItemTypeProps {
    field: SchemaMenuOptionType;
    schema: SchemaRecordType;
    onChange: (schema: SchemaRecordType) => void;
}

export const TextItem = ({field, onChange, schema}: ItemTypeProps) => (
    <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
        <SchemaInput
            autoFocus
            label={field.label}
            onChange={(text) => onChange(setSchemaField(field.value, text, schema))}
            value={getSchemaField(schema, field.value) as string}
        />
    </Item>
);

export const NumberItem = ({field, onChange, schema}: ItemTypeProps) => (
    <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
        <SchemaInput
            autoFocus
            label={field.label}
            onChange={(text) => onChange(setSchemaField(field.value, parseInt(text, 10), schema))}
            type={'number'}
            value={getSchemaField(schema, field.value) as string}
        />
    </Item>
);

export const BoolItem = ({field, onChange, schema}: ItemTypeProps) => (
    <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
        <SchemaCheckbox
            autoFocus
            label={field.label}
            onChange={(text) => onChange(setSchemaField(field.value, text, schema))}
            value={getSchemaField(schema, field.value) as boolean}
        />
    </Item>
);

export const CreatableMultiSelectItem = ({field, onChange, schema}: ItemTypeProps) => {
    const selected = getSchemaField(schema, field.value);

    const allOptions = useMemo(() => stringsToOptions(selected as string[]), [selected]);

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <fieldset className="w-full space-y-1">
                <Label>{field.label}</Label>

                <CreatableSelect
                    autoFocus
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    isMulti
                    noOptionsMessage="No Options"
                    onChange={(options: {value: string}[]) =>
                        onChange(setSchemaField(field.value, optionsToStrings(options), schema))
                    }
                    options={allOptions}
                    placeholder="Options"
                    value={allOptions}
                />
            </fieldset>
        </Item>
    );
};

export const SelectItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t: translation} = useTranslation();

    const options = useMemo(() => translateLabels(translation, field.optionList), [field.optionList, translation]);

    const option = getSchemaField(schema, field.value);

    const selected = React.useMemo(() => findOption(option as string)(options), [options, option]);

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <fieldset className="w-full space-y-1">
                <Label>Options</Label>

                <Select
                    autoFocus
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    noOptionsMessage="No options"
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    onChange={(option: any) => onChange(setSchemaField(field.value, option.value, schema))}
                    options={options}
                    placeholder="Options"
                    value={selected}
                />
            </fieldset>
        </Item>
    );
};

export const RequiredMultiSelectItem: React.FunctionComponent<ItemTypeProps> = ({
    field,
    onChange,
    schema,
}: ItemTypeProps) => {
    if (!isSchemaObject(schema) || !hasSchemaProperties(schema)) {
        return null;
    }

    const allOptions = schemaPropertiesAsOptions(schema);
    const requiredOptions = schemaRequiredPropertiesAsOptions(schema);

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <fieldset className="w-full space-y-1">
                <Label>{field.label}</Label>

                <Select
                    autoFocus
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    isMulti
                    noOptionsMessage="No option"
                    onChange={(options: {value: string}[]) =>
                        onChange(setSchemaField(field.value, optionsToStrings(options), schema))
                    }
                    options={allOptions}
                    placeholder="Options"
                    value={requiredOptions}
                />
            </fieldset>
        </Item>
    );
};

interface SchemaCheckboxProps {
    autoFocus?: boolean;
    value: boolean;
    onChange: (value: boolean) => void;
    label?: string;
}

const SchemaCheckbox = ({autoFocus, label, onChange, value}: SchemaCheckboxProps) => {
    const handleChange =
        (handler: (value: boolean) => void) =>
        (event: React.FormEvent<HTMLElement>): void => {
            handler((event.target as HTMLInputElement).checked);
        };

    return (
        <div className="flex flex-row">
            <Label>{label}</Label>

            <Toggle autoFocus={autoFocus} className="ml-2" defaultChecked={value} onChange={handleChange(onChange)} />
        </div>
    );
};
