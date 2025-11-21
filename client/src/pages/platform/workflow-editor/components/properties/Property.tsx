import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {MultiSelectOptionType} from '@/components/MultiSelect/MultiSelect';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import ArrayProperty from '@/pages/platform/workflow-editor/components/properties/ArrayProperty';
import ObjectProperty from '@/pages/platform/workflow-editor/components/properties/ObjectProperty';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/properties/components/InputTypeSwitchButton';
import PropertyComboBox from '@/pages/platform/workflow-editor/components/properties/components/PropertyComboBox';
import PropertyDynamicProperties from '@/pages/platform/workflow-editor/components/properties/components/PropertyDynamicProperties';
import PropertyMultiSelect from '@/pages/platform/workflow-editor/components/properties/components/PropertyMultiSelect';
import PropertySelect from '@/pages/platform/workflow-editor/components/properties/components/PropertySelect';
import PropertyTextArea from '@/pages/platform/workflow-editor/components/properties/components/PropertyTextArea';
import PropertyCodeEditor from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditor';
import PropertyInput from '@/pages/platform/workflow-editor/components/properties/components/property-input/PropertyInput';
import PropertyJsonSchemaBuilder from '@/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilder';
import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import getInputHTMLType from '@/pages/platform/workflow-editor/utils/getInputHTMLType';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {
    GetClusterElementParameterDisplayConditions200Response,
    Option,
} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {ArrayPropertyType, ClusterElementItemType, PropertyAllType, SelectOptionType} from '@/shared/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {UseQueryResult} from '@tanstack/react-query';
import {Editor} from '@tiptap/react';
import {usePrevious} from '@uidotdev/usehooks';
import {CircleQuestionMarkIcon} from 'lucide-react';
import resolvePath from 'object-resolve-path';
import {ChangeEvent, ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Control, Controller, FieldValues, FormState} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import {decodePath, encodeParameters, encodePath} from '../../utils/encodingUtils';

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
    displayConditionsQuery?: UseQueryResult<GetClusterElementParameterDisplayConditions200Response, Error>;
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
    const [isFormulaMode, setIsFormulaMode] = useState(false);
    const [lookupDependsOnValues, setLookupDependsOnValues] = useState<Array<unknown> | undefined>();
    const [mentionInputValue, setMentionInputValue] = useState(property.defaultValue || '');
    const [mentionInput, setMentionInput] = useState(
        !control && MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType!)
    );
    const [multiSelectValue, setMultiSelectValue] = useState<string[]>(property.defaultValue || []);
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
    const isSavingRef = useRef(false);

    const {currentComponent, currentNode, setFocusedInput, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                currentComponent: state.currentComponent,
                currentNode: state.currentNode,
                setFocusedInput: state.setFocusedInput,
                workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
            }))
        );
    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {isFetchedAfterMount: isDisplayConditionsFetched, isPending: isDisplayConditionsPending} =
        displayConditionsQuery ?? {
            isFetchedAfterMount: false,
            isPending: false,
        };

    const previousOperationName = usePrevious(currentNode?.operationName);

    const defaultValue = useMemo(
        () => (property.defaultValue !== undefined ? property.defaultValue : ''),
        [property.defaultValue]
    );

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
        numberPrecision,
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
        if (controlType === 'MULTI_SELECT') {
            return TYPE_ICONS[property.items?.[0].type as keyof typeof TYPE_ICONS];
        }

        return TYPE_ICONS[type as keyof typeof TYPE_ICONS];
    }, [controlType, property.items, type]);

    const {
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        updateClusterElementParameterMutation,
        updateWorkflowNodeParameterMutation,
    } = useWorkflowEditor();

    const {rootClusterElementNodeData} = useWorkflowEditorStore(
        useShallow((state) => ({
            rootClusterElementNodeData: state.rootClusterElementNodeData,
        }))
    );

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
        if (
            !currentComponent ||
            !workflow ||
            !name ||
            !path ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
        ) {
            return;
        }

        const valueToSave = latestValueRef.current;

        isSavingRef.current = true;

        saveProperty({
            includeInMetadata: custom,
            path,
            successCallback: () => (isSavingRef.current = false),
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            value: isNumericalInput ? parseFloat(valueToSave as string) : valueToSave,
            workflowId: workflow.id!,
        });
    }, 300);

    const handleCodeEditorChange = useDebouncedCallback((value?: string) => {
        if (
            !currentComponent ||
            !name ||
            !path ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
            return;
        }

        saveProperty({
            includeInMetadata: custom,
            path,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            value,
            workflowId: workflow.id,
        });
    }, 300);

    const handleDeleteCustomPropertyClick = useCallback(
        (path: string) => {
            deleteProperty(
                custom,
                workflow.id!,
                path!,
                deleteWorkflowNodeParameterMutation!,
                deleteClusterElementParameterMutation
            );
        },
        [custom, deleteWorkflowNodeParameterMutation, deleteClusterElementParameterMutation, workflow.id]
    );

    const handleJsonSchemaBuilderChange = useDebouncedCallback((value?: SchemaRecordType) => {
        if (
            !currentComponent ||
            !name ||
            !path ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
            return;
        }

        saveProperty({
            includeInMetadata: property.custom,
            path,
            successCallback: () => setInputValue(JSON.stringify(value)),
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            value: JSON.stringify(value),
            workflowId: workflow.id,
        });
    }, 300);

    const handleInputChange = (event: ChangeEvent<HTMLInputElement> | ChangeEvent<HTMLTextAreaElement>) => {
        const {value} = event.target;

        if (isNumericalInput && value) {
            const numericValue = parseFloat(value);

            const valueTooLow = minValue ? numericValue < minValue : numericValue < Number.MIN_SAFE_INTEGER;
            const valueTooHigh = maxValue ? numericValue > maxValue : numericValue > Number.MAX_SAFE_INTEGER;

            const hasDecimalPoint = value.includes('.');

            const exceedsDecimalPrecision =
                hasDecimalPoint &&
                numberPrecision !== undefined &&
                (numberPrecision === 0 || value.split('.')[1]?.length > numberPrecision);

            if (valueTooLow || valueTooHigh) {
                setHasError(true);

                setErrorMessage('Incorrect value');
            } else if (controlType === 'INTEGER' && !/^-?\d+$/.test(value)) {
                setHasError(true);

                setErrorMessage('Value must be a valid integer');
            } else if (controlType === 'NUMBER' && !/^-?\d+(\.\d+)?$/.test(value)) {
                setHasError(true);

                setErrorMessage('Value must be a valid number');
            } else if (exceedsDecimalPrecision) {
                setHasError(true);

                if (numberPrecision === 0) {
                    setErrorMessage('Decimal points are not allowed');
                } else {
                    setErrorMessage(`Maximum ${numberPrecision} decimal places allowed`);
                }
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

    const handleInputTypeSwitchButtonClick = () => {
        setMentionInput(!mentionInput);

        setMentionInputValue('');
        setPropertyParameterValue('');
        setMultiSelectValue([]);

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

        if (
            !currentComponent ||
            !name ||
            !path ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
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
                setMultiSelectValue([]);

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
            if (
                !currentComponent ||
                !workflow.id ||
                !name ||
                !path ||
                !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
            ) {
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
                if (property.defaultValue !== undefined) {
                    const defaultValueString = String(property.defaultValue);

                    let actualValue: boolean | null | number | string =
                        type === 'BOOLEAN' ? defaultValueString === 'true' : defaultValueString;

                    if (type === 'INTEGER' && !mentionInputValue.startsWith('${')) {
                        actualValue = parseInt(defaultValueString);
                    } else if (type === 'NUMBER' && !mentionInputValue.startsWith('${')) {
                        actualValue = parseFloat(defaultValueString);
                    }

                    if (actualValue === propertyParameterValue) {
                        return;
                    }

                    setSelectValue(defaultValueString);
                    setPropertyParameterValue(actualValue);

                    saveProperty({
                        includeInMetadata: custom,
                        path,
                        type,
                        updateClusterElementParameterMutation,
                        updateWorkflowNodeParameterMutation,
                        value: actualValue,
                        workflowId: workflow.id,
                    });
                } else {
                    deleteProperty(
                        custom,
                        workflow.id,
                        path,
                        deleteWorkflowNodeParameterMutation!,
                        deleteClusterElementParameterMutation
                    );
                }

                return;
            }

            saveProperty({
                includeInMetadata: custom,
                path,
                type,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                value: actualValue,
                workflowId: workflow.id,
            });
        },
        [
            currentComponent,
            custom,
            deleteClusterElementParameterMutation,
            deleteWorkflowNodeParameterMutation,
            mentionInputValue,
            path,
            property.defaultValue,
            propertyParameterValue,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    const handleMultiSelectChange = useCallback(
        (value: string[]) => {
            if (
                !currentComponent ||
                !workflow.id ||
                !path ||
                !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
            ) {
                return;
            }

            const currentValue = JSON.stringify(propertyParameterValue || []);
            const newValue = JSON.stringify(value);

            if (currentValue === newValue) {
                return;
            }

            setPropertyParameterValue(value);
            setMultiSelectValue(value);

            saveProperty({
                includeInMetadata: custom,
                path,
                type,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                value,
                workflowId: workflow.id,
            });
        },
        [
            currentComponent,
            custom,
            path,
            propertyParameterValue,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    const memoizedWorkflowTask = useMemo(() => {
        return [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])].find(
            (node) => node.name === currentNode?.name
        );
    }, [workflow.triggers, workflow.tasks, currentNode?.name]);

    const memoizedClusterElementTask = useMemo((): ClusterElementItemType | undefined => {
        if (!currentNode?.name || !workflow.definition) {
            return undefined;
        }

        if (currentNode.clusterElementType) {
            const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

            const mainClusterRootTask = workflowDefinitionTasks?.find(
                (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
            );

            if (mainClusterRootTask?.clusterElements) {
                return getClusterElementByName(mainClusterRootTask.clusterElements, currentNode.name);
            }
        }
    }, [currentNode, workflow.definition, rootClusterElementNodeData?.workflowNodeName]);

    // set default mentionInput state
    useEffect(() => {
        if (control || mentionInput) {
            return;
        }

        if (propertyParameterValue) {
            const isStringValue = typeof propertyParameterValue === 'string';

            const hasDataPill = isStringValue && propertyParameterValue.includes('${');
            const hasFormula = isStringValue && propertyParameterValue.includes('#{');

            const shouldUseMentionInput = hasDataPill || hasFormula;

            if (shouldUseMentionInput) {
                setMentionInput(true);

                return;
            } else {
                setMentionInput(false);
            }
        }

        if (!formState && controlType !== 'SELECT' && controlType === 'FILE_ENTRY') {
            setMentionInput(true);

            return;
        }

        if (
            controlType === 'SELECT' ||
            controlType === 'MULTI_SELECT' ||
            controlType === 'JSON_SCHEMA_BUILDER' ||
            controlType === 'OBJECT_BUILDER'
        ) {
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
        if (!name || !currentComponent || !currentComponent.parameters) {
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

                if (typeof paramValue === 'string' && paramValue.startsWith('=')) {
                    setMentionInput(true);

                    setIsFormulaMode(true);
                }
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
            (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) &&
            resolvePath(currentComponent.parameters ?? {}, path) !== defaultValue
        ) {
            const saveDefaultValue = () => {
                saveProperty({
                    path,
                    type,
                    updateClusterElementParameterMutation,
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
        // Skip updating if a save operation is in progress
        if (isSavingRef.current) {
            return;
        }

        if (propertyParameterValue === '' || propertyParameterValue === undefined) {
            if (mentionInput) {
                setMentionInputValue('');
            } else {
                setInputValue('');
                setSelectValue('');
                setMultiSelectValue([]);

                setPropertyParameterValue('');
            }
        }

        if (typeof propertyParameterValue === 'string' && propertyParameterValue.startsWith('=')) {
            setMentionInputValue(propertyParameterValue.substring(1));
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

        if (controlType === 'MULTI_SELECT' && propertyParameterValue !== undefined) {
            if (propertyParameterValue === null) {
                setMultiSelectValue([]);
            } else if (propertyParameterValue !== undefined) {
                setMultiSelectValue(propertyParameterValue);
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
    }, [propertyParameterValue, mentionInput, controlType, inputValue]);

    // set lookup dependencies
    useEffect(() => {
        if (!currentComponent?.parameters || !optionsDataSource?.optionsLookupDependsOn) {
            return;
        }

        const optionsLookupDependsOnValues: unknown[] = optionsDataSource?.optionsLookupDependsOn.map(
            (optionLookupDependency) =>
                resolvePath(currentComponent?.parameters, optionLookupDependency.replace('[index]', `[${arrayIndex}]`))
        );

        setLookupDependsOnValues(optionsLookupDependsOnValues);
    }, [arrayIndex, currentComponent?.parameters, optionsDataSource?.optionsLookupDependsOn]);

    // set lookup dependencies
    useEffect(() => {
        if (!currentComponent?.parameters || !propertiesDataSource?.propertiesLookupDependsOn) {
            return;
        }

        const propertiesLookupDependsOnValues: unknown[] = propertiesDataSource?.propertiesLookupDependsOn.map(
            (propertyLookupDependency) =>
                resolvePath(
                    currentComponent?.parameters,
                    propertyLookupDependency.replace('[index]', `[${arrayIndex}]`)
                )
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

        const encodedParameters = encodeParameters(
            (memoizedWorkflowTask?.parameters || memoizedClusterElementTask?.parameters) ?? {}
        );

        const encodedPath = encodePath(path);

        setPropertyParameterValue(resolvePath(encodedParameters, encodedPath));

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.definition]);

    // reset all values when currentNode.operationName changes
    useEffect(() => {
        const parameterDefaultValue: string | string[] =
            property.defaultValue !== undefined ? property.defaultValue : '';

        if (previousOperationName) {
            setPropertyParameterValue(parameterDefaultValue);
            setInputValue(parameterDefaultValue);
            setMentionInputValue(parameterDefaultValue);
            setSelectValue(parameterDefaultValue.toString());

            if (Array.isArray(parameterDefaultValue)) {
                setMultiSelectValue(parameterDefaultValue);
            }
        }
    }, [currentNode?.operationName, previousOperationName, property.defaultValue]);

    // handle NULL type property saving
    useEffect(() => {
        if (
            type === 'NULL' &&
            propertyParameterValue === undefined &&
            currentComponent &&
            path &&
            (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
        ) {
            const saveDefaultValue = () => {
                saveProperty({
                    includeInMetadata: custom,
                    path,
                    type,
                    updateClusterElementParameterMutation,
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

    if (hidden && !control) {
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
        displayConditionsQuery &&
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
                hidden && 'hidden',
                controlType === 'OBJECT_BUILDER' && 'flex-col',
                controlType === 'ARRAY_BUILDER' && 'flex-col',
                customClassName
            )}
            key={`${currentNode?.name}_${currentComponent?.operationName}_${name}`}
        >
            {mentionInput && currentComponent && type !== 'DYNAMIC_PROPERTIES' && controlType !== 'CODE_EDITOR' && (
                <PropertyMentionsInput
                    controlType={controlType || 'TEXT'}
                    defaultValue={defaultValue}
                    deletePropertyButton={deletePropertyButton}
                    description={description}
                    handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                    isFormulaMode={isFormulaMode}
                    label={label || name}
                    leadingIcon={typeIcon}
                    path={path}
                    placeholder={placeholder}
                    ref={editorRef}
                    required={required}
                    setIsFormulaMode={setIsFormulaMode}
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
                                                    <CircleQuestionMarkIcon className="ml-1 size-4 text-muted-foreground" />
                                                </TooltipTrigger>

                                                <TooltipPortal>
                                                    <TooltipContent className="max-w-md">{description}</TooltipContent>
                                                </TooltipPortal>
                                            </Tooltip>
                                        )}
                                    </div>

                                    <div className="flex items-center gap-1">
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
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
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
                    )}

                    {control && controlType === 'SELECT' && type !== 'BOOLEAN' && path && (
                        <Controller
                            control={control}
                            defaultValue={defaultValue}
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
                                    : placeholder || `Type a ${isNumericalInput ? 'number' : 'something'} ...`
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
                            title={label || name}
                        />
                    )}

                    {!control && controlType === 'SELECT' && type !== 'BOOLEAN' && (
                        <PropertyComboBox
                            arrayIndex={arrayIndex}
                            defaultValue={defaultValue}
                            deletePropertyButton={deletePropertyButton}
                            description={description}
                            handleInputTypeSwitchButtonClick={handleInputTypeSwitchButtonClick}
                            label={label || name}
                            leadingIcon={typeIcon}
                            lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                (optionLookupDependency) => optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                            )}
                            lookupDependsOnValues={lookupDependsOnValues}
                            name={name}
                            onValueChange={(value: string) => handleSelectChange(value, name!)}
                            options={(formattedOptions as Array<Option>) || undefined || []}
                            optionsDataSource={optionsDataSource}
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
                            label={label || name}
                            leadingIcon={typeIcon}
                            name={name!}
                            onChange={handleInputChange}
                            required={required}
                            value={inputValue}
                        />
                    )}

                    {!control && controlType === 'MULTI_SELECT' && (
                        <PropertyMultiSelect
                            defaultValue={propertyParameterValue as string[]}
                            deletePropertyButton={deletePropertyButton}
                            handleInputTypeSwitchButtonClick={() => handleInputTypeSwitchButtonClick()}
                            leadingIcon={typeIcon}
                            lookupDependsOnPaths={optionsDataSource?.optionsLookupDependsOn?.map(
                                (optionLookupDependency) => optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                            )}
                            lookupDependsOnValues={lookupDependsOnValues}
                            onChange={(value) => handleMultiSelectChange(value)}
                            options={formattedOptions as MultiSelectOptionType[]}
                            optionsDataSource={optionsDataSource}
                            path={path}
                            property={property}
                            showInputTypeSwitchButton={showInputTypeSwitchButton}
                            value={multiSelectValue}
                            workflowId={workflow.id!}
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
                    lookupDependsOnPaths={propertiesDataSource?.propertiesLookupDependsOn}
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
