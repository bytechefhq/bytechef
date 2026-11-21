import SchemaCreator from '@/components/JsonSchemaBuilder/components/SchemaCreator';
import {isEmpty} from '@/components/JsonSchemaBuilder/utils/helpers';
import {useEffect, useState} from 'react';

import {SchemaRecordType} from './utils/types';

interface JsonSchemaBuilderProps {
    onChange?: (newSchema: SchemaRecordType) => void;
    schema?: SchemaRecordType;
}

const buildSchema = (schema?: SchemaRecordType): SchemaRecordType =>
    isEmpty(schema)
        ? {
              $schema: 'https://json-schema.org/draft/2020-12/schema',
              properties: {},
              required: [],
              type: 'object',
          }
        : {...schema};

const JsonSchemaBuilder = ({onChange, schema}: JsonSchemaBuilderProps) => {
    const [curSchema, setCurSchema] = useState<SchemaRecordType>(() => buildSchema(schema));

    useEffect(() => {
        const nextSchema = buildSchema(schema);

        setCurSchema((current) => (JSON.stringify(current) === JSON.stringify(nextSchema) ? current : nextSchema));
    }, [schema]);

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
            root
            schema={curSchema}
        />
    );
};

export default JsonSchemaBuilder;
