import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/Properties/components/InputTypeSwitchButton';
import PropertyCodeEditor from '@/pages/platform/workflow-editor/components/Properties/components/PropertyCodeEditor/PropertyCodeEditor';
import PropertyComboBox from '@/pages/platform/workflow-editor/components/Properties/components/PropertyComboBox';
import PropertyDynamicProperties from '@/pages/platform/workflow-editor/components/Properties/components/PropertyDynamicProperties';
import PropertyInput from '@/pages/platform/workflow-editor/components/Properties/components/PropertyInput/PropertyInput';
import PropertyJsonSchemaBuilder from '@/pages/platform/workflow-editor/components/Properties/components/PropertyJsonSchemaBuilder/PropertyJsonSchemaBuilder';
import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/Properties/components/PropertyMentionsInput/PropertyMentionsInput';
import PropertySelect, {
    SelectOptionType,
} from '@/pages/platform/workflow-editor/components/Properties/components/PropertySelect';
import PropertyTextArea from '@/pages/platform/workflow-editor/components/Properties/components/PropertyTextArea';
import {useWorkflowNodeParameterMutation} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {GetWorkflowNodeParameterDisplayConditions200Response, Option} from '@/shared/middleware/platform/configuration';
import {ArrayPropertyType, PropertyAllType} from '@/shared/types';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {UseQueryResult} from '@tanstack/react-query';
import {Editor} from '@tiptap/react';
import {usePrevious} from '@uidotdev/usehooks';
import {decode} from 'html-entities';
import resolvePath from 'object-resolve-path';
import {ChangeEvent, ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Control, Controller, FieldValues, FormState} from 'react-hook-form';
import sanitizeHtml from 'sanitize-html';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';

import {decodePath, encodeParameters, encodePath} from '../../utils/encodingUtils';
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

const MENTION_INPUT_PROPERTY_CONTROL_TYPES = ['EMAIL', 'PHONE', 'RICH_TEXT', 'TEXT', 'TEXT_AREA', 'URL'];

interface PropertyProps {
    arrayIndex?: number;
    arrayName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control?: Control<any, any>;
    controlPath?: string;
    customClassName?: string;
    deletePropertyButton?: ReactNode;
    displayConditionsQuery?: UseQueryResult<GetWorkflowNodeParameterDisplayConditions200Response, Error>;
    formState?: FormState<FieldValues>;
    objectName?: string;
    operationName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    parameterValue?: any;
    parentArrayItems?: Array<ArrayPropertyType>;
    path?: string;
    property: PropertyAllType;
}

const Property = ({
    arrayIndex,
    arrayName,
    control,
    controlPath = 'parameters',
    customClassName,
    deletePropertyButton,
    displayConditionsQuery,
    formState,
    objectName,
    operationName,
    parameterValue,
    parentArrayItems,
    path,
    property,
}: PropertyProps) => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState(() => {
        if (!control && MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType!)) {
            return '';
        }

        if (!INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType!)) {
            return '';
        }

        return property.defaultValue || '';
    });
    const [lookupDependsOnValues, setLookupDependsOnValues] = useState<Array<string> | undefined>();
    const [mentionInputValue, setMentionInputValue] = useState(property.defaultValue || '');
    const [mentionInput, setMentionInput] = useState(
        !control && MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType!)
    );
    const [propertyParameterValue, setPropertyParameterValue] = useState(parameterValue || property.defaultValue || '');
    const [selectValue, setSelectValue] = useState(
        property.defaultValue !== undefined ? property.defaultValue : 'null'
    );
    const [showInputTypeSwitchButton, setShowInputTypeSwitchButton] = useState(
        (property.type !== 'STRING' && property.expressionEnabled) || false
    );
    const [isFetchingCurrentDisplayCondition, setIsFetchingCurrentDisplayCondition] = useState(true);

    const editorRef = useRef<Editor>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const latestValueRef = useRef<string | number | undefined>(property.defaultValue || '');

    const {currentComponent, currentNode, setFocusedInput, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();
    const {setDataPillPanelOpen} = useDataPillPanelStore();
    const {workflow} = useWorkflowDataStore();

    const {isFetchedAfterMount: isDisplayConditionsFetched, isPending: isDisplayConditionsPending} =
        displayConditionsQuery ?? {
            isFetchedAfterMount: false,
            isPending: false,
        };

    const previousOperationName = usePrevious(currentNode?.operationName);

    const defaultValue = useMemo(() => {
        return property.defaultValue !== undefined ? property.defaultValue : '';
    }, [property.defaultValue]);

    const {
        controlType,
        custom,
        description,
        expressionEnabled,
        hidden,
        label,
        languageId,
        maxLength,
        maxValue,
        minLength,
        minValue,
        name = property.name?.replace(/\s/g, '_'),
        options,
        optionsDataSource,
        placeholder = '',
        properties,
        propertiesDataSource,
        required = false,
        type,
    } = property;

    let {displayCondition} = property;

    const formattedOptions = useMemo(() => {
        return options
            ?.map((option) => {
                if (option.value === '') {
                    return null;
                }
                return option;
            })
            .filter((option) => option !== null);
    }, [options]);

    const isValidControlType = useMemo(() => {
        return controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType);
    }, [controlType]);

    const isNumericalInput = useMemo(() => {
        return !mentionInput && (controlType === 'INTEGER' || controlType === 'NUMBER');
    }, [mentionInput, controlType]);

    const typeIcon = useMemo(() => {
        return TYPE_ICONS[type as keyof typeof TYPE_ICONS];
    }, [type]);

    const {deleteWorkflowNodeParameterMutation, updateWorkflowNodeParameterMutation} =
        useWorkflowNodeParameterMutation();

    if (!path && name) {
        path = name;
    }

    if (control) {
        path = `${controlPath}.${name}`;
    }

    if (path === objectName) {
        path = `${path}.${name}`;
    }

    if (objectName && !path?.includes(objectName)) {
        path = `${objectName}.${path}`;
    }

    if (path) {
        path = decodePath(path);
    }

    if (displayCondition) {
        const displayConditionIndexes: number[] = [];
        const bracketedNumberRegex = /\[(\d+)\]/g;
        let match;

        while ((match = bracketedNumberRegex.exec(path!)) !== null) {
            displayConditionIndexes.push(parseInt(match[1], 10));
        }

        displayConditionIndexes.forEach((index) => {
            displayCondition = displayCondition!.replace(`[index]`, `[${index}]`);
        });
    }

    const saveInputValue = useDebouncedCallback(() => {
        if (!currentComponent || !workflow || !name || !path || !updateWorkflowNodeParameterMutation) {
            return;
        }

        const valueToSave = latestValueRef.current;

        saveProperty({
            includeInMetadata: custom,
            path,
            type,
            updateWorkflowNodeParameterMutation,
            value: isNumericalInput ? parseFloat(valueToSave as string) : valueToSave,
            workflowId: workflow.id!,
        });
    }, 200);

    const saveMentionInputValue = useDebouncedCallback(() => {
        if (!currentComponent || !workflow.id || !updateWorkflowNodeParameterMutation || !name || !path) {
            return;
        }

        const sanitizedCurrentValue = sanitizeHtml(mentionInputValue, {allowedTags: []});

        if (propertyParameterValue === sanitizedCurrentValue) {
            return;
        }

        const {parameters} = currentComponent;

        if (!parameters) {
            return;
        }

        let value: string | number = mentionInputValue;

        if ((type === 'INTEGER' || type === 'NUMBER') && !mentionInputValue.startsWith('${')) {
            value = parseInt(value);
        }

        if (typeof value === 'string' && controlType !== 'RICH_TEXT') {
            value = sanitizeHtml(value, {allowedTags: []});
        }

        if (typeof value === 'string') {
            value = decode(value);
        }

        saveProperty({
            includeInMetadata: custom,
            path,
            type,
            updateWorkflowNodeParameterMutation,
            value: value || null,
            workflowId: workflow.id,
        });
    }, 200);

    const handleCodeEditorChange = useDebouncedCallback((value?: string) => {
        if (!currentComponent || !name || !path || !updateWorkflowNodeParameterMutation || !workflow.id) {
            return;
        }

        saveProperty({
            includeInMetadata: custom,
            path,
            type,
            updateWorkflowNodeParameterMutation,
            value,
            workflowId: workflow.id,
        });
    }, 200);

    const handleDeleteCustomPropertyClick = useCallback(
        (path: string) => {
            deleteProperty(workflow.id!, path!, deleteWorkflowNodeParameterMutation!);
        },
        [deleteWorkflowNodeParameterMutation, workflow.id]
    );

    const handleJsonSchemaBuilderChange = useDebouncedCallback((value?: SchemaRecordType) => {
        if (!currentComponent || !name || !path || !updateWorkflowNodeParameterMutation || !workflow.id) {
            return;
        }

        saveProperty({
            includeInMetadata: property.custom,
            path,
            successCallback: () => setInputValue(JSON.stringify(value)),
            type,
            updateWorkflowNodeParameterMutation,
            value: JSON.stringify(value),
            workflowId: workflow.id,
        });
    }, 200);

    const handleInputChange = (event: ChangeEvent<HTMLInputElement> | ChangeEvent<HTMLTextAreaElement>) => {
        const {value} = event.target;

        if (isNumericalInput && value) {
            const numericValue = parseFloat(value);

            const valueTooLow = minValue ? numericValue < minValue : numericValue < Number.MIN_SAFE_INTEGER;
            const valueTooHigh = maxValue ? numericValue > maxValue : numericValue > Number.MAX_SAFE_INTEGER;

            if (valueTooLow || valueTooHigh) {
                setHasError(true);

                setErrorMessage('Incorrect value');
            } else if (controlType === 'INTEGER' && !/^-?\d+$/.test(value)) {
                setHasError(true);

                setErrorMessage('Value must be a valid integer');
            } else if (controlType === 'NUMBER' && !/^-?\d+(\.\d+)?$/.test(value)) {
                setHasError(true);

                setErrorMessage('Value must be a valid number');
            } else {
                setHasError(false);
            }

            const onlyNumericValue = type === 'NUMBER' ? value.replace(/[^0-9.-]/g, '') : value.replace(/\D/g, '');

            if (onlyNumericValue === undefined) {
                return;
            }

            setInputValue(onlyNumericValue);

            latestValueRef.current = onlyNumericValue;
        } else {
            const valueTooShort = minLength && value.length < minLength;
            const valueTooLong = maxLength && value.length > maxLength;

            setHasError(!!valueTooShort || !!valueTooLong);

            setErrorMessage('Incorrect value');

            setInputValue(value);

            latestValueRef.current = value;
        }

        saveInputValue();
    };

    const handleMentionsInputChange = (value: string) => {
        setMentionInputValue(value);

        latestValueRef.current = value;

        saveMentionInputValue();
    };

    const handleInputTypeSwitchButtonClick = () => {
        setMentionInput(!mentionInput);

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
                editorRef.current?.commands.setContent('');
                editorRef.current?.commands.focus();

                if (workflowNodeDetailsPanelOpen) {
                    setDataPillPanelOpen(true);
                }
            }, 50);
        }

        if (!currentComponent || !name || !path || !updateWorkflowNodeParameterMutation || !workflow.id) {
            return;
        }

        const parentParameterValue = resolvePath(currentComponent.parameters ?? {}, path);

        if (mentionInput && !mentionInputValue) {
            return;
        } else if (!mentionInput && isNumericalInput && !inputValue) {
            return;
        } else if (!mentionInput && controlType === 'SELECT' && !selectValue) {
            return;
        } else if (!parentParameterValue) {
            return;
        }

        saveProperty({
            path,
            successCallback: () => {
                setInputValue('');
                setMentionInputValue('');
                setSelectValue('');
                setPropertyParameterValue('');
            },
            type,
            updateWorkflowNodeParameterMutation,
            value: null,
            workflowId: workflow.id!,
        });
    };

    const handleSelectChange = useCallback(
        (value: string, name: string) => {
            if (!currentComponent || !workflow.id || !name || !path || !updateWorkflowNodeParameterMutation) {
                return;
            }

            if (value === propertyParameterValue) {
                return;
            }

            setSelectValue(value);
            setPropertyParameterValue(value);

            let actualValue: boolean | null | number | string = type === 'BOOLEAN' ? value === 'true' : value;

            if (type === 'INTEGER' && !mentionInputValue.startsWith('${')) {
                actualValue = parseInt(value);
            } else if (type === 'NUMBER' && !mentionInputValue.startsWith('${')) {
                actualValue = parseFloat(value);
            }

            if (value === 'null' || value === '') {
                actualValue = null;
            }

            saveProperty({
                includeInMetadata: custom,
                path,
                type,
                updateWorkflowNodeParameterMutation,
                value: actualValue,
                workflowId: workflow.id,
            });
        },
        [
            currentComponent,
            custom,
            mentionInputValue,
            path,
            propertyParameterValue,
            type,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    const memoizedWorkflowTask = useMemo(() => {
        return [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])].find(
            (node) => node.name === currentNode?.name
        );
    }, [workflow.triggers, workflow.tasks, currentNode?.name]);

    // set default mentionInput state
    useEffect(() => {
        if (control) {
            return;
        }

        if (propertyParameterValue) {
            setMentionInput(false);

            if (
                typeof propertyParameterValue === 'string' &&
                controlType !== 'SELECT' &&
                controlType !== 'JSON_SCHEMA_BUILDER' &&
                (propertyParameterValue.includes('${') || type === 'STRING')
            ) {
                setMentionInput(true);
            }
        }

        if (!formState && controlType !== 'SELECT' && controlType === 'FILE_ENTRY') {
            setMentionInput(true);
        }

        if (controlType === 'SELECT' || controlType === 'JSON_SCHEMA_BUILDER' || controlType === 'OBJECT_BUILDER') {
            if (
                propertyParameterValue &&
                typeof propertyParameterValue === 'string' &&
                propertyParameterValue.includes('${')
            ) {
                setMentionInput(true);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [controlType, properties?.length, propertyParameterValue]);

    // set error state
    useEffect(() => {
        if (formState && name && path) {
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

        if (!currentComponent || !currentComponent.parameters) {
            return;
        }

        const {parameters} = currentComponent;

        if (Object.keys(parameters).length && (!propertyParameterValue || propertyParameterValue === defaultValue)) {
            if (!path) {
                setPropertyParameterValue(parameters[name]);

                return;
            }

            const encodedParameters = encodeParameters(parameters);
            const encodedPath = encodePath(path);

            const paramValue = resolvePath(encodedParameters, encodedPath);

            if (paramValue !== undefined || paramValue !== null) {
                setPropertyParameterValue(paramValue);
            } else {
                setPropertyParameterValue(encodedParameters[name]);
            }
        }

        // save hidden property to definition on render
        if (
            hidden &&
            currentComponent &&
            path &&
            objectName === undefined &&
            updateWorkflowNodeParameterMutation &&
            resolvePath(currentComponent.parameters ?? {}, path) !== defaultValue
        ) {
            const saveDefaultValue = () => {
                saveProperty({
                    path,
                    type,
                    updateWorkflowNodeParameterMutation,
                    value: defaultValue,
                    workflowId: workflow.id!,
                });
            };

            const timeoutId = setTimeout(saveDefaultValue, 200);

            return () => clearTimeout(timeoutId);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // set value to propertyParameterValue
    useEffect(() => {
        if (propertyParameterValue === '' || propertyParameterValue === undefined) {
            if (mentionInput) {
                setMentionInputValue('');
            } else {
                setInputValue('');

                setSelectValue('');

                setPropertyParameterValue('');
            }
        }

        if (mentionInput && propertyParameterValue && !mentionInputValue) {
            setMentionInputValue(propertyParameterValue);
        }

        if (
            !mentionInput &&
            controlType &&
            INPUT_PROPERTY_CONTROL_TYPES.includes(controlType) &&
            inputValue === '' &&
            propertyParameterValue
        ) {
            setInputValue(propertyParameterValue);
        }

        if (!mentionInput && controlType === 'JSON_SCHEMA_BUILDER' && propertyParameterValue !== undefined) {
            setInputValue(propertyParameterValue);
        }

        if (controlType === 'SELECT' && propertyParameterValue !== undefined) {
            if (propertyParameterValue === null) {
                setSelectValue('null');
            } else if (propertyParameterValue !== undefined) {
                if (type === 'BOOLEAN') {
                    setSelectValue(propertyParameterValue.toString());
                } else {
                    setSelectValue(propertyParameterValue);
                }
            }
        }

        if (
            isNumericalInput &&
            (inputValue === null || inputValue === undefined) &&
            (propertyParameterValue !== null || propertyParameterValue !== undefined) &&
            parameterValue
        ) {
            setInputValue(propertyParameterValue);
        }

        if (
            isNumericalInput &&
            (inputValue !== null || inputValue !== undefined) &&
            (propertyParameterValue !== null || propertyParameterValue !== undefined) &&
            propertyParameterValue !== inputValue
        ) {
            setInputValue(propertyParameterValue);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [propertyParameterValue, mentionInput]);

    // set lookup dependencies
    useEffect(() => {
        if (!currentComponent?.parameters || !optionsDataSource?.optionsLookupDependsOn) {
            return;
        }

        const optionsLookupDependsOnValues: string[] = optionsDataSource?.optionsLookupDependsOn.map(
            (optionLookupDependency) =>
                resolvePath(
                    currentComponent?.parameters,
                    optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                )?.toString()
        );

        setLookupDependsOnValues(optionsLookupDependsOnValues);
    }, [arrayIndex, currentComponent?.parameters, optionsDataSource?.optionsLookupDependsOn]);

    // set lookup dependencies
    useEffect(() => {
        if (!currentComponent?.parameters || !propertiesDataSource?.propertiesLookupDependsOn) {
            return;
        }

        const propertiesLookupDependsOnValues: string[] = propertiesDataSource?.propertiesLookupDependsOn.map(
            (propertyLookupDependency) =>
                resolvePath(
                    currentComponent?.parameters,
                    propertyLookupDependency.replace('[index]', `[${arrayIndex}]`)
                )?.toString()
        );

        setLookupDependsOnValues(propertiesLookupDependsOnValues);
    }, [arrayIndex, currentComponent?.parameters, propertiesDataSource?.propertiesLookupDependsOn]);

    // set showInputTypeSwitchButton state depending on the controlType
    useEffect(() => {
        if (controlType === 'FILE_ENTRY') {
            setShowInputTypeSwitchButton(false);
        }

        if (expressionEnabled) {
            if (controlType === 'JSON_SCHEMA_BUILDER') {
                setShowInputTypeSwitchButton(true);
            }

            if (controlType === 'SELECT') {
                setShowInputTypeSwitchButton(true);
            }
        }

        if (controlType === 'NULL') {
            setShowInputTypeSwitchButton(false);
        }
    }, [controlType, expressionEnabled]);

    // set propertyParameterValue on workflow definition change
    useEffect(() => {
        if (!workflow.definition || !currentNode?.name || !name || !path) {
            return;
        }

        const encodedParameters = encodeParameters(memoizedWorkflowTask?.parameters ?? {});
        const encodedPath = encodePath(path);

        setPropertyParameterValue(resolvePath(encodedParameters, encodedPath));

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.definition]);

    // reset all values when currentNode.operationName changes
    useEffect(() => {
        const parameterDefaultValue = property.defaultValue !== undefined ? property.defaultValue : '';

        if (previousOperationName) {
            setPropertyParameterValue(parameterDefaultValue);
            setInputValue(parameterDefaultValue);
            setMentionInputValue(parameterDefaultValue);
            setSelectValue(parameterDefaultValue.toString());
            setPropertyParameterValue(parameterDefaultValue);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentNode?.operationName, previousOperationName, property.defaultValue]);

    // handle NULL type property saving
    useEffect(() => {
        if (
            type === 'NULL' &&
            propertyParameterValue === undefined &&
            currentComponent &&
            path &&
            updateWorkflowNodeParameterMutation
        ) {
            const saveDefaultValue = () => {
                saveProperty({
                    includeInMetadata: custom,
                    path,
                    type,
                    updateWorkflowNodeParameterMutation,
                    value: null,
                    workflowId: workflow.id!,
                });
            };

            const timeoutId = setTimeout(saveDefaultValue, 200);

            return () => clearTimeout(timeoutId);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [propertyParameterValue]);

    // set display condition fetching state
    useEffect(() => {
        if (displayCondition && currentComponent?.displayConditions?.[displayCondition]) {
            setIsFetchingCurrentDisplayCondition(true);

            if (isDisplayConditionsFetched) {
                setIsFetchingCurrentDisplayCondition(false);
            }
        }
    }, [displayCondition, currentComponent?.displayConditions, isDisplayConditionsFetched]);

    if (hidden) {
        return <></>;
    }

    if (displayCondition && isDisplayConditionsPending && type !== 'ARRAY' && type !== 'OBJECT') {
        return (
            <div className={twMerge('flex flex-col space-y-1', objectName && 'ml-2 mt-1')}>
                <Skeleton className="h-5 w-1/4" />

                <Skeleton className="h-9 w-full" />
            </div>
        );
    }

    if (
        displayCondition &&
        currentComponent?.displayConditions?.[displayCondition] &&
        (isFetchingCurrentDisplayCondition || isDisplayConditionsPending) &&
        type !== 'ARRAY' &&
        type !== 'OBJECT'
    ) {
        return (
            <div className={twMerge('flex flex-col space-y-1', objectName && 'ml-2 mt-1')}>
                <Skeleton className="h-5 w-1/4" />

                <Skeleton className="h-9 w-full" />
            </div>
        );
    }

    if (displayCondition && !currentComponent?.displayConditions?.[displayCondition]) {
        return <></>;
    }

    return (
        <li
            className={twMerge(
                'w-full',
                hidden && 'mb-0',
                controlType === 'OBJECT_BUILDER' && 'flex-col',
                controlType === 'ARRAY_BUILDER' && 'flex-col',
                customClassName
            )}
        >
            {mentionInput && currentComponent && type !== 'DYNAMIC_PROPERTIES' && controlType !== 'CODE_EDITOR' && (
                <PropertyMentionsInput
                    controlType={controlType || 'TEXT'}
                    defaultValue={defaultValue}
                    deletePropertyButton={deletePropertyButton}
                    description={description}
                    handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                    key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                    label={label || name}
                    leadingIcon={typeIcon}
                    onChange={handleMentionsInputChange}
                    path={path}
                    placeholder={placeholder}
                    ref={editorRef}
                    required={required}
                    showInputTypeSwitchButton={showInputTypeSwitchButton}
                    type={type}
                    value={mentionInputValue}
                />
            )}

            {!mentionInput && (
                <>
                    {((controlType === 'OBJECT_BUILDER' && name !== '__item') ||
                        controlType === 'ARRAY_BUILDER' ||
                        controlType === 'NULL') && (
                        <div className={twMerge('flex items-center', controlType !== 'NULL' && 'pb-1')}>
                            {typeIcon && controlType !== 'NULL' && (
                                <span className={twMerge(label ? 'pr-2' : 'pr-1')} title={type}>
                                    {typeIcon}
                                </span>
                            )}

                            {(label || description || showInputTypeSwitchButton) && (
                                <div className="flex w-full items-center justify-between">
                                    <div className="flex items-center">
                                        {label && (
                                            <Label className="leading-normal">
                                                {label}

                                                {required && <RequiredMark />}
                                            </Label>
                                        )}

                                        {!label && arrayIndex !== undefined && (
                                            <Label className="leading-normal">Item</Label>
                                        )}

                                        {description && (
                                            <Tooltip>
                                                <TooltipTrigger>
                                                    <QuestionMarkCircledIcon className="ml-1" />
                                                </TooltipTrigger>

                                                <TooltipPortal>
                                                    <TooltipContent className="max-w-md">{description}</TooltipContent>
                                                </TooltipPortal>
                                            </Tooltip>
                                        )}
                                    </div>

                                    <div className="flex items-center">
                                        {showInputTypeSwitchButton && (
                                            <InputTypeSwitchButton
                                                handleClick={handleInputTypeSwitchButtonClick}
                                                mentionInput={mentionInput}
                                            />
                                        )}

                                        {deletePropertyButton}
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {controlType === 'ARRAY_BUILDER' && path && (
                        <ArrayProperty
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            parentArrayItems={parentArrayItems}
                            path={path}
                            property={property}
                        />
                    )}

                    {(controlType === 'OBJECT_BUILDER' || type === 'FILE_ENTRY') && (
                        <ObjectProperty
                            arrayIndex={arrayIndex}
                            arrayName={arrayName}
                            onDeleteClick={handleDeleteCustomPropertyClick}
                            operationName={operationName}
                            path={path}
                            property={property}
                        />
                    )}

                    {control && (isValidControlType || isNumericalInput) && path && (
                        <>
                            <Controller
                                control={control}
                                defaultValue={defaultValue}
                                key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                                name={path}
                                render={({field}) => (
                                    <PropertyInput
                                        description={description}
                                        error={hasError}
                                        errorMessage={errorMessage}
                                        label={label || name}
                                        leadingIcon={typeIcon}
                                        placeholder={placeholder}
                                        required={required}
                                        type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                                        {...field}
                                    />
                                )}
                                rules={{required}}
                            />
                        </>
                    )}

                    {control && controlType === 'SELECT' && type !== 'BOOLEAN' && path && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            name={path}
                            render={({field: {name, onChange}}) => (
                                <PropertySelect
                                    description={description}
                                    label={label || name}
                                    leadingIcon={typeIcon}
                                    name={name}
                                    onValueChange={(value) => {
                                        onChange(value);

                                        setSelectValue(value);
                                    }}
                                    options={options as Array<SelectOptionType>}
                                    required={required}
                                    value={selectValue}
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {control && controlType === 'SELECT' && type === 'BOOLEAN' && path && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            name={path}
                            render={({field: {name, onChange}}) => (
                                <PropertySelect
                                    description={description}
                                    label={label || name}
                                    leadingIcon={typeIcon}
                                    name={name}
                                    onValueChange={(value) => {
                                        onChange(value);
                                        setSelectValue(value);
                                    }}
                                    options={[
                                        {label: 'True', value: 'true'},
                                        {label: 'False', value: 'false'},
                                    ]}
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {control && controlType === 'TEXT_AREA' && path && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            name={path}
                            render={({field}) => (
                                <PropertyTextArea
                                    description={description}
                                    error={hasError}
                                    errorMessage={errorMessage}
                                    label={label || name}
                                    leadingIcon={typeIcon}
                                    required={required}
                                    {...field}
                                />
                            )}
                            rules={{required}}
                        />
                    )}

                    {!control && (isValidControlType || isNumericalInput) && path && (
                        <PropertyInput
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            error={hasError}
                            errorMessage={errorMessage}
                            fieldsetClassName={objectName && arrayName && 'ml-2'}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            label={label || name}
                            leadingIcon={typeIcon}
                            max={maxValue}
                            maxLength={maxLength}
                            min={minValue}
                            minLength={minLength}
                            name={path}
                            onChange={handleInputChange}
                            placeholder={
                                isNumericalInput && minValue && maxValue
                                    ? `From ${minValue} to ${maxValue}`
                                    : placeholder || 'Type something...'
                            }
                            ref={inputRef}
                            required={required}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            title={type}
                            type={hidden ? 'hidden' : getInputHTMLType(controlType)}
                            value={inputValue}
                        />
                    )}

                    {!control && (isValidControlType || isNumericalInput) && !!options?.length && (
                        <PropertySelect
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name}
                            onValueChange={(value) => handleSelectChange(value, name!)}
                            options={options as Array<SelectOptionType>}
                            required={required}
                            value={selectValue}
                        />
                    )}

                    {!control && controlType === 'JSON_SCHEMA_BUILDER' && (
                        <PropertyJsonSchemaBuilder
                            description={description}
                            error={hasError}
                            errorMessage={errorMessage}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name!}
                            onChange={(value) => handleJsonSchemaBuilderChange(value)}
                            schema={inputValue ? JSON.parse(inputValue) : undefined}
                        />
                    )}

                    {!control && controlType === 'SELECT' && type !== 'BOOLEAN' && (
                        <PropertyComboBox
                            arrayIndex={arrayIndex}
                            defaultValue={defaultValue}
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            label={label || name}
                            leadingIcon={typeIcon}
                            lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                (optionLookupDependency) => optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                            )}
                            lookupDependsOnValues={lookupDependsOnValues}
                            name={name}
                            onValueChange={(value: string) => handleSelectChange(value, name!)}
                            options={(formattedOptions as Array<Option>) || undefined || []}
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
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name}
                            onValueChange={(value: string) => handleSelectChange(value, name!)}
                            options={[
                                {label: 'True', value: 'true'},
                                {label: 'False', value: 'false'},
                            ]}
                            required={required}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            value={selectValue}
                        />
                    )}

                    {!control && controlType === 'TEXT_AREA' && (
                        <PropertyTextArea
                            description={description}
                            error={hasError}
                            errorMessage={errorMessage}
                            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name!}
                            onChange={handleInputChange}
                            required={required}
                            value={inputValue}
                        />
                    )}

                    {controlType === 'NULL' && <span>NULL</span>}
                </>
            )}

            {type === 'DYNAMIC_PROPERTIES' && currentNode && (
                <PropertyDynamicProperties
                    currentOperationName={operationName}
                    enabled={
                        !!(currentNode?.connectionId && currentNode?.connections) ||
                        currentNode?.connections?.length === 0
                    }
                    lookupDependsOnValues={lookupDependsOnValues}
                    name={name}
                    parameterValue={propertyParameterValue}
                    path={path}
                />
            )}

            {controlType === 'CODE_EDITOR' && (
                <PropertyCodeEditor
                    defaultValue={defaultValue}
                    description={description}
                    error={hasError}
                    errorMessage={errorMessage}
                    key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
                    label={label || name}
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
        </li>
    );
};

export default Property;
