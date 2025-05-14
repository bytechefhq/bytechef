import SchemaInput from '@/components/JsonSchemaBuilder/components/SchemaInput';
import SchemaMenu from '@/components/JsonSchemaBuilder/components/SchemaMenu';
import SchemaMenuDialog from '@/components/JsonSchemaBuilder/components/SchemaMenuDialog';
import SchemaTypesSelect from '@/components/JsonSchemaBuilder/components/SchemaTypesSelect';
import {Button} from '@/components/ui/button';
import {ChevronDownIcon, ChevronRightIcon, CircleEllipsisIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useState} from 'react';

import * as helpers from '../utils/helpers';
import {SchemaRecordType} from '../utils/types';

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

export const SchemaControls = ({
    isCollapsed,
    onAdd,
    onChange,
    onChangeKey,
    onCollapse,
    onDelete,
    schema,
    schemakey,
}: SchemaControlsProps) => {
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);

    return (
        <div className="flex w-full flex-row items-end">
            <div className="grid grid-flow-col gap-2">
                <SchemaInput
                    label="Title"
                    onChange={(title) => onChange(helpers.setSchemaTitle(title, schema))}
                    placeholder="Title"
                    value={helpers.getSchemaTitle(schema)}
                />

                <SchemaTypesSelect
                    onChange={(translation) => onChange(helpers.setSchemaTypeAndRemoveWrongFields(translation, schema))}
                    type={helpers.getSchemaType(schema)}
                />

                {typeof onChangeKey === 'function' && (
                    <SchemaInput label="Key" onChange={onChangeKey} placeholder="Key" value={schemakey} />
                )}
            </div>

            <div className="ml-auto grid grid-flow-col items-center gap-1">
                {typeof onCollapse === 'function' && (
                    <Button
                        className="hover:bg-accent hover:text-accent-foreground"
                        onClick={onCollapse}
                        size="icon"
                        title="Collapse"
                        variant="ghost"
                    >
                        {isCollapsed ? <ChevronRightIcon /> : <ChevronDownIcon />}
                    </Button>
                )}

                <Button
                    onClick={() => setIsMenuOpen((open) => !open)}
                    size="icon"
                    title="Extra options"
                    variant="ghost"
                >
                    <CircleEllipsisIcon />
                </Button>

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

                {typeof onAdd === 'function' && (
                    <Button onClick={onAdd} size="icon" title="Add" variant="ghost">
                        <PlusIcon />
                    </Button>
                )}
            </div>

            {isMenuOpen && (
                <SchemaMenuDialog onClose={() => setIsMenuOpen(false)} title="Extra fields">
                    <SchemaMenu onChange={onChange} schema={schema} />
                </SchemaMenuDialog>
            )}
        </div>
    );
};

interface SchemaArrayControlsProps {
    schema: SchemaRecordType;
    onChange: (schema: SchemaRecordType) => void;
    onAdd?: () => void;
}

export const SchemaArrayControls = ({onAdd, onChange, schema}: SchemaArrayControlsProps) => {
    const [isMenuOpen, setIsMenuOpen] = useState<boolean>(false);

    return (
        <div className="flex items-end">
            <SchemaTypesSelect
                onChange={(value) => onChange(helpers.setSchemaTypeAndRemoveWrongFields(value, schema))}
                type={helpers.getSchemaType(schema)}
            />

            <div className="mb-0.5 ml-2 grid grid-flow-col gap-1">
                <Button
                    onClick={() => setIsMenuOpen((open) => !open)}
                    size="icon"
                    title="Extra options"
                    variant="ghost"
                >
                    <CircleEllipsisIcon />
                </Button>

                {typeof onAdd === 'function' && (
                    <Button onClick={onAdd} size="icon" title="Add" variant="ghost">
                        <PlusIcon />
                    </Button>
                )}
            </div>

            {isMenuOpen && (
                <SchemaMenuDialog onClose={() => setIsMenuOpen(false)} title="Extra fields">
                    <SchemaMenu onChange={onChange} schema={schema} />
                </SchemaMenuDialog>
            )}
        </div>
    );
};
