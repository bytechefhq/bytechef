import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {VALUE_PROPERTY_CONTROL_TYPES} from '@/shared/constants';
import {PopoverClose} from '@radix-ui/react-popover';
import {PlusIcon, XIcon} from 'lucide-react';

import PropertyInput from './PropertyInput/PropertyInput';
import PropertySelect from './PropertySelect';

interface SubPropertyPopoverProps {
    array?: boolean;
    availablePropertyTypes: Array<{label: string; value: string}>;
    handleClick: () => void;
    newPropertyName?: string;
    newPropertyType: keyof typeof VALUE_PROPERTY_CONTROL_TYPES;
    setNewPropertyName?: (value: string) => void;
    setNewPropertyType: (value: keyof typeof VALUE_PROPERTY_CONTROL_TYPES) => void;
}

const SubPropertyPopover = ({
    array,
    availablePropertyTypes,
    handleClick,
    newPropertyName,
    newPropertyType,
    setNewPropertyName,
    setNewPropertyType,
}: SubPropertyPopoverProps) => {
    const handleNewPropertyNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        let {value} = event.target;

        if (value.match(/^\d/)) {
            value = `_${value}`;
        }

        if (setNewPropertyName) {
            setNewPropertyName(value);
        }
    };

    return (
        <Popover>
            <PopoverTrigger asChild>
                <Button
                    className="mt-3 rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                    size="sm"
                    variant="ghost"
                >
                    <PlusIcon className="mr-2 size-4" /> Add {array ? 'array item' : 'object property'}
                </Button>
            </PopoverTrigger>

            <PopoverContent className="min-w-[400px] space-y-4 p-4">
                <header className="flex items-center justify-between">
                    <span className="font-semibold">Add {array ? 'array item' : 'object property'}</span>

                    <PopoverClose asChild>
                        <XIcon
                            aria-hidden="true"
                            className="size-4 cursor-pointer"
                            onClick={() => setNewPropertyName && setNewPropertyName('')}
                        />
                    </PopoverClose>
                </header>

                <main className="space-y-2">
                    {!array && (
                        <PropertyInput
                            className="mb-2"
                            label="Name"
                            name="additionalPropertyName"
                            onChange={handleNewPropertyNameChange}
                            placeholder="Name for the additional property"
                            required
                            value={newPropertyName}
                        />
                    )}

                    {availablePropertyTypes?.length > 1 ? (
                        <PropertySelect
                            label="Type"
                            onValueChange={(value) =>
                                setNewPropertyType(value as keyof typeof VALUE_PROPERTY_CONTROL_TYPES)
                            }
                            options={availablePropertyTypes.map((property) => ({
                                label: property.value!,
                                value: property.value!,
                            }))}
                            value={newPropertyType}
                        />
                    ) : (
                        <div className="flex w-full flex-col">
                            <span className="mb-1 text-sm font-medium text-gray-700">Type</span>

                            {availablePropertyTypes[0] && (
                                <span className="inline-flex w-full rounded-md bg-white py-2 text-sm">
                                    {availablePropertyTypes[0].value}
                                </span>
                            )}
                        </div>
                    )}
                </main>

                <footer className="flex items-center justify-end space-x-2">
                    <PopoverClose asChild>
                        <Button
                            className="cursor-pointer"
                            disabled={!array && !newPropertyName}
                            onClick={handleClick}
                            size="sm"
                        >
                            Add
                        </Button>
                    </PopoverClose>
                </footer>
            </PopoverContent>
        </Popover>
    );
};

export default SubPropertyPopover;
