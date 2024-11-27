import React, {useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';

import {SchemaRecordType} from './utils/types';

import './utils/i18n';

import SchemaCreator from '@/components/JsonSchemaBuilder/components/SchemaCreator';
import {DEFAULT_SCHEMA} from '@/components/JsonSchemaBuilder/utils/constants';
import {isEmpty} from '@/components/JsonSchemaBuilder/utils/helpers';

interface JsonSchemaBuilderProps {
    locale?: string;
    onChange?: (newSchema: SchemaRecordType) => void;
    schema: SchemaRecordType;
}

const JsonSchemaBuilder = ({locale = 'en', onChange, schema}: JsonSchemaBuilderProps) => {
    const [curSchema, setCurSchema] = useState<SchemaRecordType>(!isEmpty(schema) ? {...schema} : {...DEFAULT_SCHEMA});

    const {i18n, ready} = useTranslation('null', {useSuspense: false});

    useEffect(() => {
        i18n.changeLanguage(locale);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [locale]);

    if (!ready) {
        return null;
    }

    return (
        <SchemaCreator
            onChange={(schema) => {
                if (onChange) {
                    onChange(schema);
                }

                setCurSchema({
                    ...schema,
                });
            }}
            schema={curSchema}
        />
    );
};

export default JsonSchemaBuilder;
