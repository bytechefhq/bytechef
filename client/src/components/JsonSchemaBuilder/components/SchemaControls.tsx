import SchemaInput from '@/components/JsonSchemaBuilder/components/SchemaInput';
import SchemaMenuPopover from '@/components/JsonSchemaBuilder/components/SchemaMenuPopover';
import SchemaTypesSelect from '@/components/JsonSchemaBuilder/components/SchemaTypesSelect';
import {Button} from '@/components/ui/button';
import {CircleEllipsisIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

import {
    addSchemaProperty,
    getSchemaTitle,
    getSchemaType,
    setSchemaTitle,
    setSchemaTypeAndRemoveWrongFields,
} from '../utils/helpers';
import {SchemaRecordType} from '../utils/types';
import {Badge} from '@/components/ui/badge';
import {twMerge} from 'tailwind-merge';

interface SchemaControlsProps {
    schema: SchemaRecordType;
    schemakey: string;
    isCollapsed?: boolean;
    onDelete?: () => void;
    onAdd?: () => void;
    onCollapse?: () => void;
    onChangeKey?: (key: string) => void;
    onChange: (schema: SchemaRecordType) => void;
}

const SchemaControls = ({onAdd, onChange, onChangeKey, onDelete, schema, schemakey}: SchemaControlsProps) => {
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);

    const isObjectSchema = getSchemaType(schema) === 'object';

    const extraFields = Object.keys(schema).filter((key) => key !== 'type' && key !== 'items' && key !== 'properties');

    useEffect(() => {
        if (!schema.type || !getSchemaType(schema)) {
            onChange(setSchemaTypeAndRemoveWrongFields('object', schema));
        }
    }, [schema, onChange]);

    return (
        <div className="flex w-full flex-row">
            <div className="flex flex-1 gap-2">
                <SchemaTypesSelect
                    onChange={(translation) => onChange(setSchemaTypeAndRemoveWrongFields(translation, schema))}
                    type={getSchemaType(schema)}
                />

                <SchemaInput
                    label="Pill Title"
                    onChange={(title) => onChange(setSchemaTitle(title, schema))}
                    placeholder="Untitled Pill"
                    value={getSchemaTitle(schema)}
                />

                {typeof onChangeKey === 'function' && (
                    <SchemaInput label="Pill Key" onChange={onChangeKey} placeholder="Pill Key" value={schemakey} />
                )}
            </div>

            <div className="ml-auto grid shrink-0 grid-flow-col items-center gap-1">
                <SchemaMenuPopover
                    onChange={onChange}
                    onClose={() => setIsMenuOpen(false)}
                    open={isMenuOpen}
                    schema={schema}
                >
                    <Button
                        className={twMerge('group px-2.5', isMenuOpen && 'bg-surface-brand-secondary')}
                        onClick={() => setIsMenuOpen((open) => !open)}
                        title="Extra options"
                        variant="ghost"
                    >
                        <CircleEllipsisIcon className={twMerge(isMenuOpen && 'text-content-brand-primary')} />

                        {extraFields.length > 0 && (
                            <Badge
                                className={twMerge(
                                    'group-hover:bg-surface-neutral-secondary-hover',
                                    isMenuOpen && 'bg-surface-brand-primary text-white'
                                )}
                                variant="secondary"
                            >
                                {extraFields.length}
                            </Badge>
                        )}
                    </Button>
                </SchemaMenuPopover>

                {typeof onDelete === 'function' && (
                    <Button
                        className="text-content-destructive/50 hover:bg-surface-destructive-secondary hover:text-content-destructive"
                        onClick={onDelete}
                        size="icon"
                        title="Delete"
                        variant="ghost"
                    >
                        <TrashIcon />
                    </Button>
                )}

                {(typeof onAdd === 'function' || isObjectSchema) && (
                    <Button
                        disabled={!isObjectSchema}
                        onClick={onAdd || (() => onChange(addSchemaProperty(schema)))}
                        size="icon"
                        title="Add Property"
                        variant="ghost"
                    >
                        <PlusIcon />
                    </Button>
                )}
            </div>
        </div>
    );
};

interface SchemaArrayControlsProps {
    onAdd?: () => void;
    onChange: (schema: SchemaRecordType) => void;
    schema: SchemaRecordType;
}

const SchemaArrayControls = ({onAdd, onChange, schema}: SchemaArrayControlsProps) => {
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);

    const extraFields = Object.keys(schema).filter((key) => key !== 'type' && key !== 'items' && key !== 'properties');

    return (
        <div className="flex w-full items-center">
            <SchemaTypesSelect
                onChange={(value) => onChange(setSchemaTypeAndRemoveWrongFields(value, schema))}
                type={getSchemaType(schema)}
            />

            <div className="ml-auto flex space-x-1">
                <SchemaMenuPopover
                    onChange={onChange}
                    onClose={() => setIsMenuOpen(false)}
                    open={isMenuOpen}
                    schema={schema}
                >
                    <Button
                        className={twMerge('group px-2.5', isMenuOpen && 'bg-surface-brand-secondary')}
                        onClick={() => setIsMenuOpen((open) => !open)}
                        title="Extra options"
                        variant="ghost"
                    >
                        <CircleEllipsisIcon className={twMerge(isMenuOpen && 'text-content-brand-primary')} />

                        {extraFields.length > 0 && (
                            <Badge
                                className={twMerge(
                                    'group-hover:bg-surface-neutral-secondary-hover',
                                    isMenuOpen && 'bg-surface-brand-primary text-white'
                                )}
                                variant="secondary"
                            >
                                {extraFields.length}
                            </Badge>
                        )}
                    </Button>
                </SchemaMenuPopover>

                {typeof onAdd === 'function' && (
                    <Button onClick={onAdd} size="icon" title="Add" variant="ghost">
                        <PlusIcon />
                    </Button>
                )}
            </div>
        </div>
    );
};

export {SchemaControls, SchemaArrayControls};
