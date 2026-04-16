import Button from '@/components/Button/Button';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {PropertyAllType} from '@/shared/types';
import {PlusIcon, XIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {Control, FieldValues, FormState, useFormContext, useWatch} from 'react-hook-form';

interface ControlledObjectEntriesProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control: Control<any, any>;
    controlPath: string;
    formState?: FormState<FieldValues>;
    hideFromAi?: boolean;
    property: PropertyAllType;
    toolsMode?: boolean;
}

const FormControlledObjectEntries = ({
    control,
    controlPath,
    formState,
    hideFromAi,
    property,
    toolsMode,
}: ControlledObjectEntriesProps) => {
    const [newEntryKey, setNewEntryKey] = useState('');

    const {setValue} = useFormContext();
    const watchedValue = useWatch({control, name: controlPath});

    const entries = useMemo(() => {
        if (watchedValue && typeof watchedValue === 'object' && !Array.isArray(watchedValue)) {
            return Object.entries(watchedValue as Record<string, unknown>);
        }

        return [];
    }, [watchedValue]);

    const itemDefinition = property.additionalProperties?.[0];

    const handleAddEntry = () => {
        const trimmedKey = newEntryKey.trim();

        if (!trimmedKey) {
            return;
        }

        const currentObject =
            watchedValue && typeof watchedValue === 'object' && !Array.isArray(watchedValue)
                ? (watchedValue as Record<string, unknown>)
                : {};

        if (trimmedKey in currentObject) {
            return;
        }

        setValue(controlPath, {
            ...currentObject,
            [trimmedKey]: '',
        });

        setNewEntryKey('');
    };

    return (
        <>
            <ul className="ml-2 flex flex-col space-y-4 border-l border-l-border/50">
                {entries.map(([entryKey, entryValue]) => (
                    <li className="flex items-center gap-1" key={`${controlPath}_${entryKey}`}>
                        <Property
                            control={control}
                            controlPath={controlPath}
                            customClassName="w-full pl-2"
                            deletePropertyButton={
                                <Button
                                    icon={<XIcon />}
                                    onClick={() => {
                                        const currentObject = {...(watchedValue as Record<string, unknown>)};

                                        delete currentObject[entryKey];

                                        setValue(controlPath, currentObject);
                                    }}
                                    size="iconXs"
                                    variant="destructiveGhost"
                                />
                            }
                            formState={formState}
                            hideFromAi={hideFromAi}
                            parameterValue={entryValue}
                            property={
                                {
                                    ...itemDefinition,
                                    controlType: 'TEXT',
                                    defaultValue: entryValue,
                                    label: entryKey,
                                    name: entryKey,
                                    type: itemDefinition?.type || 'STRING',
                                } as PropertyAllType
                            }
                            toolsMode={toolsMode}
                        />
                    </li>
                ))}
            </ul>

            {!!property.additionalProperties?.length && (
                <div className="mt-3 flex items-center gap-2">
                    <input
                        className="h-8 flex-1 rounded-md border bg-background px-2 text-sm"
                        onChange={(event) => setNewEntryKey(event.target.value)}
                        onKeyDown={(event) => {
                            if (event.key === 'Enter') {
                                event.preventDefault();

                                handleAddEntry();
                            }
                        }}
                        placeholder="Key name"
                        type="text"
                        value={newEntryKey}
                    />

                    <Button
                        className="rounded-sm"
                        disabled={!newEntryKey.trim()}
                        icon={<PlusIcon />}
                        label="Add"
                        onClick={handleAddEntry}
                        size="sm"
                        variant="secondary"
                    />
                </div>
            )}
        </>
    );
};

export default FormControlledObjectEntries;
