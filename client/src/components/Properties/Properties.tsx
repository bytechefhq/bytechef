/// <reference types="vite-plugin-svgr/client" />

import Editor from '@monaco-editor/react';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import Select, {ISelectOption} from 'components/Select/Select';
import Tooltip from 'components/Tooltip/Tooltip';
import {TagModel} from 'middleware/core/tag/models/TagModel';
import {useState} from 'react';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

import Input from '../Input/Input';
import ArrayProperty from './ArrayProperty';
import ObjectProperty from './ObjectProperty';

export interface PropertyFormProps {
    authorizationName: string;
    componentName: {
        value: string;
        label: string;
    };
    name: string;
    parameters: {[key: string]: object};
    tags: Array<TagModel | {label: string; value: string}>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    [key: string]: any;
}

interface PropertyProps {
    property: PropertyType;
    customClassName?: string;
    actionName?: string;
    formState?: FormState<FieldValues>;
    path?: string;
    register?: UseFormRegister<PropertyFormProps>;
}

export const Property = ({
    actionName,
    customClassName,
    formState,
    path = 'parameters',
    property,
    register,
}: PropertyProps) => {
    const [integerValue, setIntegerValue] = useState('');

    const {
        controlType,
        defaultValue = '',
        description,
        hidden,
        label,
        name,
        options,
        required,
        type,
        types,
    } = property;

    const hasError = (propertyName: string) =>
        formState?.touchedFields[path] &&
        formState?.touchedFields[path]![propertyName] &&
        formState?.errors[path] &&
        (formState?.errors[path] as never)[propertyName];

    const formattedOptions = options?.map(
        ({name}) =>
            ({
                label: name,
                value: name,
            } as ISelectOption)
    );

    return (
        <li
            className={twMerge(
                'mb-4 flex',
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
                        <Tooltip text={description}>
                            <QuestionMarkCircledIcon className="ml-1" />
                        </Tooltip>
                    )}
                </div>
            )}

            {register && controlType === 'INPUT_TEXT' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    type={hidden ? 'hidden' : 'text'}
                    {...register(`${path}.${name}`, {
                        required: required!,
                    })}
                />
            )}

            {!register && controlType === 'INPUT_TEXT' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label || name}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    name={name!}
                    title={type}
                    type={hidden ? 'hidden' : 'text'}
                />
            )}

            {controlType === 'INPUT_INTEGER' && (
                <Input
                    description={description}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label || name}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    name={name!}
                    onChange={({target}) => {
                        const {value} = target;

                        const integerOnlyRegex = /^[0-9\b]+$/;

                        if (value === '' || integerOnlyRegex.test(value)) {
                            setIntegerValue(value);
                        }
                    }}
                    title={type}
                    type={hidden ? 'hidden' : 'text'}
                    value={integerValue}
                />
            )}

            {controlType === 'INPUT_NUMBER' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label || name}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    name={name!}
                    title={type}
                    type={hidden ? 'hidden' : 'number'}
                />
            )}

            {controlType === 'INPUT_PASSWORD' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1"
                    key={name}
                    label={label || name}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    name={name!}
                    title={type}
                    type={hidden ? 'hidden' : 'password'}
                />
            )}

            {controlType === 'DATE' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label || name}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    name={name!}
                    title={type}
                    type={hidden ? 'hidden' : 'date'}
                />
            )}

            {controlType === 'DATE_TIME' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label || name}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    name={name!}
                    title={type}
                    type={hidden ? 'hidden' : 'datetime-local'}
                />
            )}

            {controlType === 'SELECT' && (
                <Select
                    description={description}
                    label={label}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    options={formattedOptions!}
                    triggerClassName="w-full border border-gray-300"
                />
            )}

            {controlType === 'CODE_EDITOR' && (
                <div className="h-full border-2">
                    <Editor
                        defaultValue="// Add your custom code here..."
                        language={actionName}
                        options={{
                            minimap: {
                                enabled: false,
                            },
                            lineNumbers: 'off',
                            tabSize: 2,
                            lineDecorationsWidth: 0,
                            lineNumbersMinChars: 0,
                            glyphMargin: false,
                            folding: false,
                            scrollBeyondLastLine: false,
                            scrollbar: {
                                horizontalScrollbarSize: 4,
                                verticalScrollbarSize: 4,
                            },
                            extraEditorClassName: 'code-editor',
                        }}
                    />
                </div>
            )}

            {controlType === 'SCHEMA_DESIGNER' && <span>Schema designer</span>}

            {!controlType && type === 'ONE_OF' && (
                <ul>
                    {(types as Array<PropertyType>).map(
                        ({controlType, type}, index) => (
                            <li
                                className="h-full rounded-md bg-gray-100 p-2"
                                key={`${controlType}_${type}_${index}`}
                            >
                                <span>
                                    {controlType} - {type}
                                </span>
                            </li>
                        )
                    )}
                </ul>
            )}

            {type === 'ARRAY' && <ArrayProperty property={property} />}

            {type === 'BOOLEAN' && (
                <Select
                    description={description}
                    label={label}
                    leadingIcon={TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    options={[
                        {value: 'true', label: 'True'},
                        {value: 'false', label: 'False'},
                    ]}
                    triggerClassName="w-full bg-gray-100 border-none"
                />
            )}

            {type === 'OBJECT' && <ObjectProperty property={property} />}
        </li>
    );
};

interface PropertiesProps {
    properties: Array<PropertyType>;
    customClassName?: string;
    actionName?: string;
    formState?: FormState<FieldValues>;
    register?: UseFormRegister<PropertyFormProps>;
}

const Properties = ({
    actionName,
    customClassName,
    formState,
    properties,
    register,
}: PropertiesProps): JSX.Element => (
    <ul className={twMerge('h-full', customClassName)}>
        {properties.map((property, index) => (
            <Property
                actionName={actionName}
                formState={formState}
                key={`${property.name}_${index}`}
                property={property}
                register={register}
            />
        ))}
    </ul>
);

export default Properties;
