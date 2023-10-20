/// <reference types="vite-plugin-svgr/client" />

import Editor from '@monaco-editor/react';
import Select, {ISelectOption} from 'components/Select/Select';
import {TagModel} from 'middleware/core/tag/models/TagModel';
import {useState} from 'react';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

import Input from '../Input/Input';

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
    actionName?: string;
    formState?: FormState<FieldValues>;
    path?: string;
    register?: UseFormRegister<PropertyFormProps>;
}

const Property = ({
    actionName,
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
        properties,
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
                'mb-3 flex w-full items-center',
                controlType === 'CODE_EDITOR' && 'h-5/6',
                type === 'OBJECT' && 'flex-col'
            )}
        >
            {type === 'OBJECT' && label && (
                <div className="flex w-full">
                    <span className={'self-start pb-2 pr-2'} title={type}>
                        {TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                    </span>

                    <span className="text-sm font-medium">{label}</span>
                </div>
            )}

            {type !== 'OBJECT' && (
                <span
                    className={twMerge(
                        'self-end pb-2 pr-2',
                        controlType === 'CODE_EDITOR' && 'self-start'
                    )}
                    title={type}
                >
                    {TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
                </span>
            )}

            {register && controlType === 'INPUT_TEXT' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="w-full mb-0"
                    key={name}
                    label={label}
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
                    fieldsetClassName="w-full mb-0"
                    key={name}
                    label={label || name}
                    name={name!}
                    type={hidden ? 'hidden' : 'text'}
                />
            )}

            {controlType === 'INPUT_INTEGER' && (
                <Input
                    description={description}
                    error={hasError(name!)}
                    fieldsetClassName="w-full mb-0"
                    key={name}
                    label={label || name}
                    name={name!}
                    onChange={({target}) => {
                        const {value} = target;

                        const integerOnlyRegex = /^[0-9\b]+$/;

                        if (value === '' || integerOnlyRegex.test(value)) {
                            setIntegerValue(value);
                        }
                    }}
                    value={integerValue}
                    type={hidden ? 'hidden' : 'text'}
                />
            )}

            {controlType === 'INPUT_NUMBER' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="w-full mb-0"
                    key={name}
                    label={label || name}
                    name={name!}
                    type={hidden ? 'hidden' : 'number'}
                />
            )}

            {controlType === 'INPUT_PASSWORD' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="w-full"
                    key={name}
                    label={label || name}
                    name={name!}
                    type={hidden ? 'hidden' : 'password'}
                />
            )}

            {controlType === 'DATE' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="w-full mb-0"
                    key={name}
                    label={label || name}
                    name={name!}
                    type={hidden ? 'hidden' : 'date'}
                />
            )}

            {controlType === 'SELECT' && (
                <Select
                    description={description}
                    label={label}
                    options={formattedOptions!}
                    triggerClassName="w-full bg-gray-100 border-none"
                />
            )}

            {controlType === 'CHECKBOX' && (
                <Select
                    description={description}
                    label={label}
                    options={[
                        {value: 'true', label: 'True'},
                        {value: 'false', label: 'False'},
                    ]}
                    triggerClassName="w-full bg-gray-100 border-none"
                />
            )}

            {controlType === 'CODE_EDITOR' && (
                <div className="h-full w-full border-2">
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

            {type === 'OBJECT' && (
                <ul className="w-full">
                    {properties?.map((subProperty) => (
                        <Property
                            key={subProperty.name}
                            property={subProperty}
                        />
                    ))}
                </ul>
            )}

            {type === 'ARRAY' && <span>array</span>}
        </li>
    );
};

interface PropertiesProps {
    properties: Array<PropertyType>;
    actionName?: string;
    formState?: FormState<FieldValues>;
    register?: UseFormRegister<PropertyFormProps>;
}

const Properties = ({
    actionName,
    formState,
    properties,
    register,
}: PropertiesProps): JSX.Element => (
    <ul className="h-full flex-[1_1_1px] overflow-auto p-4">
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
