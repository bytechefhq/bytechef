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
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyType} from '@/types/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {usePrevious} from '@uidotdev/usehooks';
import {ChangeEvent, KeyboardEvent, useEffect, useRef, useState} from 'react';
import {Control, Controller, FieldValues, FormState} from 'react-hook-form';
import ReactQuill from 'react-quill';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';

import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import getParameterByPath from '../../utils/getParameterByPath';
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
    arrayIndex?: number;
    arrayName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control?: Control<any, any>;
    customClassName?: string;
    formState?: FormState<FieldValues>;
    inputTypeSwitchButtonClassName?: string;
    objectName?: string;
    operationName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    parameterValue?: any;
    path?: string;
    property: PropertyType;
    showDeletePropertyButton?: boolean;
}

const Property = ({
    arrayIndex,
    arrayName,
    control,
    customClassName,
    formState,
    inputTypeSwitchButtonClassName,
    objectName,
    operationName,
    parameterValue,
    path = 'parameters',
    property,
    showDeletePropertyButton = false,
}: PropertyProps) => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState(property.defaultValue || '');
    const [lookupDependsOnValues, setLookupDependsOnValues] = useState<Array<string> | undefined>();
    const [mentionInputValue, setMentionInputValue] = useState(property.defaultValue || '');
    const [mentionInput, setMentionInput] = useState(!control && property.controlType === 'TEXT' ? true : false);
    const [numericValue, setNumericValue] = useState(property.defaultValue || '');
    const [propertyParameterValue, setPropertyParameterValue] = useState(parameterValue || property.defaultValue || '');
    const [selectValue, setSelectValue] = useState(property.defaultValue || '');
    const [showInputTypeSwitchButton, setShowInputTypeSwitchButton] = useState(
        (property.type !== 'STRING' && property.expressionEnabled) || false
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
    const {workflowTestConfigurationDialogOpen} = useWorkflowEditorStore();

    const previousOperationName = usePrevious(currentNode?.operationName);

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

    const isNumericalInput = !mentionInput && (controlType === 'INTEGER' || controlType === 'NUMBER');

    const typeIcon = TYPE_ICONS[type as keyof typeof TYPE_ICONS];

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

        let strippedValue: string | number = mentionInputValue.replace(/<[^>]*>?/gm, '').trim();

        const dataValueAttributes = mentionInputValue.match(/data-value="([^"]+)"/g);

        if (dataValueAttributes?.length) {
            const dataPillValues = dataValueAttributes
                .map((match) => match.match(/data-value="([^"]+)"/)?.[1])
                .map((value) => `\${${value}}`);

            const basicValues = mentionInputValue
                .split(/<div[^>]*>[\s\S]*?<\/div>/g)
                .map((value) => value.replace(/<[^>]*>?/gm, ''));

            if (strippedValue.startsWith('${') && focusedInput) {
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

            if (dataPillValues?.length) {
                strippedValue = basicValues.reduce(
                    (acc, value, index) => `${acc}${value}${dataPillValues[index] || ''}`,
                    ''
                );
            } else if ((type === 'INTEGER' || type === 'NUMBER') && !mentionInputValue.includes('data-value')) {
                strippedValue = parseInt(strippedValue);
            } else {
                const dataPillValue = dataPillValues?.[0];

                if (dataPillValue && !dataPillValue.startsWith('${') && !dataPillValue.endsWith('}')) {
                    strippedValue = `\${${dataPillValue.replace(/\//g, '.')}}`;
                } else {
                    strippedValue = mentionInputValue.replace(/<[^>]*>?/gm, '');
                }
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

    const handleDeleteCustomPropertyClick = (path: string, name?: string, arrayIndex?: number) => {
        deleteProperty(
            workflow.id!,
            path,
            currentComponent!,
            setCurrentComponent,
            deleteWorkflowNodeParameterMutation!,
            name,
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

        if (mentionInput && !mentionInputValue) {
            return;
        } else if (!mentionInput && isNumericalInput && !numericValue) {
            return;
        } else if (!mentionInput && !isNumericalInput && !selectValue && !inputValue) {
            return;
        } else if (!mentionInput && controlType === 'SELECT' && !selectValue) {
            return;
        }

        saveProperty({
            currentComponent,
            name,
            path,
            setCurrentComponent,
            successCallback: () => {
                setNumericValue('');
                setInputValue('');
                setMentionInputValue('');
                setSelectValue('');
                setPropertyParameterValue('');
            },
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
        if (control) {
            return;
        }

        if (!formState && controlType !== 'SELECT' && controlType === 'FILE_ENTRY') {
            setMentionInput(true);
        }

        if (propertyParameterValue) {
            setMentionInput(false);

            if (typeof propertyParameterValue === 'string' && propertyParameterValue.includes('${')) {
                setMentionInput(true);
            }
        }

        if (controlType === 'SELECT') {
            setMentionInput(false);

            if (
                propertyParameterValue &&
                typeof propertyParameterValue === 'string' &&
                propertyParameterValue.includes('${')
            ) {
                setMentionInput(true);
            }
        }

        if (controlType === 'ARRAY_BUILDER') {
            setMentionInput(false);
        }

        if (controlType === 'OBJECT_BUILDER') {
            setMentionInput(false);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [controlType, properties?.length, propertyParameterValue]);

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

            let params = currentWorkflowComponent?.parameters;

            if (path && path !== 'parameters') {
                params = getParameterByPath(path, currentWorkflowComponent);
            }

            setPropertyParameterValue(name ? (params?.[name] as unknown as string) : '');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // set value to propertyParameterValue
    useEffect(() => {
        if (propertyParameterValue === '') {
            if (mentionInput) {
                setMentionInputValue('');
            } else {
                setInputValue('');

                setSelectValue('');

                setNumericValue('');
            }
        }

        if (mentionInput && propertyParameterValue) {
            const mentionInputElement = editorRef.current?.getEditor().getModule('mention');

            if (typeof propertyParameterValue === 'number') {
                setMentionInputValue(propertyParameterValue.toString());
            }

            if (!mentionInputElement || typeof propertyParameterValue !== 'string') {
                return;
            }

            const mentionValues: Array<string> = propertyParameterValue
                .split(/(\$\{.*?\})/g)
                .filter((value: string) => value !== '');

            if (typeof propertyParameterValue === 'string' && propertyParameterValue.includes('${')) {
                const mentionInputNodes = mentionValues.map((value) => {
                    if (value.startsWith('${')) {
                        value = value.replace('.', '/');
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

        if (!mentionInput && inputValue === '' && propertyParameterValue) {
            setInputValue(propertyParameterValue);
        }

        if (selectValue === '' || (selectValue === defaultValue && propertyParameterValue !== undefined)) {
            if (propertyParameterValue !== undefined) {
                if (type === 'BOOLEAN') {
                    setSelectValue(propertyParameterValue.toString());
                } else {
                    setSelectValue(propertyParameterValue);
                }
            }
        }

        if (isNumericalInput && !numericValue && propertyParameterValue) {
            setNumericValue(propertyParameterValue);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [propertyParameterValue, mentionInput]);

    useEffect(() => {
        if (optionsDataSource?.optionsLookupDependsOn) {
            const optionsLookupDependsOnValues = optionsDataSource?.optionsLookupDependsOn.map((key) => {
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

            setLookupDependsOnValues(optionsLookupDependsOnValues);
        }
    }, [arrayIndex, currentComponent?.parameters, optionsDataSource?.optionsLookupDependsOn]);

    useEffect(() => {
        if (propertiesDataSource?.propertiesLookupDependsOn) {
            const propertiesLookupDependsOnValues = propertiesDataSource?.propertiesLookupDependsOn.map((key) => {
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

            setLookupDependsOnValues(propertiesLookupDependsOnValues);
        }
    }, [arrayIndex, currentComponent?.parameters, propertiesDataSource?.propertiesLookupDependsOn]);

    // set showInputTypeSwitchButton state depending on the controlType
    useEffect(() => {
        if (controlType === 'FILE_ENTRY') {
            setShowInputTypeSwitchButton(false);
        }

        if (controlType === 'SELECT') {
            setShowInputTypeSwitchButton(true);
        }
    }, [controlType]);

    // update propertyParameterValue when workflow definition changes
    useEffect(() => {
        if (!workflow.definition || !currentNode?.name || !name || !workflowTestConfigurationDialogOpen) {
            return;
        }

        const workflowDefinition = JSON.parse(workflow.definition);

        const currentWorkflowNode = [...workflowDefinition.triggers, ...workflowDefinition.tasks].find(
            (node: any) => node.name === currentNode?.name
        );

        const value = getParameterByPath(path, currentWorkflowNode)[name];

        if (value) {
            setPropertyParameterValue(value);
        } else {
            setPropertyParameterValue('');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.definition]);

    // reset all values when currentNode.operationName changes
    useEffect(() => {
        if (previousOperationName) {
            setPropertyParameterValue('');
            setInputValue('');
            setMentionInputValue('');
            setSelectValue('');
            setNumericValue('');
        }
    }, [currentNode?.operationName, previousOperationName]);

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
                {mentionInput &&
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
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            label={label || (arrayName ? undefined : name)}
                            leadingIcon={typeIcon}
                            onChange={handleMentionsInputChange}
                            onKeyPress={(event: KeyboardEvent) => {
                                if (type !== 'STRING') {
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

                {!mentionInput && (
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

                                                    <TooltipPortal>
                                                        <TooltipContent className="max-w-md">
                                                            {description}
                                                        </TooltipContent>
                                                    </TooltipPortal>
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
                                onDeleteClick={handleDeleteCustomPropertyClick}
                                path={path}
                                property={property}
                            />
                        )}

                        {controlType === 'OBJECT_BUILDER' && (
                            <ObjectProperty
                                arrayIndex={arrayIndex}
                                arrayName={arrayName}
                                onDeleteClick={handleDeleteCustomPropertyClick}
                                operationName={operationName}
                                parameterValue={propertyParameterValue}
                                path={path}
                                property={property}
                            />
                        )}

                        {type === 'FILE_ENTRY' && <ObjectProperty operationName={operationName} property={property} />}

                        {control && (isValidControlType || isNumericalInput) && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                name={`${path}.${name}`}
                                render={({field}) => (
                                    <PropertyInput
                                        description={description}
                                        error={hasError}
                                        label={label}
                                        leadingIcon={typeIcon}
                                        placeholder={placeholder}
                                        required={required}
                                        type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                        {...field}
                                    />
                                )}
                            />
                        )}

                        {control && controlType === 'SELECT' && type !== 'BOOLEAN' && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                name={`${path}.${name}`}
                                render={({field: {name, onChange}}) => (
                                    <PropertySelect
                                        description={description}
                                        label={label}
                                        leadingIcon={typeIcon}
                                        name={name}
                                        onValueChange={(value) => onChange(value)}
                                        options={options as Array<SelectOptionType>}
                                        value={selectValue}
                                    />
                                )}
                            />
                        )}

                        {control && controlType === 'SELECT' && type === 'BOOLEAN' && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                name={`${path}.${name}`}
                                render={({field: {name, onChange}}) => (
                                    <PropertySelect
                                        description={description}
                                        label={label}
                                        leadingIcon={typeIcon}
                                        name={name}
                                        onValueChange={(value) => onChange(value)}
                                        options={[
                                            {label: 'True', value: 'true'},
                                            {label: 'False', value: 'false'},
                                        ]}
                                    />
                                )}
                            />
                        )}

                        {control && controlType === 'TEXT_AREA' && (
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                name={`${path}.${name}`}
                                render={({field}) => (
                                    <PropertyTextArea
                                        description={description}
                                        error={hasError}
                                        label={label}
                                        leadingIcon={typeIcon}
                                        required={required}
                                        {...field}
                                    />
                                )}
                            />
                        )}

                        {!control && (isValidControlType || isNumericalInput) && (
                            <PropertyInput
                                description={description}
                                error={hasError}
                                errorMessage={errorMessage}
                                handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                                inputTypeSwitchButtonClassName={inputTypeSwitchButtonClassName}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                label={arrayName ? undefined : label || name}
                                leadingIcon={typeIcon}
                                max={maxValue}
                                maxLength={maxLength}
                                min={minValue}
                                minLength={minLength}
                                name={arrayName ? `${arrayName}_${arrayIndex}` : `${path}.${name}`}
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

                        {!control && (isValidControlType || isNumericalInput) && !!options?.length && (
                            <PropertySelect
                                description={description}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value) => handleSelectChange(value, name!)}
                                options={options as Array<SelectOptionType>}
                                value={selectValue}
                            />
                        )}

                        {!control && controlType === 'SELECT' && type !== 'BOOLEAN' && (
                            <PropertyComboBox
                                arrayIndex={arrayIndex}
                                description={description}
                                handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map((path) =>
                                    path.replace('[index]', `[${arrayIndex}]`)
                                )}
                                lookupDependsOnValues={lookupDependsOnValues}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name!)}
                                options={(formattedOptions as Array<OptionModel>) || undefined || []}
                                path={path}
                                required={required}
                                showInputTypeSwitchButton={showInputTypeSwitchButton}
                                value={selectValue}
                                workflowId={workflow.id!}
                                workflowNodeName={currentNode?.name ?? ''}
                            />
                        )}

                        {!control && controlType === 'SELECT' && type === 'BOOLEAN' && (
                            <PropertySelect
                                defaultValue={defaultValue?.toString()}
                                description={description}
                                handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                                inputTypeSwitchButtonClassName={inputTypeSwitchButtonClassName}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                label={label}
                                leadingIcon={typeIcon}
                                name={name}
                                onValueChange={(value: string) => handleSelectChange(value, name!)}
                                options={[
                                    {label: 'True', value: 'true'},
                                    {label: 'False', value: 'false'},
                                ]}
                                showInputTypeSwitchButton={showInputTypeSwitchButton}
                                value={selectValue}
                            />
                        )}

                        {!control && controlType === 'TEXT_AREA' && (
                            <PropertyTextArea
                                description={description}
                                error={hasError}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
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
                        lookupDependsOnValues={lookupDependsOnValues}
                        name={name}
                        parameterValue={propertyParameterValue}
                    />
                )}

                {controlType === 'CODE_EDITOR' && (
                    <PropertyCodeEditor
                        defaultValue={defaultValue}
                        description={description}
                        key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
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
