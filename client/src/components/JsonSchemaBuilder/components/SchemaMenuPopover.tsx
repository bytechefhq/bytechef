import Button from '@/components/Button/Button';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {X as CloseIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';
import {useTranslation} from 'react-i18next';
import Select from 'react-select';

import {getAllSchemaKeys, getSchemaMenuOptions, getSchemaType, setSchemaField, translateLabels} from '../utils/helpers';
import {SchemaMenuOptionType, SchemaRecordType} from '../utils/types';
import SchemaMenuList from './SchemaMenuList';

interface SchemaMenuPopoverProps {
    onChange: (schema: SchemaRecordType) => void;
    onClose: () => void;
    schema: SchemaRecordType;
    children?: ReactNode;
    open?: boolean;
}

const SchemaMenuPopover = ({children, onChange, onClose, open: controlledOpen, schema}: SchemaMenuPopoverProps) => {
    const [internalOpen, setInternalOpen] = useState(false);
    const [selectKey, setSelectKey] = useState(0);

    const isOpen = controlledOpen !== undefined ? controlledOpen : internalOpen;

    const {t: translation} = useTranslation();

    const type = getSchemaType(schema);
    const fields = getAllSchemaKeys(schema ?? {});

    const allOptions = useMemo(() => translateLabels(translation, getSchemaMenuOptions(type)), [type, translation]);
    const displayFields = useMemo(() => allOptions.filter((item) => fields.includes(item.value)), [allOptions, fields]);

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            onClose();
        }

        if (controlledOpen === undefined) {
            setInternalOpen(open);
        }
    };

    const handleSelectChange = (option: SchemaMenuOptionType) => {
        onChange(setSchemaField(option.value, undefined, schema));

        setSelectKey((previousSelectKey) => previousSelectKey + 1);
    };

    return (
        <Popover onOpenChange={handleOpenChange} open={isOpen}>
            <PopoverTrigger asChild>{children}</PopoverTrigger>

            <PopoverContent align="end" className="w-80 p-0">
                <div className="space-y-2 p-4">
                    <fieldset className="space-y-1 p-0.5">
                        <Label>Add a field</Label>

                        <Select
                            className="w-full min-w-48 text-sm"
                            isClearable={false}
                            key={selectKey}
                            onChange={handleSelectChange}
                            options={allOptions.filter(
                                (option) => !displayFields.some((field) => field.value === option.value)
                            )}
                            placeholder="Description, Required, etc."
                            value={null}
                        />
                    </fieldset>

                    <Button
                        className="absolute right-1 top-1 size-8 p-0 hover:bg-transparent hover:text-content-destructive"
                        icon={<CloseIcon className="size-4" />}
                        onClick={() => handleOpenChange(false)}
                        size="icon"
                        title="Close"
                        variant="ghost"
                    />

                    <SchemaMenuList fields={displayFields} onChange={onChange} schema={schema} />
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default SchemaMenuPopover;
