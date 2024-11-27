import {Label} from '@/components/ui/label';
import React from 'react';
import {useTranslation} from 'react-i18next';
import Select from 'react-select';

import {SCHEMA_TYPES} from '../utils/constants';
import * as helpers from '../utils/helpers';
import {SchemaType} from '../utils/types';

import '../../CreatableSelect/CreatableSelect.css';

interface SchemaTypesSelectProps {
    type: SchemaType;
    onChange: (type: SchemaType) => void;
}

const SchemaTypesSelect = ({onChange, type}: SchemaTypesSelectProps) => {
    const {t} = useTranslation();

    const options = React.useMemo(() => helpers.translateLabels(t, SCHEMA_TYPES), [t]);

    return (
        <div>
            <Label>{t('type')}</Label>

            <Select
                className="w-full min-w-48"
                classNamePrefix="react-select"
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                onChange={(option: any) => onChange(option.value)}
                options={options}
                placeholder={t('type')}
                value={helpers.findOption(type)(options)}
            />
        </div>
    );
};

export default SchemaTypesSelect;
