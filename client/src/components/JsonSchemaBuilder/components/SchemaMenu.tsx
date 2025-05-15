import {useMemo} from 'react';
import {useTranslation} from 'react-i18next';
import Select from 'react-select';

import {getAllSchemaKeys, getSchemaMenuOptions, getSchemaType, setSchemaField, translateLabels} from '../utils/helpers';
import {SchemaMenuOptionType, SchemaRecordType} from '../utils/types';
import SchemaMenuList from './SchemaMenuList';

import '../../CreatableSelect/CreatableSelect.css';

interface SchemaMenuProps {
    schema: SchemaRecordType;
    onChange: (schema: SchemaRecordType) => void;
}

const SchemaMenu = ({onChange, schema}: SchemaMenuProps) => {
    const {t: translation} = useTranslation();

    const type = getSchemaType(schema);
    const fields = getAllSchemaKeys(schema);

    const allOptions = useMemo(() => translateLabels(translation, getSchemaMenuOptions(type)), [type, translation]);

    const displayFields = useMemo(() => allOptions.filter((item) => fields.includes(item.value)), [allOptions, fields]);

    return (
        <div className="min-w-72 text-sm">
            <SchemaMenuList fields={displayFields} onChange={onChange} schema={schema} />

            <Select
                className="w-full min-w-48"
                onChange={(option: SchemaMenuOptionType) => onChange(setSchemaField(option.value, undefined, schema))}
                options={allOptions.filter((option) => !displayFields.some((field) => field.value === option.value))}
                placeholder={translation('addFields')}
                value={null}
            />
        </div>
    );
};

export default SchemaMenu;
