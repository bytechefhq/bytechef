import SchemaCheckbox from '@/components/JsonSchemaBuilder/components/SchemaCheckbox';
import SchemaDeleteButton from '@/components/JsonSchemaBuilder/components/SchemaDeleteButton';
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

interface ItemProps {
    onDelete: () => void;
    children: React.ReactNode;
}

const Item = ({children, onDelete}: ItemProps) => {
    return (
        <div className="flex items-end justify-between">
            {children}

            <div className="ml-2">
                <SchemaDeleteButton onClick={onDelete} />
            </div>
        </div>
    );
};

interface ItemTypeProps {
    field: SchemaMenuOptionType;
    schema: SchemaRecordType;
    onChange: (schema: SchemaRecordType) => void;
}

export const TextItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <SchemaInput
                label={t(field.label)}
                onChange={(text) => onChange(setSchemaField(field.value, text, schema))}
                value={getSchemaField(schema, field.value) as string}
            />
        </Item>
    );
};

export const NumberItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <SchemaInput
                label={t(field.label)}
                onChange={(text) => onChange(setSchemaField(field.value, parseInt(text, 10), schema))}
                type={'number'}
                value={getSchemaField(schema, field.value) as string}
            />
        </Item>
    );
};

export const BoolItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();
    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <SchemaCheckbox
                label={t(field.label)}
                onChange={(text) => onChange(setSchemaField(field.value, text, schema))}
                value={getSchemaField(schema, field.value) as boolean}
            />
        </Item>
    );
};

export const CreatableMultiSelectItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();
    const selected = getSchemaField(schema, field.value);

    const allOptions = useMemo(() => stringsToOptions(selected as string[]), [selected]);

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <div className="w-full">
                <Label>{t(field.label)}</Label>

                <CreatableSelect
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    isMulti
                    noOptionsMessage={() => t('noOptions')}
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    onChange={(options: any) => {
                        onChange(setSchemaField(field.value, optionsToStrings(options), schema));
                    }}
                    options={allOptions}
                    placeholder={t('options')}
                    value={allOptions}
                />
            </div>
        </Item>
    );
};

export const SelectItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();
    const options = useMemo(() => translateLabels(t, field.optionList), [field.optionList, t]);
    const option = getSchemaField(schema, field.value);
    const selected = React.useMemo(() => findOption(option as string)(options), [options, option]);

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <div className="w-full">
                <Label>{t(field.label)}</Label>

                <Select
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    noOptionsMessage={() => t('noOptions')}
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    onChange={(option: any) => {
                        onChange(setSchemaField(field.value, option.value, schema));
                    }}
                    options={options}
                    placeholder={t('options')}
                    value={selected}
                />
            </div>
        </Item>
    );
};

export const RequiredMultiSelectItem: React.FunctionComponent<ItemTypeProps> = ({
    field,
    onChange,
    schema,
}: ItemTypeProps) => {
    const {t} = useTranslation();

    if (!isSchemaObject(schema) || !hasSchemaProperties(schema)) {
        return null;
    }

    const allOptions = schemaPropertiesAsOptions(schema);
    const requiredOptions = schemaRequiredPropertiesAsOptions(schema);

    return (
        <Item onDelete={() => onChange(deleteSchemaField(field.value, schema))}>
            <div className="w-full">
                <Label>{t(field.label)}</Label>

                <Select
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    isMulti
                    noOptionsMessage={() => t('noOptions')}
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    onChange={(options: any) => {
                        onChange(setSchemaField(field.value, optionsToStrings(options), schema));
                    }}
                    options={allOptions}
                    placeholder={t('options')}
                    value={requiredOptions}
                />
            </div>
        </Item>
    );
};
