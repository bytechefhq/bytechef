import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PropertyType} from '@/types/projectTypes';
import {DataPillType} from '@/types/types';
import Editor from '@monaco-editor/react';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import Select, {ISelectOption} from 'components/Select/Select';
import TextArea from 'components/TextArea/TextArea';
import {ChangeEvent} from 'react';
import {FieldValues, FormState, UseFormRegister} from 'react-hook-form';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';

import Input from '../Input/Input';
import ArrayProperty from './ArrayProperty';
import InputProperty from './InputProperty';
import ObjectProperty from './ObjectProperty';

const inputPropertyControlTypes = [
    'DATE',
    'DATE_TIME',
    'EMAIL',
    'INTEGER',
    'NUMBER',
    'PASSWORD',
    'PHONE',
    'TEXT',
    'TIME',
    'URL',
];

interface PropertyProps {
    actionName?: string;
    customClassName?: string;
    dataPills?: DataPillType[];
    defaultValue?: string;
    formState?: FormState<FieldValues>;
    mention?: boolean;
    onChange?: (event: ChangeEvent<HTMLInputElement>) => void;
    path?: string;
    property: PropertyType;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register?: UseFormRegister<any>;
}

const Property = ({
    actionName,
    customClassName,
    dataPills,
    defaultValue,
    formState,
    mention,
    onChange,
    path = 'parameters',
    property,
    register,
}: PropertyProps) => {
    const {
        controlType,
        description,
        hidden,
        label,
        name,
        options,
        required,
        type,
    } = property;

    const hasError = (propertyName: string): boolean =>
        formState?.touchedFields[path] &&
        formState?.touchedFields[path]![propertyName] &&
        formState?.errors[path] &&
        (formState?.errors[path] as never)[propertyName];

    const formattedOptions = options?.map(
        ({name}) =>
            ({
                label: name,
                value: name,
            }) as ISelectOption
    );

    if (!name) {
        return <></>;
    }

    return (
        <li
            className={twMerge(
                'mb-4 flex last:mb-0',
                controlType === 'CODE_EDITOR' && 'h-5/6',
                hidden && 'mb-0',
                type === 'OBJECT' && 'flex-col',
                type === 'ARRAY' && 'flex-col',
                customClassName
            )}
        >
            {(type === 'OBJECT' || type === 'ARRAY') && label && (
                <div className="flex items-center py-2">
                    <span className="pr-2" title={type}>
                        {TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    </span>

                    <span className="text-sm font-medium">{label}</span>

                    {description && (
                        <Tooltip>
                            <TooltipTrigger>
                                <QuestionMarkCircledIcon className="ml-1" />
                            </TooltipTrigger>

                            <TooltipContent>{description}</TooltipContent>
                        </Tooltip>
                    )}
                </div>
            )}

            {register && inputPropertyControlTypes.includes(controlType!) && (
                <Input
                    defaultValue={defaultValue as string}
                    description={description}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    required={required}
                    type={hidden ? 'hidden' : 'text'}
                    {...register(`${path}.${name}`, {
                        required: required!,
                    })}
                />
            )}

            {!register && inputPropertyControlTypes.includes(controlType!) && (
                <InputProperty
                    controlType={controlType}
                    dataPills={dataPills}
                    defaultValue={defaultValue}
                    description={description}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label || name}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    mention={mention}
                    name={name!}
                    onChange={onChange}
                    required={required}
                    type={type}
                />
            )}

            {controlType === 'SELECT' && !!formattedOptions?.length && (
                <Select
                    description={description}
                    label={label}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    options={formattedOptions}
                    triggerClassName="w-full border border-gray-300"
                />
            )}

            {controlType === 'CODE_EDITOR' && (
                <div className="h-full w-full border-2">
                    <Editor
                        defaultValue="// Add your custom code here..."
                        language={actionName}
                        options={{
                            extraEditorClassName: 'code-editor',
                            folding: false,
                            glyphMargin: false,
                            lineDecorationsWidth: 0,
                            lineNumbers: 'off',
                            lineNumbersMinChars: 0,
                            minimap: {
                                enabled: false,
                            },
                            scrollBeyondLastLine: false,
                            scrollbar: {
                                horizontalScrollbarSize: 4,
                                verticalScrollbarSize: 4,
                            },
                            tabSize: 2,
                        }}
                    />
                </div>
            )}

            {controlType === 'TEXT_AREA' && (
                <TextArea
                    description={description}
                    fieldsetClassName="w-full"
                    key={name}
                    required={required}
                    label={label}
                    name={name!}
                />
            )}

            {(type === 'ARRAY' || controlType === 'MULTI_SELECT') && (
                <ArrayProperty dataPills={dataPills} property={property} />
            )}

            {register && type === 'BOOLEAN' && (
                <Select
                    description={description}
                    label={label}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    options={[
                        {label: 'True', value: 'true'},
                        {label: 'False', value: 'false'},
                    ]}
                    triggerClassName="w-full border border-gray-300"
                    {...register(`${path}.${name}`, {
                        required: required!,
                    })}
                />
            )}

            {type === 'BOOLEAN' && (
                <Select
                    description={description}
                    label={label}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    options={[
                        {label: 'True', value: 'true'},
                        {label: 'False', value: 'false'},
                    ]}
                    triggerClassName="w-full border border-gray-300"
                />
            )}

            {type === 'OBJECT' && (
                <ObjectProperty dataPills={dataPills} property={property} />
            )}

            {controlType === 'SCHEMA_DESIGNER' && <span>Schema designer</span>}

            {!controlType && type === 'ANY' && (
                <span>
                    {controlType} - {type}
                </span>
            )}

            {type === 'NULL' && <span>NULL</span>}

            {type === 'DYNAMIC_PROPERTIES' && <span>Dynamic properties</span>}
        </li>
    );
};

export default Property;
