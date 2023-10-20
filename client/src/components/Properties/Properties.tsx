/// <reference types="vite-plugin-svgr/client" />

import Editor from '@monaco-editor/react';
import Select, {ISelectOption} from 'components/Select/Select';
import {TagModel} from 'middleware/core/tag/models/TagModel';
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
                'flex w-full items-center space-x-2',
                controlType === 'CODE_EDITOR' && 'h-5/6'
            )}
        >
            <span
                className={twMerge(
                    controlType === 'CODE_EDITOR' && 'self-start'
                )}
                title={type}
            >
                {TYPE_ICONS[type as keyof typeof TYPE_ICONS]}
            </span>

            {register && controlType === 'INPUT_TEXT' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="w-full"
                    type={hidden ? 'hidden' : 'text'}
                    key={name}
                    label={label}
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
                    fieldsetClassName="w-full"
                    key={name}
                    label={label || name}
                    name={name!}
                    type={hidden ? 'hidden' : 'text'}
                />
            )}

            {controlType === 'INPUT_INTEGER' && (
                <Input
                    description={description}
                    defaultValue={defaultValue as string}
                    error={hasError(name!)}
                    fieldsetClassName="w-full"
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

            {controlType === 'SELECT' && (
                <Select
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

            {controlType === 'JSON_BUILDER' && <span>JSON builder</span>}

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

            {!controlType && type === 'ONE_OF' && (
                <ul className="space-y-2">
                    {(types as Array<PropertyType>).map(
                        ({controlType, type}, index) => (
                            <li
                                className="h-full space-y-2 rounded-md bg-gray-100 p-2"
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
    <ul className="h-full flex-[1_1_1px] space-y-2 overflow-auto p-4">
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
