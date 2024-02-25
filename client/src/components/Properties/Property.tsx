import PropertyComboBox from '@/components/Properties/components/PropertyComboBox';
import PropertyDynamicProperties from '@/components/Properties/components/PropertyDynamicProperties';
import PropertyInput from '@/components/Properties/components/PropertyInput/PropertyInput';
import PropertyMentionsInput from '@/components/Properties/components/PropertyMentionsInput/PropertyMentionsInput';
import PropertySelect from '@/components/Properties/components/PropertySelect';
import PropertyTextArea from '@/components/Properties/components/PropertyTextArea';
import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {OptionModel, WorkflowModel} from '@/middleware/platform/configuration';
import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import getInputHTMLType from '@/pages/automation/project/utils/getInputHTMLType';
import saveWorkflowDefinition from '@/pages/automation/project/utils/saveWorkflowDefinition';
import {useEvaluateWorkflowNodeDisplayConditionQuery} from '@/queries/platform/workflowNodeDisplayConditions.queries';
import {ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import Editor from '@monaco-editor/react';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import {FormInputIcon, FunctionSquareIcon} from 'lucide-react';
import {ChangeEvent, KeyboardEvent, useEffect, useRef, useState} from 'react';
import {FieldValues, FormState, UseFormRegister} from 'react-hook-form';
import ReactQuill from 'react-quill';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';

import {SelectOptionType} from '../CreatableSelect/CreatableSelect';
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
    arrayName?: string;
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
}

const Property = ({
    actionName,
    arrayName,
    currentComponent,
    currentComponentData,
    customClassName,
    dataPills,
    formState,
    path = 'parameters',
    property,
    register,
    updateWorkflowMutation,
}: PropertyProps) => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState(property.defaultValue || '');
    const [mentionInput, setMentionInput] = useState(property.controlType !== 'SELECT');
    const [numericValue, setNumericValue] = useState(property.defaultValue || '');

    const editorRef = useRef<ReactQuill>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const {currentNode, setFocusedInput} = useWorkflowNodeDetailsPanelStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {componentData, setComponentData, workflow} = useWorkflowDataStore();

    let {name} = property;

    let defaultValue: string | undefined = property.defaultValue;

    const {
        additionalProperties,
        controlType,
        description,
        hidden,
        label,
        maxLength,
        maxValue,
        minLength,
        minValue,
        options,
        optionsDataSource,
        properties,
        required,
        type,
    } = property;

    if (!name) {
        controlType === 'OBJECT_BUILDER' || controlType === 'ARRAY_BUILDER' ? (name = 'item') : <></>;
    }

    const formattedOptions = options
        ?.map((option) => {
            if (option.value === '') {
                return null;
            }

            return option;
        })
        .filter((option) => option !== null);

    const isValidControlType = controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType);

    const isNumericalInput = controlType === 'INTEGER' || controlType === 'NUMBER';

    const typeIcon = TYPE_ICONS[type as keyof typeof TYPE_ICONS];

    let showMentionInput = controlType === 'FILE_ENTRY' || (mentionInput && !!dataPills?.length);

    if (controlType === 'OBJECT_BUILDER' && !!properties?.length) {
        showMentionInput = false;
    }

    let showInputTypeSwitchButton = type !== 'STRING' && !!dataPills?.length && !!name;

    if (controlType === 'FILE_ENTRY') {
        showInputTypeSwitchButton = false;
    }

    const currentWorkflowTask = workflow?.tasks?.find((task) => task.name === currentComponent?.workflowNodeName);

    let taskParameterValue = name ? (currentWorkflowTask?.parameters?.[name] as unknown as string) : '';

    if (name && name.endsWith('_0') && defaultValue) {
        taskParameterValue = defaultValue;
    }

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

    const {data: displayCondition, isLoading: isDisplayConditionLoading} = useEvaluateWorkflowNodeDisplayConditionQuery(
        {
            evaluateWorkflowNodeDisplayConditionRequestModel: {
                displayCondition: property.displayCondition!,
            },
            id: workflow.id!,
            workflowNodeName: currentNode.name!,
        },
        !!property.displayCondition
    );

    const handlePropertyChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
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
    }, 200);

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

            if (onlyNumericValue === undefined) {
                return;
            }

            handlePropertyChange(event);

            setNumericValue(onlyNumericValue);
        } else {
            handlePropertyChange(event);

            setInputValue(event.target.value);
        }
    };

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

    useEffect(() => {
        if (taskParameterValue === undefined) {
            return;
        }

        isNumericalInput ? setNumericValue(taskParameterValue || '') : setInputValue(taskParameterValue || '');
    }, [defaultValue, isNumericalInput, taskParameterValue]);

    if (controlType === 'OBJECT_BUILDER' && !properties?.length && !additionalProperties?.length) {
        return <></>;
    }

    if (displayCondition === false || (property.displayCondition && isDisplayConditionLoading)) {
        return <></>;
    }

    return (
        <li
            className={twMerge(
                controlType === 'CODE_EDITOR' && 'h-5/6',
                hidden && 'mb-0',
                controlType === 'OBJECT_BUILDER' && 'flex-col',
                controlType === 'ARRAY_BUILDER' && 'flex-col',
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
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <FormInputIcon className="size-5 text-gray-800" />
                                </TooltipTrigger>

                                <TooltipContent>Switch to constant value</TooltipContent>
                            </Tooltip>
                        ) : (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <FunctionSquareIcon className="size-5 text-gray-800" />
                                </TooltipTrigger>

                                <TooltipContent>Switch to dynamic value</TooltipContent>
                            </Tooltip>
                        )}
                    </Button>
                )}

                {showMentionInput && (controlType === 'FILE_ENTRY' || !!dataPills?.length) && (
                    <PropertyMentionsInput
                        arrayName={arrayName}
                        controlType={controlType}
                        currentComponent={currentComponent}
                        currentComponentData={currentComponentData}
                        dataPills={dataPills}
                        defaultValue={defaultValue}
                        description={description}
                        label={label || (arrayName ? undefined : name)}
                        leadingIcon={typeIcon}
                        name={name || `${arrayName}_0`}
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
                        {(controlType === 'OBJECT_BUILDER' || controlType === 'ARRAY_BUILDER') && label && (
                            <div className="flex items-center py-2">
                                <span className="pr-2" title={type}>
                                    {typeIcon}
                                </span>

                                <Label>{label}</Label>

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

                        {(controlType === 'ARRAY_BUILDER' || controlType === 'MULTI_SELECT') && (
                            <ArrayProperty
                                currentComponentData={currentComponentData}
                                dataPills={dataPills}
                                property={property}
                                updateWorkflowMutation={updateWorkflowMutation}
                            />
                        )}

                        {controlType === 'OBJECT_BUILDER' && (
                            <ObjectProperty
                                actionName={actionName}
                                currentComponent={currentComponent}
                                currentComponentData={currentComponentData}
                                dataPills={dataPills}
                                property={property}
                                updateWorkflowMutation={updateWorkflowMutation}
                            />
                        )}

                        {type === 'FILE_ENTRY' && (
                            <ObjectProperty
                                actionName={actionName}
                                currentComponent={currentComponent}
                                currentComponentData={currentComponentData}
                                dataPills={dataPills}
                                property={property}
                            />
                        )}

                        {register && (isValidControlType || isNumericalInput) && (
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

                        {!register && (isValidControlType || isNumericalInput) && (
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
                                name={name || `${arrayName}_0`}
                                onBlur={handleInputBlur}
                                onChange={handleInputChange}
                                placeholder={
                                    isNumericalInput && minValue && maxValue ? `From ${minValue} to ${maxValue}` : ''
                                }
                                ref={inputRef}
                                required={required}
                                title={type}
                                type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                value={isNumericalInput ? numericValue : inputValue}
                            />
                        )}

                        {!register && (isValidControlType || isNumericalInput) && !!options?.length && (
                            <PropertySelect
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={options as Array<SelectOptionType>}
                                value={taskParameterValue || defaultValue?.toString()}
                            />
                        )}

                        {!register && (isValidControlType || isNumericalInput) && !!optionsDataSource && (
                            <PropertyComboBox
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={(formattedOptions as Array<OptionModel>) || undefined || []}
                                optionsDataSource={optionsDataSource}
                                value={taskParameterValue || defaultValue?.toString()}
                            />
                        )}

                        {controlType === 'SELECT' && type !== 'BOOLEAN' && (
                            <PropertyComboBox
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={(formattedOptions as Array<OptionModel>) || undefined || []}
                                optionsDataSource={optionsDataSource}
                                value={taskParameterValue || defaultValue?.toString()}
                            />
                        )}

                        {controlType === 'SELECT' && type === 'BOOLEAN' && (
                            <PropertySelect
                                defaultValue={defaultValue?.toString()}
                                description={description}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={[
                                    {label: 'True', value: 'true'},
                                    {label: 'False', value: 'false'},
                                ]}
                                value={taskParameterValue}
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
                                leadingIcon={typeIcon}
                                name={name!}
                                required={required}
                            />
                        )}

                        {type === 'NULL' && <span>NULL</span>}

                        {type === 'DYNAMIC_PROPERTIES' && (
                            <PropertyDynamicProperties
                                name={name}
                                propertiesDataSource={property.propertiesDataSource}
                            />
                        )}
                    </>
                )}
            </div>
        </li>
    );
};

export default Property;
