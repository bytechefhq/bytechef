import Button from '@/components/Button/Button';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {PropertyAllType} from '@/shared/types';
import {PlusIcon, XIcon} from 'lucide-react';
import {useRef} from 'react';
import {Control, FieldValues, FormState, useFormContext, useWatch} from 'react-hook-form';

interface ControlledArrayItemsProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control: Control<any, any>;
    controlPath: string;
    formState?: FormState<FieldValues>;
    property: PropertyAllType;
    toolsMode?: boolean;
}

/**
 * Renders array items in controlled (react-hook-form) mode.
 * Must be rendered within a FormProvider ancestor (e.g., shadcn's Form component).
 */
const ControlledArrayItems = ({control, controlPath, formState, property, toolsMode}: ControlledArrayItemsProps) => {
    const {setValue} = useFormContext();
    const watchedValue = useWatch({control, name: controlPath});
    const itemKeysRef = useRef<string[]>([]);

    const items = Array.isArray(watchedValue) ? (watchedValue as unknown[]) : [];
    const itemDefinition = property.items?.[0];

    // Sync stable keys with items (handles initial load and external resets)
    while (itemKeysRef.current.length < items.length) {
        itemKeysRef.current.push(crypto.randomUUID());
    }

    itemKeysRef.current.length = items.length;

    if (!itemDefinition) {
        return null;
    }

    return (
        <>
            <ul className="ml-2 flex flex-col space-y-4 border-l border-l-border/50">
                {items.map((item, index) => (
                    <li className="flex items-center gap-1" key={itemKeysRef.current[index]}>
                        <Property
                            control={control}
                            controlPath={controlPath}
                            customClassName="w-full pl-2"
                            deletePropertyButton={
                                <Button
                                    icon={<XIcon />}
                                    onClick={() => {
                                        itemKeysRef.current.splice(index, 1);

                                        setValue(
                                            controlPath,
                                            items.filter((_, itemIndex) => itemIndex !== index)
                                        );
                                    }}
                                    size="iconXs"
                                    variant="destructiveGhost"
                                />
                            }
                            formState={formState}
                            parameterValue={item}
                            property={
                                {
                                    ...itemDefinition,
                                    defaultValue: item,
                                    label: `Item ${index}`,
                                    name: String(index),
                                } as PropertyAllType
                            }
                            toolsMode={toolsMode}
                        />
                    </li>
                ))}
            </ul>

            <Button
                className="mt-3 rounded-sm"
                icon={<PlusIcon />}
                label="Add item"
                onClick={() => {
                    itemKeysRef.current.push(crypto.randomUUID());

                    setValue(controlPath, [...items, '']);
                }}
                size="sm"
                variant="secondary"
            />
        </>
    );
};

export default ControlledArrayItems;
