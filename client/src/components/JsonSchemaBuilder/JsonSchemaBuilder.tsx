import React, {useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';

import {SchemaRecordType} from './utils/types';

import './utils/i18n';

import SchemaCreator from '@/components/JsonSchemaBuilder/components/SchemaCreator';
import {isEmpty} from '@/components/JsonSchemaBuilder/utils/helpers';

interface JsonSchemaBuilderProps {
    locale?: string;
    onChange?: (newSchema: SchemaRecordType) => void;
    schema?: SchemaRecordType;
}

const JsonSchemaBuilder = ({locale = 'en', onChange, schema}: JsonSchemaBuilderProps) => {
    const [curSchema, setCurSchema] = useState<SchemaRecordType>();

    const {i18n, ready} = useTranslation('null', {useSuspense: false});

    useEffect(() => {
        setCurSchema(
            isEmpty(schema)
                ? {
                      $schema: 'https://json-schema.org/draft/2020-12/schema',
                      properties: {},
                      required: [],
                      type: 'object',
                  }
                : {...schema}
        );

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        i18n.changeLanguage(locale);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [locale]);

    if (!ready || !curSchema) {
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
