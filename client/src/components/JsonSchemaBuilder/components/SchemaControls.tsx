import SchemaInput from '@/components/JsonSchemaBuilder/components/SchemaInput';
import SchemaMenuPopover from '@/components/JsonSchemaBuilder/components/SchemaMenuPopover';
import SchemaTypesSelect from '@/components/JsonSchemaBuilder/components/SchemaTypesSelect';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {CircleEllipsisIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {
    addSchemaProperty,
    getSchemaTitle,
    getSchemaType,
    setSchemaTitle,
    setSchemaTypeAndRemoveWrongFields,
} from '../utils/helpers';
import {SchemaRecordType} from '../utils/types';

interface SchemaControlsProps {
    isCollapsed?: boolean;
    onAdd?: () => void;
    onChangeKey?: (key: string) => void;
    onChange: (schema: SchemaRecordType) => void;
    onCollapse?: () => void;
    onDelete?: () => void;
    root?: boolean;
    schema: SchemaRecordType;
    schemakey: string;
}

const SchemaControls = ({onAdd, onChange, onChangeKey, onDelete, root, schema, schemakey}: SchemaControlsProps) => {
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);

    const isObjectSchema = getSchemaType(schema) === 'object';

    let extraFields = Object.keys(schema).filter(
        (key) => key !== 'type' && key !== 'items' && key !== 'properties' && key !== '$schema'
    );

    if (root) {
        extraFields = extraFields.filter((key) => key !== 'required');
    }

    useEffect(() => {
        if (!schema.type || !getSchemaType(schema)) {
            onChange(setSchemaTypeAndRemoveWrongFields('object', schema));
        }
    }, [schema, onChange]);

    return (
        <div className="flex w-full items-end">
            <div className={twMerge('flex gap-2', root ? 'mr-1' : 'flex-1')}>
                <SchemaTypesSelect
                    onChange={(translation) => onChange(setSchemaTypeAndRemoveWrongFields(translation, schema))}
                    root={root}
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

            <div
                className={twMerge(
                    'ml-auto grid shrink-0 grid-flow-col items-center gap-1',
                    root && 'ml-0 flex-1 justify-between'
                )}
            >
                <SchemaMenuPopover
                    onChange={onChange}
                    onClose={() => setIsMenuOpen(false)}
                    open={isMenuOpen}
                    schema={schema}
                >
                    <Button
                        className={twMerge('group px-2.5', isMenuOpen && 'bg-surface-brand-secondary')}
                        onClick={() => setIsMenuOpen((open) => !open)}
                        title="Extra fields"
                        variant="ghost"
                    >
                        <CircleEllipsisIcon className={twMerge(isMenuOpen && 'text-content-brand-primary')} />

                        {root && <span>Extra fields</span>}

                        {extraFields?.length > 0 && (
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
                        className={twMerge(
                            root && 'ml-auto bg-surface-brand-primary hover:bg-surface-brand-primary-hover'
                        )}
                        disabled={!isObjectSchema}
                        onClick={onAdd || (() => onChange(addSchemaProperty(schema)))}
                        size={root ? 'default' : 'icon'}
                        title="Add Property"
                        variant={root ? 'default' : 'ghost'}
                    >
                        <PlusIcon />

                        {root && <span>Add Data Pill</span>}
                    </Button>
                )}
            </div>
        </div>
    );
};

interface SchemaArrayControlsProps {
    onAdd?: () => void;
    onChange: (schema: SchemaRecordType) => void;
    root?: boolean;
    schema: SchemaRecordType;
}

const SchemaArrayControls = ({onAdd, onChange, root, schema}: SchemaArrayControlsProps) => {
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);

    const isObjectSchema = getSchemaType(schema) === 'object';

    const extraFields: string[] =
        (schema &&
            isObjectSchema &&
            Object.keys(schema).filter((key) => key !== 'type' && key !== 'items' && key !== 'properties')) ||
        [];

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
                        title="Extra fields"
                        variant="ghost"
                    >
                        <CircleEllipsisIcon className={twMerge(isMenuOpen && 'text-content-brand-primary')} />

                        {root && <span>Extra fields</span>}

                        {extraFields?.length > 0 && (
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
