import SchemaAddButton from '@/components/JsonSchemaBuilder/components/SchemaAddButton';
import SchemaCollapseButton from '@/components/JsonSchemaBuilder/components/SchemaCollapseButton';
import SchemaDeleteButton from '@/components/JsonSchemaBuilder/components/SchemaDeleteButton';
import SchemaInput from '@/components/JsonSchemaBuilder/components/SchemaInput';
import SchemaMenu from '@/components/JsonSchemaBuilder/components/SchemaMenu';
import SchemaMenuButton from '@/components/JsonSchemaBuilder/components/SchemaMenuButton';
import SchemaMenuModal from '@/components/JsonSchemaBuilder/components/SchemaMenuModal';
import SchemaTypesSelect from '@/components/JsonSchemaBuilder/components/SchemaTypesSelect';
import React, {useState} from 'react';
import {useTranslation} from 'react-i18next';

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

    const {t} = useTranslation();

    return (
        <div className="flex flex-row items-end">
            <div className="mr-2 grid grid-flow-col gap-2">
                <SchemaInput
                    label={t('title')}
                    onChange={(title) => onChange(helpers.setSchemaTitle(title, schema))}
                    placeholder={t('title')}
                    value={helpers.getSchemaTitle(schema)}
                />

                <SchemaTypesSelect
                    onChange={(t) => onChange(helpers.setSchemaTypeAndRemoveWrongFields(t, schema))}
                    type={helpers.getSchemaType(schema)}
                />

                {typeof onChangeKey === 'function' ? (
                    <SchemaInput label={t('key')} onChange={onChangeKey} placeholder={t('key')} value={schemakey} />
                ) : null}
            </div>

            <div className="mb-0.5 grid grid-flow-col items-center gap-1">
                {typeof onCollapse === 'function' ? (
                    <SchemaCollapseButton isCollapsed={isCollapsed} onClick={onCollapse} title={t('collapse')} />
                ) : null}

                <SchemaMenuButton onClick={() => setIsMenuOpen((o) => !o)} title={t('extraOptions')} />

                {typeof onDelete === 'function' ? <SchemaDeleteButton onClick={onDelete} title={t('delete')} /> : null}

                {typeof onAdd === 'function' ? <SchemaAddButton onClick={onAdd} title={t('add')} /> : null}
            </div>

            {isMenuOpen && (
                <SchemaMenuModal onClose={() => setIsMenuOpen(false)} title={t('extraFields')}>
                    <SchemaMenu onChange={onChange} schema={schema} />
                </SchemaMenuModal>
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

    const {t} = useTranslation();

    return (
        <div className="flex items-end">
            <SchemaTypesSelect
                onChange={(t) => onChange(helpers.setSchemaTypeAndRemoveWrongFields(t, schema))}
                type={helpers.getSchemaType(schema)}
            />

            <div className="mb-0.5 ml-2 grid grid-flow-col gap-1">
                <SchemaMenuButton onClick={() => setIsMenuOpen((o) => !o)} title={t('extraOptions')} />

                {typeof onAdd === 'function' ? <SchemaAddButton onClick={onAdd} title={t('add')} /> : null}
            </div>

            {isMenuOpen ? (
                <SchemaMenuModal onClose={() => setIsMenuOpen(false)} title={t('extraFields')}>
                    <SchemaMenu onChange={onChange} schema={schema} />
                </SchemaMenuModal>
            ) : null}
        </div>
    );
};
