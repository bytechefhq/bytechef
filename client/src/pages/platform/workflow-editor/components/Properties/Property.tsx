import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {OptionModel} from '@/middleware/platform/configuration';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/Properties/components/InputTypeSwitchButton';
import PropertyCodeEditor from '@/pages/platform/workflow-editor/components/Properties/components/PropertyCodeEditor/PropertyCodeEditor';
import PropertyComboBox from '@/pages/platform/workflow-editor/components/Properties/components/PropertyComboBox';
import PropertyDynamicProperties from '@/pages/platform/workflow-editor/components/Properties/components/PropertyDynamicProperties';
import PropertyInput from '@/pages/platform/workflow-editor/components/Properties/components/PropertyInput/PropertyInput';
import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/Properties/components/PropertyMentionsInput/PropertyMentionsInput';
import PropertySelect, {
    SelectOptionType,
} from '@/pages/platform/workflow-editor/components/Properties/components/PropertySelect';
import PropertyTextArea from '@/pages/platform/workflow-editor/components/Properties/components/PropertyTextArea';
import {useWorkflowNodeParameterMutation} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import {useDataPillPanelStore} from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyType} from '@/types/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {ChangeEvent, KeyboardEvent, useEffect, useRef, useState} from 'react';
import {FieldValues, FormState, UseFormRegister} from 'react-hook-form';
import ReactQuill from 'react-quill';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';

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
    operationName?: string;
    arrayIndex?: number;
    arrayName?: string;
    customClassName?: string;
    formState?: FormState<FieldValues>;
    inputTypeSwitchButtonClassName?: string;
    objectName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    parameterValue?: any;
    path?: string;
    property: PropertyType;
    register?: UseFormRegister<any>;
    showDeletePropertyButton?: boolean;
}

const Property = ({
    arrayIndex,
    arrayName,
    customClassName,
    formState,
    inputTypeSwitchButtonClassName,
    objectName,
    operationName,
    parameterValue,
    path = 'parameters',
    property,
    register,
    showDeletePropertyButton = false,
}: PropertyProps) => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState(property.defaultValue || '');
    const [loadDependsOnValues, setLoadDependsOnValues] = useState<Array<string> | undefined>();
    const [mentionInputValue, setMentionInputValue] = useState(property.defaultValue || '');
    const [mentionInput, setMentionInput] = useState(!formState && property.controlType !== 'SELECT');
    const [numericValue, setNumericValue] = useState(property.defaultValue || '');
    const [propertyParameterValue, setPropertyParameterValue] = useState(parameterValue || property.defaultValue || '');
    const [selectValue, setSelectValue] = useState(property.defaultValue || '');
    const [showInputTypeSwitchButton, setShowInputTypeSwitchButton] = useState(
        (property.type !== 'STRING' && !!property.name && property.expressionEnabled) || false
    );

    const editorRef = useRef<ReactQuill>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const {
        currentComponent,
        currentComponentDefinition,
        currentNode,
        focusedInput,
        setCurrentComponent,
        setFocusedInput,
    } = useWorkflowNodeDetailsPanelStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {componentDefinitions, workflow} = useWorkflowDataStore();

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
        placeholder = '',
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

    const {deleteWorkflowNodeParameterMutation, updateWorkflowNodeParameterMutation} =
        useWorkflowNodeParameterMutation();

    const saveInputValue = useDebouncedCallback(() => {
        if (!currentComponent || !workflow || !name || !updateWorkflowNodeParameterMutation) {
            return;
        }

        const numericValueToSave = controlType === 'NUMBER' ? parseFloat(numericValue) : parseInt(numericValue, 10);

        saveProperty({
            arrayIndex,
            currentComponent,
            name,
            path,
            setCurrentComponent,
            updateWorkflowNodeParameterMutation,
            value: isNumericalInput ? numericValueToSave : inputValue,
            workflowId: workflow.id!,
        });
    }, 200);

    const saveMentionInputValue = useDebouncedCallback(() => {
        if (!currentComponent || !workflow.id || !updateWorkflowNodeParameterMutation || !name) {
            return;
        }

        const {parameters} = currentComponent;

        if (!parameters) {
            return;
        }

        // TODO handle mix of text and multiple data pills when pasting

        let strippedValue = mentionInputValue.replace(/<[^>]*>?/gm, '').trim();

        if (strippedValue.startsWith('${')) {
            const editor = focusedInput.getEditor();

            editor.deleteText(0, editor.getLength());

            editor.setText(' ');

            const mentionInput = editor.getModule('mention');

            mentionInput.insertItem(
                {
                    componentIcon: currentComponentDefinition?.icon,
                    id: currentNode?.name,
                    value: strippedValue.replace('${', '').replace('}', '').replace('.', '/'),
                },
                true,
                {blotName: 'property-mention'}
            );

            return;
        }

        if (type === 'STRING') {
            const realValues = mentionInputValue
                .trim()
                .split(/<[^>]*>?/gm)
                .filter((value) => value && (!value.startsWith('<') || !value.endsWith('>')))
                .filter((value) => value.trim().length > 0)
                .filter((value) => !value.startsWith('\\'))
                .filter((value) => !value.includes('data-value'))
                .filter((value) => !value.includes('">'));

            const isDataPillRegEx = new RegExp(/([a-zA-Z]+_[0-9]+.[a-zA-Z]+)/g);

            strippedValue = realValues
                .map((value) => {
                    if (isDataPillRegEx.test(value) && !value.startsWith('${') && !value.endsWith('}')) {
                        return `\${${value.replace(/\//g, '.')}}`;
                    } else {
                        return value;
                    }
                })
                .join('');
        } else {
            const dataPillValues = mentionInputValue
                .match(/data-value="([^"]+)"/g)
                ?.map((match) => match.match(/data-value="([^"]+)"/)?.[1]);

            const dataPillValue = dataPillValues?.[0];

            if (dataPillValue && !dataPillValue.startsWith('${') && !dataPillValue.endsWith('}')) {
                strippedValue = `\${${dataPillValue.replace(/\//g, '.')}}`;
            } else {
                strippedValue = mentionInputValue.replace(/<[^>]*>?/gm, '');
            }
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

        saveProperty({
            arrayIndex,
            currentComponent,
            name,
            path,
            setCurrentComponent,
            updateWorkflowNodeParameterMutation,
            value: strippedValue,
            workflowId: workflow.id,
        });
    }, 200);

    const handleCodeEditorChange = useDebouncedCallback((value?: string) => {
        if (!currentComponent || !name || !updateWorkflowNodeParameterMutation || !workflow.id) {
            return;
        }

        saveProperty({
            arrayIndex,
            currentComponent,
            name,
            path,
            setCurrentComponent,
            updateWorkflowNodeParameterMutation,
            value,
            workflowId: workflow.id,
        });
    }, 200);

    const handleDelete = (path: string, name: string, arrayIndex?: number) => {
        deleteProperty(
            workflow.id!,
            path,
            name,
            currentComponent!,
            setCurrentComponent,
            deleteWorkflowNodeParameterMutation!,
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

        if (!currentComponent || !name || !updateWorkflowNodeParameterMutation || !workflow.id) {
            return;
        }

        saveProperty({
            currentComponent,
            name,
            path,
            setCurrentComponent,
            updateWorkflowNodeParameterMutation,
            value: null,
            workflowId: workflow.id!,
        });
    };

    const handleSelectChange = (value: string, name: string) => {
        if (!currentComponent || !workflow.id || !name || !updateWorkflowNodeParameterMutation) {
            return;
        }

        setSelectValue(value);

        saveProperty({
            arrayIndex,
            currentComponent,
            name,
            path,
            setCurrentComponent,
            updateWorkflowNodeParameterMutation,
            value: type === 'BOOLEAN' ? value === 'true' : value,
            workflowId: workflow.id,
        });
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

    // set propertyParameterValue on initial render
    useEffect(() => {
        if (!name) {
            return;
        }

        if (!propertyParameterValue || propertyParameterValue === defaultValue) {
            const workflowComponents = [...(workflow.triggers || []), ...(workflow.tasks || [])];

            const currentWorkflowComponent = workflowComponents?.find(
                (component) => component.name === currentNode?.name
            );

            setPropertyParameterValue(name ? (currentWorkflowComponent?.parameters?.[name] as unknown as string) : '');
        }

        if (name.endsWith('_0') && defaultValue) {
            setPropertyParameterValue(defaultValue);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // set value to propertyParameterValue
    useEffect(() => {
        if (mentionInput && propertyParameterValue && mentionInputValue === '') {
            const mentionInputElement = editorRef.current?.getEditor().getModule('mention');

            if (!mentionInputElement || typeof propertyParameterValue !== 'string') {
                return;
            }

            const mentionValues: Array<string> = propertyParameterValue
                .split(/(\$\{.*?\})/g)
                .filter((value: string) => value !== '');

            if (typeof propertyParameterValue === 'string' && propertyParameterValue.includes('${')) {
                const mentionInputNodes = mentionValues.map((value) => {
                    if (value.startsWith('${')) {
                        const componentName = value.split('_')[0].replace('${', '');

                        const componentIcon =
                            componentDefinitions.find((component) => component.name === componentName)?.icon || '📄';

                        const node = document.createElement('div');

                        node.className = 'property-mention';

                        node.dataset.value = value.replace(/\$\{|\}/g, '');
                        node.dataset.componentIcon = componentIcon;

                        return node.outerHTML;
                    } else {
                        return value;
                    }
                });

                setMentionInputValue(mentionInputNodes.join(''));
            } else {
                setMentionInputValue(propertyParameterValue);
            }
        }

        if (inputValue === '' && propertyParameterValue) {
            setInputValue(propertyParameterValue);
        }

        if (selectValue === '' || (selectValue === defaultValue && propertyParameterValue)) {
            setSelectValue(propertyParameterValue);
        }

        if (numericValue === '' && propertyParameterValue) {
            setNumericValue(propertyParameterValue);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [propertyParameterValue]);

    useEffect(() => {
        if (optionsDataSource?.loadOptionsDependsOn) {
            const loadDependsOnValues = optionsDataSource?.loadOptionsDependsOn.map((key) => {
                return key
                    .split('.')
                    .reduce((acc, key) => {
                        if (key.endsWith('[index]') && arrayIndex !== undefined) {
                            return acc && acc[key.replace('[index]', '')][arrayIndex];
                        } else {
                            return acc && acc[key];
                        }
                    }, currentComponent?.parameters ?? {})
                    ?.toString();
            });

            setLoadDependsOnValues(loadDependsOnValues);
        }
    }, [arrayIndex, currentComponent?.parameters, optionsDataSource?.loadOptionsDependsOn]);

    useEffect(() => {
        if (propertiesDataSource?.loadPropertiesDependsOn) {
            const loadDependsOnValues = propertiesDataSource?.loadPropertiesDependsOn.map((key) => {
                return key
                    .split('.')
                    .reduce((acc, key) => {
                        if (key.endsWith('[index]') && arrayIndex !== undefined) {
                            return acc && acc[key.replace('[index]', '')][arrayIndex];
                        } else {
                            return acc && acc[key];
                        }
                    }, currentComponent?.parameters ?? {})
                    ?.toString();
            });

            setLoadDependsOnValues(loadDependsOnValues);
        }
    }, [arrayIndex, currentComponent?.parameters, propertiesDataSource?.loadPropertiesDependsOn]);

    // set showInputTypeSwitchButton state depending on the controlType
    useEffect(() => {
        if (controlType === 'FILE_ENTRY') {
            setShowInputTypeSwitchButton(false);
        }

        if (controlType === 'SELECT') {
            setShowInputTypeSwitchButton(true);
        }
    }, [controlType]);

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
                            controlType={controlType}
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
                            placeholder={placeholder}
                            ref={editorRef}
                            required={required}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            singleMention={type !== 'STRING'}
                            value={mentionInputValue}
                        />
                    )}

                {!showMentionInput && (
                    <>
                        {((controlType === 'OBJECT_BUILDER' && name !== '__item') ||
                            controlType === 'ARRAY_BUILDER') && (
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

                                            {!label && arrayIndex !== undefined && (
                                                <Label className="leading-normal">Item</Label>
                                            )}

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
                            <ArrayProperty onDeleteClick={handleDelete} path={path} property={property} />
                        )}

                        {controlType === 'OBJECT_BUILDER' && (
                            <ObjectProperty
                                arrayIndex={arrayIndex}
                                arrayName={arrayName}
                                onDeleteClick={handleDelete}
                                operationName={operationName}
                                parameterValue={propertyParameterValue}
                                path={path}
                                property={property}
                            />
                        )}

                        {type === 'FILE_ENTRY' && <ObjectProperty operationName={operationName} property={property} />}

                        {register && (isValidControlType || isNumericalInput) && (
                            <PropertyInput
                                defaultValue={defaultValue}
                                description={description}
                                error={hasError}
                                key={`${currentNode?.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                placeholder={placeholder}
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
                                key={`${currentNode?.name}_${name}`}
                                label={label || name}
                                leadingIcon={typeIcon}
                                max={maxValue}
                                maxLength={maxLength}
                                min={minValue}
                                minLength={minLength}
                                name={`${path}.${name}` || name || `${arrayName}_0`}
                                onChange={handleInputChange}
                                placeholder={
                                    isNumericalInput && minValue && maxValue
                                        ? `From ${minValue} to ${maxValue}`
                                        : placeholder
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
                                key={`${currentNode?.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={options as Array<SelectOptionType>}
                                value={selectValue}
                            />
                        )}

                        {controlType === 'SELECT' && type !== 'BOOLEAN' && (
                            <PropertyComboBox
                                arrayIndex={arrayIndex}
                                description={description}
                                handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                                key={`${currentNode?.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                loadDependsOnPaths={optionsDataSource?.loadOptionsDependsOn?.map(
                                    (loadOptionDependsOn) => loadOptionDependsOn.replace('[index]', `[${arrayIndex}]`)
                                )}
                                loadDependsOnValues={loadDependsOnValues}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={(formattedOptions as Array<OptionModel>) || undefined || []}
                                path={path}
                                required={required}
                                showInputTypeSwitchButton={showInputTypeSwitchButton}
                                value={selectValue}
                                workflowId={workflow.id!}
                                workflowNodeName={currentNode?.name ?? ''}
                            />
                        )}

                        {controlType === 'SELECT' && type === 'BOOLEAN' && (
                            <PropertySelect
                                defaultValue={defaultValue?.toString()}
                                description={description}
                                key={`${currentNode?.name}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name)}
                                options={[
                                    {label: 'True', value: 'true'},
                                    {label: 'False', value: 'false'},
                                ]}
                                value={propertyParameterValue}
                            />
                        )}

                        {controlType === 'TEXT_AREA' && (
                            <PropertyTextArea
                                description={description}
                                error={hasError}
                                key={`${currentNode?.name}_${name}`}
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
                        currentNodeConnectionId={currentNode?.connectionId}
                        currentOperationName={operationName}
                        loadDependsOnValues={loadDependsOnValues}
                        name={name}
                        parameterValue={propertyParameterValue}
                    />
                )}

                {controlType === 'CODE_EDITOR' && (
                    <PropertyCodeEditor
                        defaultValue={defaultValue}
                        description={description}
                        key={`${currentNode?.name}_${name}`}
                        label={label}
                        language={languageId!}
                        leadingIcon={typeIcon}
                        name={name!}
                        onChange={handleCodeEditorChange}
                        required={required}
                        value={propertyParameterValue}
                        workflow={workflow}
                        workflowNodeName={currentNode?.name ?? ''}
                    />
                )}
            </div>
        </li>
    );
};

export default Property;
