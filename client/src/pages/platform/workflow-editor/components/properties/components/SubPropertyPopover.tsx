import Button from '@/components/Button/Button';
import {Popover, PopoverClose, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import PropertySelect from '@/pages/platform/workflow-editor/components/properties/components/PropertySelect';
import PropertyInput from '@/pages/platform/workflow-editor/components/properties/components/property-input/PropertyInput';
import {encodePath} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {PlusIcon, XIcon} from 'lucide-react';
import {ChangeEvent, memo, useCallback, useEffect, useMemo, useState} from 'react';

export interface NewSubPropertyI {
    name: string;
    type: keyof typeof VALUE_PROPERTY_CONTROL_TYPES | string;
}

interface SubPropertyPopoverProps {
    array?: boolean;
    availablePropertyTypes: Array<{label: string; value: string}>;
    buttonLabel?: string;
    defaultPropertyType?: keyof typeof VALUE_PROPERTY_CONTROL_TYPES | string;
    disabled?: boolean;
    disabledTooltip?: string;
    existingPropertyNames?: Array<string>;
    handleClick: (newSubProperty: NewSubPropertyI) => void;
    insideConditionTaskDispatcher?: boolean;
    propertyName?: string;
}

const SubPropertyPopover = ({
    array,
    availablePropertyTypes,
    buttonLabel,
    defaultPropertyType,
    disabled,
    disabledTooltip,
    existingPropertyNames,
    handleClick,
    insideConditionTaskDispatcher,
    propertyName,
}: SubPropertyPopoverProps) => {
    const [newPropertyName, setNewPropertyName] = useState('');
    const [newPropertyType, setNewPropertyType] = useState<string>(
        defaultPropertyType ?? availablePropertyTypes[0]?.value ?? 'STRING'
    );

    const isDuplicateName = useMemo(() => {
        if (!newPropertyName || !existingPropertyNames?.length) {
            return false;
        }

        const encodedNewPropertyName = encodePath(newPropertyName);

        return existingPropertyNames.some(
            (existingPropertyName) => encodePath(existingPropertyName) === encodedNewPropertyName
        );
    }, [existingPropertyNames, newPropertyName]);

    const typeOptions = useMemo(
        () =>
            availablePropertyTypes.map((availablePropertyType) => ({
                label: availablePropertyType.label,
                value: availablePropertyType.value,
            })),
        [availablePropertyTypes]
    );

    const handleNewPropertyNameChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
        let {value} = event.target;

        if (value.match(/^\d/)) {
            value = `_${value}`;
        }

        setNewPropertyName(value);
    }, []);

    const handleTypeChange = useCallback((value: string) => setNewPropertyType(value), []);

    const handleAddClick = useCallback(() => {
        handleClick({name: newPropertyName, type: newPropertyType});

        setNewPropertyName('');
    }, [handleClick, newPropertyName, newPropertyType]);

    // The array flow resolves its available types asynchronously, so adopt the parent default
    // whenever it changes rather than only on mount.
    useEffect(() => {
        if (defaultPropertyType) {
            setNewPropertyType(defaultPropertyType);
        }
    }, [defaultPropertyType]);

    if (disabled && disabledTooltip) {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="inline-block">
                        <Button
                            className="mb-2 rounded-sm"
                            disabled
                            icon={<PlusIcon />}
                            label={buttonLabel || `Add ${array ? 'array item' : 'object property'}`}
                            size="sm"
                            variant="secondary"
                        />
                    </span>
                </TooltipTrigger>

                <TooltipContent>{disabledTooltip}</TooltipContent>
            </Tooltip>
        );
    }

    return (
        <Popover>
            <PopoverTrigger asChild>
                <Button
                    aria-label={`Add ${propertyName ? `${propertyName} ` : ''}${array ? 'array item' : 'object property'}`}
                    className="mb-2"
                    icon={<PlusIcon />}
                    label={buttonLabel || `Add ${array ? 'array item' : 'object property'}`}
                    size="sm"
                    variant="outline"
                />
            </PopoverTrigger>

            <PopoverContent
                aria-label={`${propertyName ? `${propertyName} ` : ''}${array ? 'array' : 'object'} property popover`}
                className="min-w-sub-property-popover-width space-y-4 p-4"
            >
                <header className="flex items-center justify-between">
                    <span className="font-semibold">Add {array ? 'array item' : 'object property'}</span>

                    <PopoverClose asChild>
                        <XIcon
                            aria-hidden="true"
                            className="size-4 cursor-pointer"
                            onClick={() => setNewPropertyName('')}
                        />
                    </PopoverClose>
                </header>

                <main className="space-y-2">
                    {!array && (
                        <PropertyInput
                            className="mb-2"
                            error={isDuplicateName}
                            errorMessage="A property with this name already exists."
                            label="Name"
                            name="additionalPropertyName"
                            onChange={handleNewPropertyNameChange}
                            placeholder="Name for the additional property"
                            required
                            value={newPropertyName}
                        />
                    )}

                    {insideConditionTaskDispatcher && typeOptions.length > 1 && (
                        <PropertySelect
                            label="Type"
                            onValueChange={handleTypeChange}
                            options={typeOptions}
                            value={newPropertyType}
                        />
                    )}

                    {!insideConditionTaskDispatcher &&
                        (typeOptions.length > 1 ? (
                            <PropertySelect
                                label="Type"
                                onValueChange={handleTypeChange}
                                options={typeOptions}
                                value={newPropertyType}
                            />
                        ) : (
                            <div className="flex w-full items-center gap-2 text-sm">
                                <span className="font-medium">Type</span>

                                {typeOptions[0] && (
                                    <span className="inline-flex w-full rounded-md bg-white">
                                        {typeOptions[0].value}
                                    </span>
                                )}
                            </div>
                        ))}
                </main>

                <footer className="flex items-center justify-end space-x-2">
                    <PopoverClose asChild>
                        <Button
                            disabled={isDuplicateName || (!array && !newPropertyName)}
                            label="Add"
                            onClick={handleAddClick}
                            size="sm"
                        />
                    </PopoverClose>
                </footer>
            </PopoverContent>
        </Popover>
    );
};

export default memo(SubPropertyPopover);
