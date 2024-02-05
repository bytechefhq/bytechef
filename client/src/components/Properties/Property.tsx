import PropertyMentionsInput from '@/components/Properties/components/PropMentionsInput/PropertyMentionsInput';
import PropertyInput from '@/components/Properties/components/PropertyInput/PropertyInput';
import PropertySelect, {ISelectOption} from '@/components/Properties/components/PropertySelect';
import PropertyTextArea from '@/components/Properties/components/PropertyTextArea';
import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import getInputHTMLType from '@/pages/automation/project/utils/getInputHTMLType';
import saveWorkflowDefinition from '@/pages/automation/project/utils/saveWorkflowDefinition';
import {PropertyType} from '@/types/projectTypes';
import {ComponentDataType, CurrentComponentType, DataPillType} from '@/types/types';
import Editor from '@monaco-editor/react';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import {FormInputIcon, FunctionSquareIcon} from 'lucide-react';
import {ChangeEvent, KeyboardEvent, useEffect, useRef, useState} from 'react';
import {FieldValues, FormState, UseFormRegister} from 'react-hook-form';
import ReactQuill from 'react-quill';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';

import ArrayProperty from './ArrayProperty';
import ObjectProperty from './ObjectProperty';

const INPUT_PROPERTY_CONTROL_TYPES = [
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
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
    workflow?: WorkflowModel;
}

const Property = ({
    actionName,
    currentComponent,
    currentComponentData,
    customClassName,
    dataPills,
    formState,
    path = 'parameters',
    property,
    register,
    updateWorkflowMutation,
    workflow,
}: PropertyProps) => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState('');
    const [mentionInput, setMentionInput] = useState(property.controlType !== 'SELECT');
    const [numericValue, setNumericValue] = useState('');

    const editorRef = useRef<ReactQuill>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const {setFocusedInput} = useWorkflowNodeDetailsPanelStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {componentData, setComponentData} = useWorkflowDataStore();

    let {name} = property;

    let defaultValue: string | undefined = property.defaultValue;

    const {
        controlType,
        description,
        hidden,
        items,
        label,
        maxLength,
        maxValue,
        minLength,
        minValue,
        options,
        properties,
        required,
        type,
    } = property;

    if (!name) {
        type === 'OBJECT' || type === 'ARRAY' ? (name = 'item') : <></>;
    }

    useEffect(() => {
        if (formState && name) {
            setHasError(
                formState.touchedFields[path] &&
                    formState.touchedFields[path]![name] &&
                    formState.errors[path] &&
                    (formState.errors[path] as never)[name]
            );
        }
    }, [formState, name, path]);

    const formattedOptions = options
        ?.map(({description, label, value}) => {
            if (value === '') {
                return null;
            }

            return {
                description,
                label,
                value,
            } as ISelectOption;
        })
        .filter((option) => option !== null);

    const isValidControlType = controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType);

    const isNumericalInput = type === 'INTEGER' || type === 'NUMBER';

    const typeIcon = TYPE_ICONS[type as keyof typeof TYPE_ICONS];

    const showMentionInput = mentionInput && !!dataPills?.length;

    let showInputTypeSwitchButton = type !== 'STRING' && !!dataPills?.length && !!name;

    if (controlType === 'SELECT') {
        showInputTypeSwitchButton = true;
    }

    if (controlType === 'FILE_ENTRY') {
        showInputTypeSwitchButton = false;
    }

    const currentWorkflowTask = workflow?.tasks?.find((task) => task.name === currentComponent?.workflowNodeName);

    const taskPropertyValue = name ? (currentWorkflowTask?.parameters?.[name] as unknown as string) : '';

    useEffect(() => {
        if (taskPropertyValue === undefined) {
            return;
        }

        isNumericalInput ? setNumericValue(taskPropertyValue || '') : setInputValue(taskPropertyValue || '');
    }, [isNumericalInput, taskPropertyValue]);

    const otherComponentData = componentData.filter((component) => {
        if (component.componentName !== currentComponent?.name) {
            return true;
        } else {
            currentComponentData = component;

            return false;
        }
    });

    if (actionName && name && currentComponentData?.parameters?.[name]) {
        defaultValue = currentComponentData?.parameters?.[name];
    }

    const handlePropertyChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (currentComponentData) {
            const {parameters} = currentComponentData;

            setComponentData([
                ...otherComponentData,
                {
                    ...currentComponentData,
                    parameters: {
                        ...parameters,
                        [event.target.name]: event.target.value,
                    },
                },
            ]);
        }
    };

    const handleSelectChange = (value: string, name: string | undefined) => {
        if (currentComponentData) {
            const {actionName, componentName, parameters, workflowNodeName} = currentComponentData;

            if (actionName) {
                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        parameters: {
                            ...parameters,
                            [name!]: value,
                        },
                    },
                ]);

                if (!workflow || !updateWorkflowMutation) {
                    return;
                }

                saveWorkflowDefinition(
                    {
                        actionName,
                        componentName,
                        name: workflowNodeName,
                        parameters: {
                            ...parameters,
                            [name as string]: value,
                        },
                    },
                    workflow,
                    updateWorkflowMutation
                );
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

    const handleInputBlur = () => {
        if (isNumericalInput) {
            const valueTooLow = minValue && parseFloat(numericValue) < minValue;
            const valueTooHigh = maxValue && parseFloat(numericValue) > maxValue;

            if (valueTooLow || valueTooHigh) {
                setHasError(true);

                setErrorMessage('Incorrect value');
            } else {
                setHasError(false);
            }
        } else {
            const valueTooShort = minLength && inputValue.length < minLength;
            const valueTooLong = maxLength && inputValue.length > maxLength;

            setHasError(!!valueTooShort || !!valueTooLong);

            setErrorMessage('Incorrect value');
        }

        if (!currentComponentData || !workflow || !updateWorkflowMutation) {
            return;
        }

        const {actionName, componentName, parameters, workflowNodeName} = currentComponentData;

        if (!name) {
            return;
        }

        saveWorkflowDefinition(
            {
                actionName,
                componentName,
                name: workflowNodeName,
                parameters: {
                    ...parameters,
                    [name as string]: isNumericalInput ? numericValue : inputValue,
                },
            },
            workflow,
            updateWorkflowMutation
        );
    };

    const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (isNumericalInput) {
            const {value} = event.target;

            const onlyNumericValue = type === 'NUMBER' ? value.replace(/[^0-9.-]/g, '') : value.replace(/\D/g, '');

            if (!onlyNumericValue) {
                return;
            }

            handlePropertyChange(event);

            setNumericValue(onlyNumericValue);
        } else {
            handlePropertyChange(event);

            setInputValue(event.target.value);
        }
    };

    if (type === 'OBJECT' && !properties?.length && !items?.length) {
        return <></>;
    }

    if (type === 'FILE_ENTRY' && !dataPills?.length) {
        return <></>;
    }

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
                        className="absolute right-0 top-0 size-auto p-0.5"
                        onClick={handleInputTypeSwitchButtonClick}
                        size="icon"
                        title="Switch input type"
                        variant="ghost"
                    >
                        {mentionInput ? (
                            <FormInputIcon className="size-5 text-gray-800" />
                        ) : (
                            <FunctionSquareIcon className="size-5 text-gray-800" />
                        )}
                    </Button>
                )}

                {showMentionInput && !!dataPills?.length && (
                    <PropertyMentionsInput
                        controlType={controlType}
                        currentComponent={currentComponent}
                        currentComponentData={currentComponentData}
                        dataPills={dataPills}
                        defaultValue={defaultValue}
                        description={description}
                        label={label || name}
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
                        updateWorkflowMutation={updateWorkflowMutation}
                        workflow={workflow}
                    />
                )}

                {!showMentionInput && (
                    <>
                        {(type === 'OBJECT' || type === 'ARRAY') && label && (
                            <div className="flex items-center py-2">
                                <span className="pr-2" title={type}>
                                    {typeIcon}
                                </span>

                                <span className="text-sm font-medium">{label}</span>

                                {description && (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <QuestionMarkCircledIcon className="ml-1" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-md">{description}</TooltipContent>
                                    </Tooltip>
                                )}
                            </div>
                        )}

                        {(type === 'ARRAY' || controlType === 'MULTI_SELECT') && (
                            <ArrayProperty dataPills={dataPills} property={property} />
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

                        {type === 'FILE_ENTRY' && !!dataPills?.length && (
                            <ObjectProperty
                                actionName={actionName}
                                currentComponent={currentComponent}
                                currentComponentData={currentComponentData}
                                dataPills={dataPills}
                                property={property}
                            />
                        )}

                        {register && isValidControlType && (
                            <PropertyInput
                                defaultValue={defaultValue}
                                description={description}
                                error={hasError}
                                key={name}
                                label={label}
                                leadingIcon={typeIcon}
                                required={required}
                                type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                {...register(`${path}.${name}`, {
                                    maxLength,
                                    minLength,
                                    required: required!,
                                })}
                            />
                        )}

                        {!register && isValidControlType && (
                            <PropertyInput
                                description={description}
                                error={hasError}
                                errorMessage={errorMessage}
                                key={name}
                                label={label || name}
                                leadingIcon={typeIcon}
                                max={maxValue}
                                maxLength={maxLength}
                                min={minValue}
                                minLength={minLength}
                                name={name!}
                                onBlur={handleInputBlur}
                                onChange={handleInputChange}
                                placeholder={
                                    isNumericalInput && minValue && maxValue ? `${minValue} - ${maxValue}` : ''
                                }
                                ref={inputRef}
                                required={required}
                                title={type}
                                type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                value={(isNumericalInput ? numericValue || defaultValue : defaultValue) || inputValue}
                            />
                        )}

                        {controlType === 'SELECT' && (
                            <PropertySelect
                                defaultValue={taskPropertyValue || defaultValue?.toString()}
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={(formattedOptions as Array<ISelectOption>) || undefined || []}
                            />
                        )}

                        {controlType === 'CODE_EDITOR' && (
                            <div className="size-full border-2">
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
                            <PropertyTextArea
                                description={description}
                                key={name}
                                label={label}
                                name={name!}
                                required={required}
                            />
                        )}

                        {register && type === 'BOOLEAN' && (
                            <PropertySelect
                                defaultValue={defaultValue?.toString()}
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
                                options={[
                                    {label: 'True', value: 'true'},
                                    {label: 'False', value: 'false'},
                                ]}
                                {...register(`${path}.${name}`, {
                                    required: required!,
                                })}
                            />
                        )}

                        {!register && type === 'BOOLEAN' && (
                            <PropertySelect
                                defaultValue={defaultValue?.toString()}
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
                                options={[
                                    {label: 'True', value: 'true'},
                                    {label: 'False', value: 'false'},
                                ]}
                            />
                        )}

                        {!controlType && type === 'ANY' && (
                            <span>
                                {controlType} - {type}
                            </span>
                        )}

                        {type === 'NULL' && <span>NULL</span>}

                        {type === 'DYNAMIC_PROPERTIES' && <span>Dynamic properties</span>}
                    </>
                )}
            </div>
        </li>
    );
};

export default Property;
