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
    hideFromAi?: boolean;
    property: PropertyAllType;
    toolsMode?: boolean;
}

const FormControlledArrayItems = ({
    control,
    controlPath,
    formState,
    hideFromAi,
    property,
    toolsMode,
}: ControlledArrayItemsProps) => {
    const itemKeysRef = useRef<string[]>([]);
    const nextKeyIdRef = useRef(0);

    const {setValue} = useFormContext();
    const watchedValue = useWatch({control, name: controlPath});

    const items = Array.isArray(watchedValue) ? (watchedValue as unknown[]) : [];
    const itemDefinition = property.items?.[0];

    while (itemKeysRef.current.length < items.length) {
        itemKeysRef.current.push(`item-${nextKeyIdRef.current++}`);
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
                            hideFromAi={hideFromAi}
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
                    itemKeysRef.current.push(`item-${nextKeyIdRef.current++}`);

                    setValue(controlPath, [...items, '']);
                }}
                size="sm"
                variant="secondary"
            />
        </>
    );
};

export default FormControlledArrayItems;
