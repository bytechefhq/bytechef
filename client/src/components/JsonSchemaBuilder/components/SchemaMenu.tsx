import React, {useMemo} from 'react';
import {useTranslation} from 'react-i18next';
import Select from 'react-select';

import * as helpers from '../utils/helpers';
import {SchemaMenuOptionType, SchemaRecordType} from '../utils/types';
import SchemaMenuList from './SchemaMenuList';

import '../../CreatableSelect/CreatableSelect.css';

interface SchemaMenuProps {
    schema: SchemaRecordType;
    onChange: (schema: SchemaRecordType) => void;
}

const SchemaMenu = ({onChange, schema}: SchemaMenuProps) => {
    const {t} = useTranslation();

    const type = helpers.getSchemaType(schema);
    const allOptions = useMemo(() => helpers.translateLabels(t, helpers.getSchemaMenuOptions(type)), [type, t]);

    const fields = helpers.getAllSchemaKeys(schema);

    const displayFields = useMemo(() => allOptions.filter((item) => fields.includes(item.value)), [allOptions, fields]);

    return (
        <div className="min-w-72">
            <SchemaMenuList fields={displayFields} onChange={onChange} schema={schema} />

            <Select
                className="w-full min-w-48"
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                onChange={(option: any) =>
                    onChange(helpers.setSchemaField((option as SchemaMenuOptionType).value, undefined, schema))
                }
                options={allOptions.filter((option) => !displayFields.some((field) => field.value === option.value))}
                placeholder={t('addFields')}
                value={null}
            />
        </div>
    );
};

export default SchemaMenu;
