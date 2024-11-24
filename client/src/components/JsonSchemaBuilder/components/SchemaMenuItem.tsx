import SchemaCheckbox from '@/components/JsonSchemaBuilder/components/SchemaCheckbox';
import SchemaDeleteButton from '@/components/JsonSchemaBuilder/components/SchemaDeleteButton';
import SchemaInput from '@/components/JsonSchemaBuilder/components/SchemaInput';
import {Label} from '@/components/ui/label';
import React, {useMemo} from 'react';
import {useTranslation} from 'react-i18next';
import Select from 'react-select';
import CreatableSelect from 'react-select/creatable';

import * as helpers from '../utils/helpers';
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
        <Item onDelete={() => onChange(helpers.deleteSchemaField(field.value, schema))}>
            <SchemaInput
                label={t(field.label)}
                onChange={(text) => onChange(helpers.setSchemaField(field.value, text, schema))}
                value={helpers.getSchemaField(schema, field.value) as string}
            />
        </Item>
    );
};

export const NumberItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();

    return (
        <Item onDelete={() => onChange(helpers.deleteSchemaField(field.value, schema))}>
            <SchemaInput
                label={t(field.label)}
                onChange={(text) => onChange(helpers.setSchemaField(field.value, parseInt(text, 10), schema))}
                type={'number'}
                value={helpers.getSchemaField(schema, field.value) as string}
            />
        </Item>
    );
};

export const BoolItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();
    return (
        <Item onDelete={() => onChange(helpers.deleteSchemaField(field.value, schema))}>
            <SchemaCheckbox
                label={t(field.label)}
                onChange={(text) => onChange(helpers.setSchemaField(field.value, text, schema))}
                value={helpers.getSchemaField(schema, field.value) as boolean}
            />
        </Item>
    );
};

export const CreatableMultiSelectItem = ({field, onChange, schema}: ItemTypeProps) => {
    const {t} = useTranslation();
    const selected = helpers.getSchemaField(schema, field.value);

    const allOptions = useMemo(() => helpers.stringsToOptions(selected as string[]), [selected]);

    return (
        <Item onDelete={() => onChange(helpers.deleteSchemaField(field.value, schema))}>
            <div className="w-full">
                <Label>{t(field.label)}</Label>

                <CreatableSelect
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    isMulti
                    noOptionsMessage={() => t('noOptions')}
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    onChange={(options: any) => {
                        onChange(helpers.setSchemaField(field.value, helpers.optionsToStrings(options), schema));
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
    const options = React.useMemo(() => helpers.translateLabels(t, field.optionList), [field.optionList, t]);
    const option = helpers.getSchemaField(schema, field.value);
    const selected = React.useMemo(() => helpers.findOption(option as string)(options), [options, option]);

    return (
        <Item onDelete={() => onChange(helpers.deleteSchemaField(field.value, schema))}>
            <div className="w-full">
                <Label>{t(field.label)}</Label>

                <Select
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    noOptionsMessage={() => t('noOptions')}
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    onChange={(option: any) => {
                        onChange(helpers.setSchemaField(field.value, option.value, schema));
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

    if (!helpers.isSchemaObject(schema) || !helpers.hasSchemaProperties(schema)) {
        return null;
    }

    const allOptions = helpers.schemaPropertiesAsOptions(schema);
    const requiredOptions = helpers.schemaRequiredPropertiesAsOptions(schema);

    return (
        <Item onDelete={() => onChange(helpers.deleteSchemaField(field.value, schema))}>
            <div className="w-full">
                <Label>{t(field.label)}</Label>

                <Select
                    className="w-full min-w-48"
                    classNamePrefix="react-select"
                    isMulti
                    noOptionsMessage={() => t('noOptions')}
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    onChange={(options: any) => {
                        onChange(helpers.setSchemaField(field.value, helpers.optionsToStrings(options), schema));
                    }}
                    options={allOptions}
                    placeholder={t('options')}
                    value={requiredOptions}
                />
            </div>
        </Item>
    );
};
