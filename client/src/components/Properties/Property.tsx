import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import useWorkflowDefinitionStore from '@/pages/automation/project/stores/useWorkflowDefinitionStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import getInputType from '@/pages/automation/project/utils/getInputType';
import {PropertyType} from '@/types/projectTypes';
import {
    ComponentDataType,
    CurrentComponentType,
    DataPillType,
} from '@/types/types';
import Editor from '@monaco-editor/react';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import Select, {ISelectOption} from 'components/Select/Select';
import TextArea from 'components/TextArea/TextArea';
import {FormInputIcon, FunctionSquareIcon} from 'lucide-react';
import {ChangeEvent, useEffect, useRef, useState} from 'react';
import {FieldValues, FormState, UseFormRegister} from 'react-hook-form';
import ReactQuill from 'react-quill';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';

import Input from '../Input/Input';
import MentionsInput from '../MentionsInput/MentionsInput';
import ArrayProperty from './ArrayProperty';
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
    currentComponent?: CurrentComponentType;
    currentComponentData?: ComponentDataType;
    customClassName?: string;
    dataPills?: DataPillType[];
    formState?: FormState<FieldValues>;
    mention?: boolean;
    path?: string;
    property: PropertyType;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register?: UseFormRegister<any>;
}

const Property = ({
    actionName,
    currentComponent,
    currentComponentData,
    customClassName,
    dataPills,
    formState,
    mention,
    path = 'parameters',
    property,
    register,
}: PropertyProps) => {
    const [mentionInput, setMentionInput] = useState(true);
    const [integerValue, setIntegerValue] = useState('');

    useEffect(() => {
        if (mention !== undefined) {
            setMentionInput(mention);
        }
    }, [mention]);

    const editorRef = useRef<ReactQuill>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const {setFocusedInput} = useWorkflowNodeDetailsPanelStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {componentData, setComponentData} = useWorkflowDefinitionStore();

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

    if (!name) {
        return <></>;
    }

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

    const isValidPropertyType =
        inputPropertyControlTypes.includes(controlType!) ||
        inputPropertyControlTypes.includes(type!);

    const isNumericalInput =
        getInputType(controlType) === 'number' ||
        type === 'INTEGER' ||
        type === 'NUMBER';

    const typeIcon = TYPE_ICONS[type as keyof typeof TYPE_ICONS];

    const showMentionInput =
        type !== 'OBJECT' &&
        type !== 'ARRAY' &&
        mentionInput &&
        !!dataPills?.length;

    const showInputTypeSwitchButton =
        type !== 'OBJECT' &&
        type !== 'ARRAY' &&
        type !== 'STRING' &&
        !!dataPills?.length &&
        !!name;

    const otherComponentData = componentData.filter((component) => {
        if (component.name !== currentComponent?.name) {
            return true;
        } else {
            currentComponentData = component;

            return false;
        }
    });

    let defaultValue = '';

    if (
        actionName &&
        property.name &&
        currentComponentData?.properties?.[actionName]
    ) {
        defaultValue =
            currentComponentData?.properties?.[actionName][property.name];
    }

    const handlePropertyChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (currentComponentData) {
            const {action, name, properties} = currentComponentData;

            if (action && properties) {
                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        name,
                        properties: {
                            ...properties,
                            [action]: {
                                ...properties[action],
                                [event.target.name]: event.target.value,
                            },
                        },
                    },
                ]);
            }
        }
    };

    const handleInputTypeSwitchButtonClick = () => {
        setMentionInput(!mentionInput);

        if (mentionInput) {
            setTimeout(() => {
                if (inputRef.current) {
                    inputRef.current.value = '';

                    inputRef.current.focus();
                }
            }, 50);
        } else {
            setTimeout(() => {
                setFocusedInput(editorRef.current);

                editorRef.current?.focus();

                setDataPillPanelOpen(true);
            }, 50);
        }
    };

    return (
        <li
            className={twMerge(
                controlType === 'CODE_EDITOR' && 'h-5/6',
                hidden && 'mb-0',
                type === 'OBJECT' && 'flex-col',
                type === 'ARRAY' && 'flex-col',
                customClassName
            )}
        >
            <div className="relative w-full">
                {showInputTypeSwitchButton && (
                    <Button
                        className="absolute right-0 top-0 h-auto w-auto p-0.5"
                        onClick={handleInputTypeSwitchButtonClick}
                        size="icon"
                        title="Switch input type"
                        variant="ghost"
                    >
                        {mentionInput ? (
                            <FormInputIcon className="h-5 w-5 text-gray-800" />
                        ) : (
                            <FunctionSquareIcon className="h-5 w-5 text-gray-800" />
                        )}
                    </Button>
                )}

                {showMentionInput && (
                    <MentionsInput
                        controlType={controlType || getInputType(controlType)}
                        dataPills={dataPills}
                        defaultValue={defaultValue}
                        description={description}
                        label={label}
                        leadingIcon={typeIcon}
                        name={name}
                        onChange={handlePropertyChange}
                        onKeyPress={(event: KeyboardEvent) => {
                            if (isNumericalInput) {
                                event.key !== '{' && event.preventDefault();
                            }
                        }}
                        ref={editorRef}
                        required={required}
                        singleMention={controlType !== 'TEXT'}
                    />
                )}

                {!showMentionInput && (
                    <>
                        {(type === 'OBJECT' || type === 'ARRAY') && label && (
                            <div className="flex items-center py-2">
                                <span className="pr-2" title={type}>
                                    {typeIcon}
                                </span>

                                <span className="text-sm font-medium">
                                    {label}
                                </span>

                                {description && (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <QuestionMarkCircledIcon className="ml-1" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-md">
                                            {description}
                                        </TooltipContent>
                                    </Tooltip>
                                )}
                            </div>
                        )}

                        {(type === 'ARRAY' ||
                            controlType === 'MULTI_SELECT') && (
                            <ArrayProperty
                                dataPills={dataPills}
                                property={property}
                            />
                        )}

                        {type === 'OBJECT' && (
                            <ObjectProperty
                                actionName={actionName}
                                currentComponent={currentComponent}
                                currentComponentData={currentComponentData}
                                dataPills={dataPills}
                                property={property}
                            />
                        )}

                        {register && isValidPropertyType && (
                            <Input
                                defaultValue={defaultValue}
                                description={description}
                                error={hasError(name!)}
                                fieldsetClassName="flex-1 mb-0"
                                key={name}
                                label={label}
                                leadingIcon={typeIcon}
                                required={required}
                                type={hidden ? 'hidden' : 'text'}
                                {...register(`${path}.${name}`, {
                                    required: required!,
                                })}
                            />
                        )}

                        {!register && isValidPropertyType && (
                            <Input
                                description={description}
                                error={hasError(name!)}
                                fieldsetClassName="flex-1 mb-0"
                                key={name}
                                label={label || name}
                                leadingIcon={typeIcon}
                                name={name!}
                                onChange={(event) => {
                                    handlePropertyChange(event);

                                    if (isNumericalInput) {
                                        const {value} = event.target;

                                        const integerOnlyRegex = /^[0-9\b]+$/;

                                        if (
                                            value === '' ||
                                            integerOnlyRegex.test(value)
                                        ) {
                                            setIntegerValue(value);
                                        }
                                    }
                                }}
                                ref={inputRef}
                                required={required}
                                title={type}
                                type={
                                    hidden
                                        ? 'hidden'
                                        : getInputType(controlType)
                                }
                                value={
                                    isNumericalInput
                                        ? integerValue || defaultValue
                                        : defaultValue
                                }
                            />
                        )}

                        {controlType === 'SELECT' &&
                            !!formattedOptions?.length && (
                                <Select
                                    description={description}
                                    label={label}
                                    leadingIcon={typeIcon}
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
                                label={label}
                                name={name!}
                                required={required}
                            />
                        )}

                        {register && type === 'BOOLEAN' && (
                            <Select
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
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

                        {!register && type === 'BOOLEAN' && (
                            <Select
                                description={description}
                                fieldsetClassName="mt-2"
                                label={label}
                                leadingIcon={typeIcon}
                                options={[
                                    {label: 'True', value: 'true'},
                                    {label: 'False', value: 'false'},
                                ]}
                                triggerClassName="w-full border border-gray-300"
                            />
                        )}

                        {controlType === 'SCHEMA_DESIGNER' && (
                            <span>Schema designer</span>
                        )}

                        {!controlType && type === 'ANY' && (
                            <span>
                                {controlType} - {type}
                            </span>
                        )}

                        {type === 'NULL' && <span>NULL</span>}

                        {type === 'DYNAMIC_PROPERTIES' && (
                            <span>Dynamic properties</span>
                        )}
                    </>
                )}
            </div>
        </li>
    );
};

export default Property;
