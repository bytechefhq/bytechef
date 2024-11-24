import SchemaBox from '@/components/JsonSchemaBuilder/components/SchemaBox';
import {AsteriskIcon} from 'lucide-react';
import React, {useState} from 'react';

import * as helpers from '../utils/helpers';
import {SchemaRecordType} from '../utils/types';
import {SchemaArrayControls, SchemaControls} from './SchemaControls';

interface SchemaCreatorProps {
    schema: SchemaRecordType;
    schemakey?: string;
    isRequired?: boolean;
    onChangeKey?: (key: string) => void;
    onDelete?: (key: string) => void;
    onChange?: (schema: SchemaRecordType) => void;
}

const SchemaCreator = ({
    isRequired,
    onChange = () => {},
    onChangeKey = () => {},
    onDelete = () => {},
    schema,
    schemakey = '__root__',
}: SchemaCreatorProps) => {
    const [isCollapsed, setIsCollapsed] = useState<boolean>(false);

    return (
        <div className="h-full">
            <div className="flex items-end">
                {isRequired && <AsteriskIcon className="mb-3 mr-1 h-4 text-xs" />}

                <SchemaControls
                    isCollapsed={isCollapsed}
                    onAdd={
                        helpers.isSchemaObject(schema) ? () => onChange(helpers.addSchemaProperty(schema)) : undefined
                    }
                    onChange={onChange}
                    onChangeKey={schemakey !== '__root__' ? onChangeKey : undefined}
                    onCollapse={
                        helpers.isSchemaObject(schema) || helpers.isSchemaArray(schema)
                            ? () => setIsCollapsed((c) => !c)
                            : undefined
                    }
                    onDelete={schemakey !== '__root__' ? () => onDelete(schemakey) : undefined}
                    schema={schema}
                    schemakey={schemakey}
                />
            </div>

            <div className={`${isCollapsed ? 'hidden' : 'block'}`}>
                {helpers.isSchemaObject(schema) && helpers.hasSchemaProperties(schema) && (
                    <SchemaBox>
                        <SchemaObjectProperties
                            onChange={(key, s) => onChange(helpers.setSchemaProperty(key, s, schema))}
                            onChangeKey={(oldkey, newkey) =>
                                onChange(helpers.renameSchemaProperty(oldkey, newkey, schema))
                            }
                            onDelete={(key) => onChange(helpers.deleteSchemaProperty(key, schema))}
                            schema={schema}
                        />
                    </SchemaBox>
                )}

                {helpers.isSchemaArray(schema) && (
                    <SchemaBox>
                        <SchemaArrayItems
                            onChange={(s) => onChange(helpers.setSchemaItems(s, schema))}
                            schema={helpers.getSchemaItems(schema)}
                        />
                    </SchemaBox>
                )}
            </div>
        </div>
    );
};

interface SchemaArrayItemsProps {
    schema: SchemaRecordType;
    onChange: (schema: SchemaRecordType) => void;
}

const SchemaArrayItems = ({onChange, schema}: SchemaArrayItemsProps) => {
    return (
        <div>
            <SchemaArrayControls
                onAdd={helpers.isSchemaObject(schema) ? () => onChange(helpers.addSchemaProperty(schema)) : undefined}
                onChange={onChange}
                schema={schema}
            />

            {helpers.isSchemaObject(schema) && helpers.hasSchemaProperties(schema) && (
                <div className="mt-2">
                    <SchemaObjectProperties
                        onChange={(key, s) => onChange(helpers.setSchemaProperty(key, s, schema))}
                        onChangeKey={(oldkey, newkey) => onChange(helpers.renameSchemaProperty(oldkey, newkey, schema))}
                        onDelete={(key) => onChange(helpers.deleteSchemaProperty(key, schema))}
                        schema={schema}
                    />
                </div>
            )}

            {helpers.isSchemaArray(schema) && (
                <SchemaBox>
                    <SchemaArrayItems
                        onChange={(s) => onChange(helpers.setSchemaItems(s, schema))}
                        schema={helpers.getSchemaItems(schema)}
                    />
                </SchemaBox>
            )}
        </div>
    );
};

interface SchemaObjectPropertiesProps {
    schema: SchemaRecordType;
    onDelete: (key: string) => void;
    onChangeKey: (oldKey: string, newKey: string) => void;
    onChange: (key: string, schema: SchemaRecordType) => void;
}

const SchemaObjectProperties = ({onChange, onChangeKey, onDelete, schema}: SchemaObjectPropertiesProps) => {
    return (
        <ul className="grid gap-2">
            {Object.entries(helpers.getSchemaProperties(schema)).map(([key, s]) => (
                <li key={key}>
                    <SchemaCreator
                        isRequired={helpers.isFieldRequired(key, schema)}
                        onChange={(newSchema) => onChange(key, newSchema)}
                        onChangeKey={(newKey) => onChangeKey(key, newKey)}
                        onDelete={onDelete}
                        schema={s as SchemaRecordType}
                        schemakey={key}
                    />
                </li>
            ))}
        </ul>
    );
};

export default SchemaCreator;
