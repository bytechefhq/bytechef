import Button from '@/components/Button/Button';
import {Badge} from '@/components/ui/badge';
import {AsteriskIcon, ChevronDownIcon} from 'lucide-react';
import {PropsWithChildren, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {
    addSchemaProperty,
    deleteSchemaProperty,
    getSchemaItems,
    getSchemaProperties,
    hasSchemaProperties,
    isFieldRequired,
    isSchemaArray,
    isSchemaObject,
    renameSchemaProperty,
    setSchemaItems,
    setSchemaProperty,
} from '../utils/helpers';
import {SchemaRecordType} from '../utils/types';
import {SchemaArrayControls, SchemaControls} from './SchemaControls';

interface SchemaCreatorProps {
    isRequired?: boolean;
    onChangeKey?: (key: string) => void;
    onChange?: (schema: SchemaRecordType) => void;
    onDelete?: (key: string) => void;
    root?: boolean;
    schema: SchemaRecordType;
    schemakey?: string;
}

const SchemaCreator = ({
    isRequired,
    onChange = () => {},
    onChangeKey = () => {},
    onDelete = () => {},
    root = false,
    schema,
    schemakey = '__root__',
}: SchemaCreatorProps) => {
    const objectPropertiesCount =
        isSchemaObject(schema) && getSchemaProperties(schema) ? Object.keys(getSchemaProperties(schema)).length : 0;

    return (
        <div className="w-full min-w-0 overflow-hidden">
            <div className="flex items-end">
                {isRequired && <AsteriskIcon className="mb-3 mr-1 h-4 text-xs" />}

                <SchemaControls
                    onAdd={isSchemaObject(schema) ? () => onChange(addSchemaProperty(schema)) : undefined}
                    onChange={onChange}
                    onChangeKey={schemakey !== '__root__' ? onChangeKey : undefined}
                    onDelete={schemakey !== '__root__' ? () => onDelete(schemakey) : undefined}
                    root={root}
                    schema={schema}
                    schemakey={schemakey}
                />
            </div>

            <div className="block">
                {isSchemaObject(schema) && !hasSchemaProperties(schema) && root && (
                    <p className="p-4 text-xs text-content-neutral-secondary">
                        No pills added. Add some to start creating a response.
                    </p>
                )}

                {isSchemaObject(schema) && hasSchemaProperties(schema) && (
                    <SchemaBox itemCount={objectPropertiesCount}>
                        <SchemaObjectProperties
                            onChange={(key, s) => onChange(setSchemaProperty(key, s, schema))}
                            onChangeKey={(oldkey, newkey) => onChange(renameSchemaProperty(oldkey, newkey, schema))}
                            onDelete={(key) => onChange(deleteSchemaProperty(key, schema))}
                            schema={schema}
                        />
                    </SchemaBox>
                )}

                {isSchemaArray(schema) && (
                    <SchemaBox>
                        <SchemaArrayItems
                            onChange={(s) => onChange(setSchemaItems(s, schema))}
                            root={schemakey === '__root__'}
                            schema={getSchemaItems(schema)}
                        />
                    </SchemaBox>
                )}
            </div>
        </div>
    );
};

interface SchemaArrayItemsProps {
    schema: SchemaRecordType;
    root?: boolean;
    onChange: (schema: SchemaRecordType) => void;
}

const SchemaArrayItems = ({onChange, root, schema}: SchemaArrayItemsProps) => (
    <>
        <SchemaArrayControls
            onAdd={isSchemaObject(schema) ? () => onChange(addSchemaProperty(schema)) : undefined}
            onChange={onChange}
            root={root}
            schema={schema}
        />

        {isSchemaObject(schema) && hasSchemaProperties(schema) && (
            <div className="mt-2">
                <SchemaObjectProperties
                    onChange={(key, s) => onChange(setSchemaProperty(key, s, schema))}
                    onChangeKey={(oldkey, newkey) => onChange(renameSchemaProperty(oldkey, newkey, schema))}
                    onDelete={(key) => onChange(deleteSchemaProperty(key, schema))}
                    schema={schema}
                />
            </div>
        )}

        {isSchemaArray(schema) && (
            <SchemaBox>
                <SchemaArrayItems
                    onChange={(s) => onChange(setSchemaItems(s, schema))}
                    root={root}
                    schema={getSchemaItems(schema)}
                />
            </SchemaBox>
        )}
    </>
);

interface SchemaObjectPropertiesProps {
    schema: SchemaRecordType;
    onDelete: (key: string) => void;
    onChangeKey: (oldKey: string, newKey: string) => void;
    onChange: (key: string, schema: SchemaRecordType) => void;
}

const SchemaObjectProperties = ({onChange, onChangeKey, onDelete, schema}: SchemaObjectPropertiesProps) => (
    <ul className="grid w-full gap-2">
        {Object.entries(getSchemaProperties(schema)).map(([key, s]) => (
            <li key={key}>
                <SchemaCreator
                    isRequired={isFieldRequired(key, schema)}
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

const SchemaBox = ({children, itemCount}: {itemCount?: number} & PropsWithChildren) => {
    const [isCollapsed, setIsCollapsed] = useState<boolean>(false);

    return (
        <div
            className={twMerge(
                'relative my-4 ml-4 border-l-2 border-stroke-neutral-secondary pl-6',
                isCollapsed && 'h-2'
            )}
        >
            <Button
                className="absolute -left-3 top-0 size-6"
                icon={
                    <ChevronDownIcon className={twMerge('transition-all duration-200', isCollapsed && 'rotate-180')} />
                }
                onClick={() => setIsCollapsed((prev) => !prev)}
                size="icon"
                variant="outline"
            />

            {isCollapsed ? <Badge variant="secondary">{itemCount} items nested</Badge> : <div>{children}</div>}
        </div>
    );
};

export default SchemaCreator;
