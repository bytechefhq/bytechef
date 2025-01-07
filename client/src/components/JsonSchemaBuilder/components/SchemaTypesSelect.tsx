import {Label} from '@/components/ui/label';
import React from 'react';
import {useTranslation} from 'react-i18next';

import {SCHEMA_TYPES} from '../utils/constants';
import * as helpers from '../utils/helpers';
import {SchemaType} from '../utils/types';

import '../../CreatableSelect/CreatableSelect.css';

import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';

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
                onValueChange={(option: SchemaType) => {
                    onChange(option);
                }}
                value={helpers.findOption(type)(options)?.value}
            >
                <SelectTrigger className="w-full min-w-48">
                    <SelectValue placeholder={t('type')} />
                </SelectTrigger>

                <SelectContent>
                    {options.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                            {option.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
};

export default SchemaTypesSelect;
