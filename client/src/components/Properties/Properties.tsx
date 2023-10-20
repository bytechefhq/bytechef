/// <reference types="vite-plugin-svgr/client" />

import Editor from '@monaco-editor/react';
import Select, {ISelectOption} from 'components/Select/Select';
import {TagModel} from 'middleware/core/tag/models/TagModel';
import {FieldValues} from 'react-hook-form/dist/types';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import {PropertyType} from 'types/projectTypes';

import {ReactComponent as ArrayIcon} from '../../assets/array.svg';
import {ReactComponent as BooleanIcon} from '../../assets/boolean.svg';
import {ReactComponent as DateIcon} from '../../assets/date.svg';
import {ReactComponent as DateTimeIcon} from '../../assets/datetime.svg';
import {ReactComponent as DynamicIcon} from '../../assets/dynamic.svg';
import {ReactComponent as IntegerIcon} from '../../assets/integer.svg';
import {ReactComponent as NullIcon} from '../../assets/null.svg';
import {ReactComponent as NumberIcon} from '../../assets/number.svg';
import {ReactComponent as ObjectIcon} from '../../assets/object.svg';
import {ReactComponent as OneOfIcon} from '../../assets/oneof.svg';
import {ReactComponent as StringIcon} from '../../assets/string.svg';
import {ReactComponent as TimeIcon} from '../../assets/time.svg';
import Input from '../Input/Input';

const TYPE_ICONS = {
    ARRAY: <ArrayIcon className="h-5 w-5 text-gray-600" />,
    BOOLEAN: <BooleanIcon className="h-5 w-5 text-gray-600" />,
    DATE: <DateIcon className="h-5 w-5 text-gray-600" />,
    DATE_TIME: <DateTimeIcon className="h-5 w-5 text-gray-600" />,
    DYNAMIC_PROPERTIES: <DynamicIcon className="h-5 w-5 text-gray-600" />,
    INTEGER: <IntegerIcon className="h-5 w-5 text-gray-600" />,
    NUMBER: <NumberIcon className="h-5 w-5 text-gray-600" />,
    NULL: <NullIcon className="h-5 w-5 text-gray-600" />,
    OBJECT: <ObjectIcon className="h-5 w-5 text-gray-600" />,
    ONE_OF: <OneOfIcon className="h-5 w-5 text-gray-600" />,
    STRING: <StringIcon className="h-5 w-5 text-gray-600" />,
    TIME: <TimeIcon className="h-5 w-5 text-gray-600" />,
};

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
        (option) =>
            ({
                label: option.name,
                value: option.name,
            } as ISelectOption)
    );

    return (
        <li className="flex w-full items-center space-x-2">
            <span title={type}>
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
                <Editor
                    language={actionName}
                    height="40vh"
                    defaultValue="// Add your custom code here..."
                />
            )}

            {!controlType && type === 'ONE_OF' && (
                <ul className="space-y-2">
                    {(types as Array<PropertyType>).map(
                        ({controlType, type}, index) => (
                            <li
                                className="space-y-2 rounded-md bg-gray-100 p-2"
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
    <ul className="mb-4 space-y-2">
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
