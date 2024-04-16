import InputTypeSwitchButton from '@/components/Properties/components/InputTypeSwitchButton';
import PropertyCodeEditor from '@/components/Properties/components/PropertyCodeEditor/PropertyCodeEditor';
import PropertyComboBox from '@/components/Properties/components/PropertyComboBox';
import PropertyDynamicProperties from '@/components/Properties/components/PropertyDynamicProperties';
import PropertyInput from '@/components/Properties/components/PropertyInput/PropertyInput';
import PropertyMentionsInput from '@/components/Properties/components/PropertyMentionsInput/PropertyMentionsInput';
import PropertySelect from '@/components/Properties/components/PropertySelect';
import PropertyTextArea from '@/components/Properties/components/PropertyTextArea';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {OptionModel} from '@/middleware/platform/configuration';
import {
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/mutations/platform/workflowNodeParameters.mutations';
import {useDataPillPanelStore} from '@/pages/automation/project/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/automation/project/utils/deleteProperty';
import getInputHTMLType from '@/pages/automation/project/utils/getInputHTMLType';
import saveProperty from '@/pages/automation/project/utils/saveProperty';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import {ComponentType, CurrentComponentDefinitionType, DataPillType, PropertyType} from '@/types/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
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
    arrayIndex?: number;
    arrayName?: string;
    currentComponentDefinition?: CurrentComponentDefinitionType;
    currentComponent?: ComponentType;
    customClassName?: string;
    dataPills?: DataPillType[];
    formState?: FormState<FieldValues>;
    inputTypeSwitchButtonClassName?: string;
    objectName?: string;
    path?: string;
    property: PropertyType;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register?: UseFormRegister<any>;
    showDeletePropertyButton?: boolean;
    taskParameterValue?: any;
}

const Property = ({
    actionName,
    arrayIndex,
    arrayName,
    currentComponent,
    currentComponentDefinition,
    customClassName,
    dataPills,
    formState,
    inputTypeSwitchButtonClassName,
    objectName,
    path = 'parameters',
    property,
    register,
    showDeletePropertyButton = false,
    taskParameterValue,
}: PropertyProps) => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState(property.defaultValue || '');
    const [loadDependsOnValues, setLoadDependsOnValues] = useState<Array<string> | undefined>();
    const [mentionInputValue, setMentionInputValue] = useState(property.defaultValue || '');
    const [mentionInput, setMentionInput] = useState(!formState && property.controlType !== 'SELECT');
    const [numericValue, setNumericValue] = useState(property.defaultValue || '');
    const [selectValue, setSelectValue] = useState(property.defaultValue || '');

    const editorRef = useRef<ReactQuill>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const {currentNode, setFocusedInput} = useWorkflowNodeDetailsPanelStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {componentDefinitions, components, setComponents, setDirty, workflow} = useWorkflowDataStore();

    const defaultValue = property.defaultValue || '';

    const {
        controlType,
        description,
        hidden,
        label,
        languageId,
        maxLength,
        maxValue,
        minLength,
        minValue,
        name,
        options,
        optionsDataSource,
        properties,
        propertiesDataSource,
        required,
        type,
    } = property;

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

    const showMentionInput = controlType === 'FILE_ENTRY' || mentionInput;

    let showInputTypeSwitchButton = type !== 'STRING' && !!name && property.expressionEnabled;

    if (controlType === 'FILE_ENTRY') {
        showInputTypeSwitchButton = false;
    }

    if (controlType === 'SELECT') {
        showInputTypeSwitchButton = true;
    }

    const currentWorkflowTask = workflow?.tasks?.find(
        (task) => task.name === currentComponentDefinition?.workflowNodeName
    );

    if (!taskParameterValue) {
        taskParameterValue = name ? (currentWorkflowTask?.parameters?.[name] as unknown as string) : '';
    }

    if (name && name.endsWith('_0') && defaultValue) {
        taskParameterValue = defaultValue;
    }

    const otherComponents = components.filter((component) => {
        if (component.componentName !== currentComponentDefinition?.name) {
            return true;
        } else {
            currentComponent = component;

            return false;
        }
    });

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () =>
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            }),
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: () =>
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            }),
    });

    const saveInputValue = useDebouncedCallback(() => {
        if (!currentComponent || !workflow) {
            return;
        }

        if (!name) {
            return;
        }

        const numericValueToSave = controlType === 'NUMBER' ? parseFloat(numericValue) : parseInt(numericValue, 10);

        saveProperty(
            workflow.id!,
            path,
            name,
            currentComponent,
            otherComponents,
            setComponents,
            setDirty,
            updateWorkflowNodeParameterMutation,
            isNumericalInput ? numericValueToSave : inputValue,
            arrayIndex
        );
    }, 200);

    const saveMentionInputValue = useDebouncedCallback(() => {
        if (!currentComponent || !workflow || !updateWorkflowNodeParameterMutation || !name) {
            return;
        }

        const {parameters} = currentComponent;

        if (!parameters) {
            return;
        }
        let strippedValue = mentionInputValue;

        const dataPillValue = mentionInputValue.match(/data-value="([^"]+)"/)?.[1];

        if (dataPillValue && !dataPillValue.startsWith('${') && !dataPillValue.endsWith('}')) {
            strippedValue = `\${${dataPillValue.replace(/\//g, '.')}}`;
        } else {
            strippedValue = mentionInputValue.replace(/<[^>]*>?/gm, '');
        }

        let currentValue;

        if (arrayName && arrayIndex !== undefined) {
            if (path?.includes('parameters')) {
                if (objectName) {
                    currentValue = parameters?.[arrayName]?.[arrayIndex]?.[name];
                } else {
                    currentValue = parameters?.[arrayName]?.[arrayIndex];
                }
            } else {
                currentValue = parameters[arrayName];
            }
        } else if (objectName && parameters && path) {
            const matchingObject = path.split('.').reduce((acc, key) => {
                if (key !== 'parameters') {
                    if (acc && acc[key] === undefined) {
                        acc[key] = {};
                    }

                    return acc && acc[key];
                } else {
                    return acc;
                }
            }, parameters);

            if (matchingObject) {
                currentValue = matchingObject[name as string];
            }
        } else {
            currentValue = parameters[name as string];
        }

        if (currentValue === strippedValue) {
            return;
        }

        saveProperty(
            workflow.id!,
            path,
            name,
            currentComponent,
            otherComponents,
            setComponents,
            setDirty,
            updateWorkflowNodeParameterMutation,
            strippedValue,
            arrayIndex
        );
    }, 200);

    const handleCodeEditorChange = useDebouncedCallback((value?: string) => {
        if (!currentComponent || !name) {
            return;
        }

        saveProperty(
            workflow.id!,
            path,
            name,
            currentComponent,
            otherComponents,
            setComponents,
            setDirty,
            updateWorkflowNodeParameterMutation,
            value,
            undefined
        );
    }, 200);

    const handleDelete = (path: string, name: string, arrayIndex?: number) => {
        deleteProperty(
            workflow.id!,
            path,
            name,
            currentComponent!,
            otherComponents,
            setComponents,
            setDirty,
            deleteWorkflowNodeParameterMutation,
            arrayIndex
        );
    };

    const handleInputChange = (event: ChangeEvent<HTMLInputElement> | ChangeEvent<HTMLTextAreaElement>) => {
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

        if (isNumericalInput) {
            const {value} = event.target;

            const onlyNumericValue = type === 'NUMBER' ? value.replace(/[^0-9.-]/g, '') : value.replace(/\D/g, '');

            if (onlyNumericValue === undefined) {
                return;
            }

            setNumericValue(onlyNumericValue);
        } else {
            setInputValue(event.target.value);
        }

        saveInputValue();
    };

    const handleMentionsInputChange = (value: string) => {
        setMentionInputValue(value);

        saveMentionInputValue();
    };

    const handleInputTypeSwitchButtonClick = () => {
        setMentionInput(!mentionInput);
        setNumericValue('');
        setInputValue('');
        setMentionInputValue('');

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

    const handleSelectChange = (value: string, name: string) => {
        if (!currentComponent || !workflow || !name) {
            return;
        }

        setSelectValue(value);

        saveProperty(
            workflow.id!,
            path,
            name,
            currentComponent,
            otherComponents,
            setComponents,
            setDirty,
            updateWorkflowNodeParameterMutation,
            value,
            arrayIndex
        );
    };

    // set default mentionInput state
    useEffect(() => {
        if (controlType === 'ARRAY_BUILDER') {
            setMentionInput(false);
        }

        if (controlType === 'OBJECT_BUILDER') {
            setMentionInput(false);
        }
    }, [controlType, properties?.length]);

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

    // set value to taskParameterValue only on initial render
    useEffect(() => {
        if (mentionInput && taskParameterValue) {
            const mentionInputElement = editorRef.current?.getEditor().getModule('mention');

            if (!mentionInputElement) {
                return;
            }

            if (typeof taskParameterValue === 'string' && taskParameterValue.startsWith('${')) {
                const componentName = taskParameterValue.split('_')[0].replace('${', '');

                const componentIcon =
                    componentDefinitions.find((component) => component.name === componentName)?.icon || '📄';

                const node = document.createElement('div');

                node.className = 'property-mention';

                node.dataset.value = taskParameterValue.replace(/\$\{|\}/g, '');
                node.dataset.componentIcon = componentIcon;

                setMentionInputValue(node.outerHTML);
            } else {
                setMentionInputValue(taskParameterValue);
            }
        }

        if (inputValue === '' && taskParameterValue) {
            setInputValue(taskParameterValue);
        }

        if (selectValue === '' && taskParameterValue) {
            setSelectValue(taskParameterValue);
        }

        if (numericValue === '' && taskParameterValue) {
            setNumericValue(taskParameterValue);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (optionsDataSource?.loadOptionsDependsOn) {
            const loadDependsOnValues = optionsDataSource?.loadOptionsDependsOn.map(
                (key) => currentComponent?.parameters?.[key]
            );

            setLoadDependsOnValues(loadDependsOnValues);
        }
    }, [currentComponent?.parameters, optionsDataSource?.loadOptionsDependsOn]);

    useEffect(() => {
        if (propertiesDataSource?.loadPropertiesDependsOn) {
            const loadDependsOnValues = propertiesDataSource?.loadPropertiesDependsOn.map(
                (key) => currentComponent?.parameters?.[key]
            );

            setLoadDependsOnValues(loadDependsOnValues);
        }
    }, [currentComponent?.parameters, propertiesDataSource?.loadPropertiesDependsOn]);

    if (property.displayCondition && !currentComponent?.displayConditions?.[property.displayCondition]) {
        return <></>;
    }

    if (!name) {
        return <></>;
    }

    return (
        <li
            className={twMerge(
                hidden && 'mb-0',
                controlType === 'OBJECT_BUILDER' && 'flex-col',
                controlType === 'ARRAY_BUILDER' && 'flex-col',
                customClassName
            )}
        >
            <div className="w-full">
                {showMentionInput &&
                    currentComponent &&
                    currentComponentDefinition &&
                    type !== 'DYNAMIC_PROPERTIES' &&
                    controlType !== 'CODE_EDITOR' && (
                        <PropertyMentionsInput
                            arrayIndex={arrayIndex}
                            controlType={controlType}
                            currentComponentDefinition={currentComponentDefinition}
                            dataPills={dataPills}
                            defaultValue={defaultValue}
                            description={description}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            inputTypeSwitchButtonClassName={inputTypeSwitchButtonClassName}
                            label={label || (arrayName ? undefined : name)}
                            leadingIcon={typeIcon}
                            onChange={handleMentionsInputChange}
                            onKeyPress={(event: KeyboardEvent) => {
                                if (isNumericalInput || type === 'BOOLEAN') {
                                    event.key !== '{' && event.preventDefault();
                                }
                            }}
                            ref={editorRef}
                            required={required}
                            showInputTypeSwitchButton={showInputTypeSwitchButton!}
                            singleMention={controlType !== 'TEXT'}
                            value={mentionInputValue}
                        />
                    )}

                {!showMentionInput && (
                    <>
                        {(controlType === 'OBJECT_BUILDER' || controlType === 'ARRAY_BUILDER') && (
                            <div className="flex items-center pb-2">
                                {typeIcon && (
                                    <span className={twMerge(label ? 'pr-2' : 'pr-1')} title={type}>
                                        {typeIcon}
                                    </span>
                                )}

                                {(label || description || showInputTypeSwitchButton) && (
                                    <div className="flex w-full items-center justify-between">
                                        <div className="flex items-center">
                                            {label && <Label className="leading-normal">{label}</Label>}

                                            {description && (
                                                <Tooltip>
                                                    <TooltipTrigger>
                                                        <QuestionMarkCircledIcon className="ml-1" />
                                                    </TooltipTrigger>

                                                    <TooltipContent className="max-w-md">{description}</TooltipContent>
                                                </Tooltip>
                                            )}
                                        </div>

                                        {showInputTypeSwitchButton && (
                                            <InputTypeSwitchButton
                                                className={
                                                    showDeletePropertyButton
                                                        ? inputTypeSwitchButtonClassName
                                                        : undefined
                                                }
                                                handleClick={handleInputTypeSwitchButtonClick}
                                                mentionInput={mentionInput}
                                            />
                                        )}
                                    </div>
                                )}
                            </div>
                        )}

                        {(controlType === 'ARRAY_BUILDER' || controlType === 'MULTI_SELECT') && (
                            <ArrayProperty
                                currentComponent={currentComponent}
                                currentComponentDefinition={currentComponentDefinition}
                                dataPills={dataPills}
                                onDeleteClick={handleDelete}
                                path={path}
                                property={property}
                            />
                        )}

                        {controlType === 'OBJECT_BUILDER' && (
                            <ObjectProperty
                                actionName={actionName}
                                arrayIndex={arrayIndex}
                                arrayName={arrayName}
                                currentComponent={currentComponent}
                                currentComponentDefinition={currentComponentDefinition}
                                dataPills={dataPills}
                                onDeleteClick={handleDelete}
                                path={path}
                                property={property}
                                taskParameterValue={taskParameterValue}
                            />
                        )}

                        {type === 'FILE_ENTRY' && (
                            <ObjectProperty
                                actionName={actionName}
                                currentComponent={currentComponent}
                                currentComponentDefinition={currentComponentDefinition}
                                dataPills={dataPills}
                                property={property}
                            />
                        )}

                        {register && (isValidControlType || isNumericalInput) && (
                            <PropertyInput
                                defaultValue={defaultValue}
                                description={description}
                                error={hasError}
                                key={`${currentNode.name}_${name}`}
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
                                handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                                inputTypeSwitchButtonClassName={inputTypeSwitchButtonClassName}
                                key={`${currentNode.name}_${name}`}
                                label={label || name}
                                leadingIcon={typeIcon}
                                max={maxValue}
                                maxLength={maxLength}
                                min={minValue}
                                minLength={minLength}
                                name={`${path}.${name}` || name || `${arrayName}_0`}
                                onChange={handleInputChange}
                                placeholder={
                                    isNumericalInput && minValue && maxValue ? `From ${minValue} to ${maxValue}` : ''
                                }
                                ref={inputRef}
                                required={required}
                                showInputTypeSwitchButton={showInputTypeSwitchButton}
                                title={type}
                                type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                value={isNumericalInput ? numericValue : inputValue}
                            />
                        )}

                        {!register && (isValidControlType || isNumericalInput) && !!options?.length && (
                            <PropertySelect
                                description={description}
                                key={`${currentNode.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={options as Array<SelectOptionType>}
                                value={selectValue}
                            />
                        )}

                        {!register && (isValidControlType || isNumericalInput) && !!optionsDataSource && (
                            <PropertyComboBox
                                currentNodeConnectionId={currentNode.connectionId}
                                description={description}
                                key={`${currentNode.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                loadDependsOnValues={loadDependsOnValues}
                                name={objectName ? `${objectName}.${name}` : name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={(formattedOptions as Array<OptionModel>) || undefined || []}
                                optionsDataSource={optionsDataSource}
                                required={required}
                                value={selectValue}
                            />
                        )}

                        {controlType === 'SELECT' && type !== 'BOOLEAN' && (
                            <PropertyComboBox
                                currentNodeConnectionId={currentNode.connectionId}
                                description={description}
                                key={`${currentNode.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                loadDependsOnValues={loadDependsOnValues}
                                name={objectName ? `${objectName}.${name}` : name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={(formattedOptions as Array<OptionModel>) || undefined || []}
                                optionsDataSource={optionsDataSource}
                                required={required}
                                value={selectValue}
                            />
                        )}

                        {controlType === 'SELECT' && type === 'BOOLEAN' && (
                            <PropertySelect
                                defaultValue={defaultValue?.toString()}
                                description={description}
                                key={`${currentNode.name}_${name}`}
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

                        {controlType === 'TEXT_AREA' && (
                            <PropertyTextArea
                                description={description}
                                error={hasError}
                                key={`${currentNode.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name!}
                                onChange={handleInputChange}
                                required={required}
                                value={inputValue}
                            />
                        )}

                        {type === 'NULL' && <span>NULL</span>}
                    </>
                )}

                {type === 'DYNAMIC_PROPERTIES' && currentComponentDefinition && currentComponent && (
                    <PropertyDynamicProperties
                        currentActionName={actionName}
                        currentComponent={currentComponent}
                        currentComponentDefinition={currentComponentDefinition}
                        currentNodeConnectionId={currentNode.connectionId}
                        loadDependsOnValues={loadDependsOnValues}
                        name={name}
                        propertiesDataSource={property.propertiesDataSource}
                        taskParameterValue={taskParameterValue}
                    />
                )}

                {controlType === 'CODE_EDITOR' && (
                    <PropertyCodeEditor
                        defaultValue={defaultValue}
                        description={description}
                        key={`${currentNode.name}_${name}`}
                        label={label}
                        language={languageId!}
                        leadingIcon={typeIcon}
                        name={name!}
                        onChange={handleCodeEditorChange}
                        required={required}
                        value={taskParameterValue}
                        workflow={workflow}
                        workflowNodeName={currentNode.name}
                    />
                )}
            </div>
        </li>
    );
};

export default Property;
