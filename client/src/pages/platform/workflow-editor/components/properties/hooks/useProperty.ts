import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import {decodePath, encodeParameters, encodePath} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {
    ControlType,
    GetClusterElementParameterDisplayConditions200Response,
    Option,
    OptionsDataSource,
    PropertiesDataSource,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {ArrayPropertyType, ClusterElementItemType, ComponentType, NodeDataType, PropertyAllType} from '@/shared/types';
import {UseQueryResult} from '@tanstack/react-query';
import {Editor} from '@tiptap/react';
import {usePrevious} from '@uidotdev/usehooks';
import resolvePath from 'object-resolve-path';
import {
    ChangeEvent,
    Dispatch,
    ReactNode,
    RefObject,
    SetStateAction,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react';
import {Control, FieldValues, FormState} from 'react-hook-form';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/react/shallow';

import {getTask} from '../../../utils/getTask';

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

const MENTION_INPUT_PROPERTY_CONTROL_TYPES = [
    'EMAIL',
    'FORMULA_MODE',
    'PHONE',
    'RICH_TEXT',
    'TEXT',
    'TEXT_AREA',
    'URL',
];

function getInitialControlledDynamicMode(
    control: Control<FieldValues, FieldValues> | undefined,
    controlPath: string,
    propertyName: string | undefined
): boolean {
    if (!control?._formValues || !propertyName) {
        return false;
    }

    const fieldPath = controlPath ? `${controlPath}.${propertyName}` : propertyName;
    const fieldValue = fieldPath
        .split('.')
        .reduce<unknown>(
            (currentObject, key) => (currentObject as Record<string, unknown>)?.[key],
            control._formValues
        );

    return typeof fieldValue === 'string' && fieldValue.startsWith('=');
}

function getInitialInputValue(
    control: Control<FieldValues, FieldValues> | undefined,
    property: PropertyAllType
): string {
    if (!control && MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType!)) {
        return '';
    }

    if (!INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType!)) {
        return '';
    }

    return property.defaultValue || '';
}

type UsePropertyReturnType = {
    calculatedPath: string | undefined;
    controlledBlurError: string | undefined;
    controlledDynamicMode: boolean;
    controlledDynamicOnChangeRef: RefObject<((value: string) => void) | null>;
    controlledFromAi: boolean | undefined;
    controlType?: ControlType;
    currentComponent: ComponentType | undefined;
    currentNode: NodeDataType | undefined;
    defaultValue: string;
    description?: string;
    displayCondition?: string;
    editorRef: RefObject<Editor | null>;
    errorMessage: string;
    formattedOptions: Array<Option> | undefined;
    fromAiExpression: string;
    handleCodeEditorChange: (value?: string) => void;
    handleControlledBlur: (value: unknown) => void;
    handleControlledModeSwitch: (toDynamic: boolean) => void;
    handleDeleteCustomPropertyClick: (path: string) => void;
    handleFromAiClick: ((fromAi: boolean) => void) | undefined;
    handleFromAiToggle: (fromAi: boolean, fieldOnChange: (value: string) => void) => void;
    handleInputChange: (event: ChangeEvent<HTMLInputElement> | ChangeEvent<HTMLTextAreaElement>) => void;
    handleInputTypeSwitchButtonClick: () => void;
    handleJsonSchemaBuilderChange: (value?: SchemaRecordType) => void;
    handleMentionInputValueChange: (value: string | number) => void;
    handleMultiSelectChange: (value: string[]) => void;
    handleSelectChange: (value: string, name: string) => void;
    expressionEnabled: boolean | undefined;
    hasError: boolean;
    hidden: boolean | undefined;
    inputRef: RefObject<HTMLInputElement | null>;
    inputValue: string;
    isFormulaMode: boolean;
    isFromAi: boolean;
    isLoadingDisplayCondition: boolean;
    isNumericalInput: boolean;
    isToolsClusterElement: boolean;
    isValidControlType: boolean | undefined;
    label?: string;
    languageId?: string;
    lookupDependsOnValues?: Array<unknown>;
    maxLength?: number;
    maxValue?: number;
    mentionInput: boolean;
    mentionInputValue: string;
    minLength?: number;
    minValue?: number;
    multiSelectValue: string[];
    name: string | undefined;
    options?: PropertyAllType['options'];
    optionsDataSource?: OptionsDataSource;
    placeholder: string;
    propertiesDataSource?: PropertiesDataSource;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    propertyParameterValue: any;
    required: boolean;
    resetOnModeChangeRef: RefObject<boolean>;
    selectValue: string;
    setControlledFromAi: Dispatch<SetStateAction<boolean | undefined>>;
    setDataPillPanelOpen: (open: boolean) => void;
    setIsFormulaMode: Dispatch<SetStateAction<boolean>>;
    setSelectValue: Dispatch<SetStateAction<string>>;
    showInputTypeSwitchButton: boolean;
    type?: PropertyAllType['type'];
    typeIcon: ReactNode;
    validatePropertyValue: (value: string | number) => boolean;
    workflow: Workflow;
};

interface UsePropertyProps {
    arrayIndex?: number;
    arrayName?: string;
    control?: Control<FieldValues, FieldValues>;
    controlPath?: string;
    displayConditionsQuery?: UseQueryResult<GetClusterElementParameterDisplayConditions200Response, Error>;
    dynamicPropertySource?: string;
    formState?: FormState<FieldValues>;
    hideFromAi?: boolean;
    objectName?: string;
    operationName?: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    parameterValue?: any;
    parentArrayItems?: Array<ArrayPropertyType>;
    path?: string;
    property: PropertyAllType;
    toolsMode?: boolean;
}

export const useProperty = ({
    arrayIndex,
    control,
    controlPath = 'parameters',
    displayConditionsQuery,
    dynamicPropertySource,
    formState,
    hideFromAi,
    objectName,
    parameterValue,
    path,
    property,
    toolsMode,
}: UsePropertyProps): UsePropertyReturnType => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState(() => getInitialInputValue(control, property));
    const [isFormulaMode, setIsFormulaModeInternal] = useState(property.controlType === 'FORMULA_MODE');
    const [lookupDependsOnValues, setLookupDependsOnValues] = useState<Array<unknown> | undefined>();
    const [mentionInputValue, setMentionInputValue] = useState(() => {
        const initialMentionValue = parameterValue !== undefined ? parameterValue : property.defaultValue || '';

        return typeof initialMentionValue === 'string' ? initialMentionValue : '';
    });
    const [mentionInput, setMentionInput] = useState(
        !control && MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType!)
    );
    const [multiSelectValue, setMultiSelectValue] = useState<string[]>(property.defaultValue || []);
    const [propertyParameterValue, setPropertyParameterValue] = useState(() =>
        parameterValue !== undefined ? parameterValue : property.defaultValue || ''
    );
    const [selectValue, setSelectValue] = useState(() => {
        if (parameterValue !== undefined && parameterValue !== null) {
            return typeof parameterValue === 'boolean' ? parameterValue.toString() : parameterValue;
        }

        return property.defaultValue !== undefined ? property.defaultValue : 'null';
    });
    const [showInputTypeSwitchButton, setShowInputTypeSwitchButton] = useState(
        !control && ((property.type !== 'STRING' && property.expressionEnabled) || false)
    );
    const [isFetchingCurrentDisplayCondition, setIsFetchingCurrentDisplayCondition] = useState(true);
    const [controlledBlurError, setControlledBlurError] = useState<string | undefined>();
    const [controlledDynamicMode, setControlledDynamicMode] = useState(() =>
        getInitialControlledDynamicMode(control, controlPath, property.name?.replace(/\s/g, '_'))
    );
    const [controlledFromAi, setControlledFromAi] = useState<boolean | undefined>(undefined);

    const controlledDynamicOnChangeRef = useRef<((value: string) => void) | null>(null);
    const editorRef = useRef<Editor>(null!);

    const inputRef = useRef<HTMLInputElement>(null!);
    const latestValueRef = useRef<string | number | undefined>(property.defaultValue || '');
    const isSavingRef = useRef(false);
    const mentionInputValueRef = useRef<string | number | undefined>(undefined);
    const parameterValueRef = useRef(parameterValue);

    parameterValueRef.current = parameterValue;

    const previousPropertyPathForParameterSyncRef = useRef<string | undefined>(undefined);
    const resetOnModeChangeRef = useRef(false);

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

    const isToolsClusterElement = !hideFromAi && (toolsMode || currentNode?.clusterElementType === 'tools');

    const {isPending: isDisplayConditionsPending, isSuccess: isDisplayConditionsSuccess} = displayConditionsQuery ?? {
        isPending: false,
        isSuccess: false,
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
        maxNumberPrecision,
        maxValue,
        minLength,
        minNumberPrecision,
        minValue,
        name = property.name?.replace(/\s/g, '_'),
        numberPrecision,
        options,
        optionsDataSource,
        placeholder = '',
        properties,
        propertiesDataSource,
        regex,
        required = false,
        type,
    } = property;

    const {
        DECIMAL_POINTS_NOT_ALLOWED,
        INCORRECT_VALUE,
        MAX_DECIMAL_PLACES,
        MIN_DECIMAL_PLACES,
        VALUE_DOES_NOT_MATCH_PATTERN,
        VALUE_MUST_BE_VALID_INTEGER,
        VALUE_MUST_BE_VALID_NUMBER,
    } = ERROR_MESSAGES.PROPERTY;

    let {displayCondition} = property;

    const {
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        updateClusterElementParameterMutation,
        updateWorkflowNodeParameterMutation,
    } = useWorkflowEditor();

    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    if (!path && name) {
        path = name;
    }

    if (control) {
        path = controlPath ? `${controlPath}.${name}` : name;
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
            displayCondition = displayCondition!.replace('[index]', `[${index}]`);
        });

        if (displayConditionIndexes.length > 0 && displayCondition.includes('[index]')) {
            const lastIndex = displayConditionIndexes[displayConditionIndexes.length - 1];

            while (displayCondition.includes('[index]')) {
                displayCondition = displayCondition.replace('[index]', `[${lastIndex}]`);
            }
        }
    }

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

    const fromAiExpression = useMemo(() => {
        const mapEntries: string[] = [];

        if (description) {
            const escapedDescription = description.replace(/'/g, "''");

            mapEntries.push(`'description': '${escapedDescription}'`);
        }

        if (defaultValue !== '' && defaultValue !== null && defaultValue !== undefined) {
            const escapedDefault = String(defaultValue).replace(/'/g, "''");

            mapEntries.push(`'defaultValue': '${escapedDefault}'`);
        }

        if (formattedOptions != null && formattedOptions.length > 0) {
            const optionValues = formattedOptions
                .map((option) => `'${String(option?.value ?? '').replace(/'/g, "''")}'`)
                .join(', ');

            mapEntries.push(`'options': {${optionValues}}`);
        }

        if (mapEntries.length === 0) {
            return `=fromAi('${name}', '${type}')`;
        }

        return `=fromAi('${name}', '${type}', {${mapEntries.join(', ')}})`;
    }, [defaultValue, description, formattedOptions, name, type]);

    const isValidControlType = useMemo(
        () => controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType),
        [controlType]
    );

    const isNumericalInput = useMemo(
        () => !mentionInput && (controlType === 'INTEGER' || controlType === 'NUMBER'),
        [mentionInput, controlType]
    );

    const typeIcon = useMemo(() => {
        if (controlType === 'MULTI_SELECT') {
            return TYPE_ICONS[property.items?.[0]?.type as keyof typeof TYPE_ICONS];
        }

        return TYPE_ICONS[type as keyof typeof TYPE_ICONS];
    }, [controlType, property.items, type]);

    const isFromAi = useMemo(() => {
        if (controlledFromAi !== undefined) {
            return controlledFromAi;
        }

        if (!currentComponent?.metadata?.ui?.fromAi || !path) {
            return false;
        }

        return currentComponent.metadata.ui.fromAi.includes(path);
    }, [controlledFromAi, currentComponent?.metadata?.ui?.fromAi, path]);

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

            const mainClusterRootTask = rootClusterElementNodeData?.workflowNodeName
                ? getTask({
                      tasks: workflowDefinitionTasks,
                      workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                  })
                : undefined;

            if (mainClusterRootTask?.clusterElements) {
                return getClusterElementByName(mainClusterRootTask.clusterElements, currentNode.name);
            }
        }
    }, [currentNode, workflow.definition, rootClusterElementNodeData?.workflowNodeName]);

    const validatePropertyValue = useCallback(
        (value: string | number): boolean => {
            const stringValue = typeof value === 'string' ? value : String(value);

            if (typeof value === 'string' && (value.startsWith('=') || value.includes('${'))) {
                return true;
            }

            if ((type === 'INTEGER' || type === 'NUMBER') && typeof value === 'string' && !value.includes('${')) {
                const numericValue = parseFloat(value);

                if (minValue != null && numericValue < minValue) {
                    return false;
                }

                if (maxValue != null && numericValue > maxValue) {
                    return false;
                }

                if (controlType === 'INTEGER' && !/^-?\d+$/.test(value)) {
                    return false;
                }

                if (controlType === 'NUMBER' && !/^-?\d+(\.\d+)?$/.test(value)) {
                    return false;
                }

                if (numberPrecision != null && value.includes('.')) {
                    const decimalLength = value.split('.')[1]?.length ?? 0;

                    if (numberPrecision === 0 || decimalLength > numberPrecision) {
                        return false;
                    }
                }

                if (value.includes('.')) {
                    const decimalLength = value.split('.')[1]?.length ?? 0;

                    if (minNumberPrecision != null && decimalLength < minNumberPrecision) {
                        return false;
                    }

                    if (maxNumberPrecision != null && decimalLength > maxNumberPrecision) {
                        return false;
                    }
                }

                return true;
            }

            if (minLength != null && stringValue.length < minLength) {
                return false;
            }

            if (maxLength != null && stringValue.length > maxLength) {
                return false;
            }

            if (regex) {
                try {
                    if (new RegExp(regex).test(stringValue)) {
                        return false;
                    }
                } catch {
                    console.warn('Invalid regex provided: ', regex);
                }
            }

            return true;
        },
        [
            controlType,
            maxLength,
            maxNumberPrecision,
            maxValue,
            minLength,
            minNumberPrecision,
            minValue,
            numberPrecision,
            regex,
            type,
        ]
    );

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

        if (valueToSave !== undefined && valueToSave !== '' && !validatePropertyValue(valueToSave)) {
            return;
        }

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
    }, 600);

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
    }, 600);

    const handleDeleteCustomPropertyClick = useCallback(
        (path: string) => {
            deleteProperty(
                workflow.id!,
                path!,
                deleteWorkflowNodeParameterMutation!,
                deleteClusterElementParameterMutation
            );
        },
        [deleteWorkflowNodeParameterMutation, deleteClusterElementParameterMutation, workflow.id]
    );

    const handleControlledBlur = useCallback(
        (value: unknown) => {
            const isInvalid = value !== '' && value != null && !validatePropertyValue(value as string | number);

            setControlledBlurError(isInvalid ? ERROR_MESSAGES.PROPERTY.INCORRECT_VALUE : undefined);
        },
        [validatePropertyValue]
    );

    const handleControlledModeSwitch = useCallback((toDynamic: boolean) => {
        resetOnModeChangeRef.current = true;

        setControlledDynamicMode(toDynamic);
        setControlledFromAi(undefined);
    }, []);

    const handleFromAiToggle = useCallback(
        (fromAi: boolean, fieldOnChange: (value: string) => void) => {
            setControlledFromAi(fromAi);

            if (fromAi) {
                fieldOnChange(fromAiExpression);
            }
        },
        [fromAiExpression]
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
    }, 600);

    const handleInputChange = (event: ChangeEvent<HTMLInputElement> | ChangeEvent<HTMLTextAreaElement>) => {
        const {value} = event.target;

        if (isNumericalInput && value && value.startsWith('=') && expressionEnabled) {
            setMentionInput(true);
            setIsFormulaMode(true);

            const expressionContent = value.substring(1);

            setMentionInputValue(expressionContent);

            setTimeout(() => {
                if (editorRef.current) {
                    editorRef.current.commands.setContent(expressionContent);
                    editorRef.current.commands.focus();

                    setFocusedInput(editorRef.current);

                    if (workflowNodeDetailsPanelOpen) {
                        setDataPillPanelOpen(true);
                    }
                }
            }, 50);

            return;
        }

        if (isNumericalInput && value) {
            const numericValue = parseFloat(value);

            const valueTooLow = minValue ? numericValue < minValue : numericValue < Number.MIN_SAFE_INTEGER;
            const valueTooHigh = maxValue ? numericValue > maxValue : numericValue > Number.MAX_SAFE_INTEGER;

            const hasDecimalPoint = value.includes('.');
            const decimalLength = hasDecimalPoint ? (value.split('.')[1]?.length ?? 0) : 0;

            const exceedsDecimalPrecision =
                hasDecimalPoint &&
                numberPrecision !== undefined &&
                (numberPrecision === 0 || decimalLength > numberPrecision);

            const belowMinNumberPrecision =
                hasDecimalPoint && minNumberPrecision != null && decimalLength < minNumberPrecision;
            const aboveMaxNumberPrecision =
                hasDecimalPoint && maxNumberPrecision != null && decimalLength > maxNumberPrecision;

            if (valueTooLow || valueTooHigh) {
                setHasError(true);

                setErrorMessage(INCORRECT_VALUE);
            } else if (controlType === 'INTEGER' && !/^-?\d+$/.test(value)) {
                setHasError(true);

                setErrorMessage(VALUE_MUST_BE_VALID_INTEGER);
            } else if (controlType === 'NUMBER' && !/^-?\d+(\.\d+)?$/.test(value)) {
                setHasError(true);

                setErrorMessage(VALUE_MUST_BE_VALID_NUMBER);
            } else if (exceedsDecimalPrecision) {
                setHasError(true);

                if (numberPrecision === 0) {
                    setErrorMessage(DECIMAL_POINTS_NOT_ALLOWED);
                } else {
                    setErrorMessage(MAX_DECIMAL_PLACES(numberPrecision));
                }
            } else if (belowMinNumberPrecision) {
                setHasError(true);

                setErrorMessage(MIN_DECIMAL_PLACES(minNumberPrecision!));
            } else if (aboveMaxNumberPrecision) {
                setHasError(true);

                setErrorMessage(MAX_DECIMAL_PLACES(maxNumberPrecision!));
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

            let regexMismatch = false;

            if (regex && value && !value.startsWith('=')) {
                try {
                    regexMismatch = new RegExp(regex).test(value);
                } catch {
                    // Invalid regex from backend; skip regex validation
                }
            }

            const hasValidationError = !!valueTooShort || !!valueTooLong || regexMismatch;

            setHasError(hasValidationError);

            setErrorMessage(regexMismatch ? VALUE_DOES_NOT_MATCH_PATTERN : INCORRECT_VALUE);

            setInputValue(value);

            latestValueRef.current = value;
        }

        saveInputValue();
    };

    const handleMentionInputValueChange = useCallback(
        (value: string | number) => {
            setMentionInputValue(typeof value === 'number' ? String(value) : value);

            const stringValue = typeof value === 'string' ? value : '';
            const isExpression = typeof value === 'string' && (value.startsWith('=') || value.includes('${'));

            if (!stringValue || isExpression) {
                setHasError(false);

                return;
            }

            const valueTooShort = minLength && stringValue.length < minLength;
            const valueTooLong = maxLength && stringValue.length > maxLength;

            let regexMismatch = false;

            if (regex) {
                try {
                    regexMismatch = new RegExp(regex).test(stringValue);
                } catch {
                    console.warn('Invalid regex provided: ', regex);
                }
            }

            const hasValidationError = !!valueTooShort || !!valueTooLong || regexMismatch;

            const errorMessage = regexMismatch ? VALUE_DOES_NOT_MATCH_PATTERN : INCORRECT_VALUE;

            setHasError(hasValidationError);
            setErrorMessage(errorMessage);
        },
        [INCORRECT_VALUE, maxLength, minLength, regex, VALUE_DOES_NOT_MATCH_PATTERN]
    );

    const handleInputTypeSwitchButtonClick = () => {
        const switchingToDynamic = !mentionInput;

        setMentionInput(switchingToDynamic);

        if (switchingToDynamic && isFromAi) {
            setMentionInputValue(fromAiExpression.substring(1));
            setPropertyParameterValue(fromAiExpression);
            setMultiSelectValue([]);

            setTimeout(() => {
                setFocusedInput(editorRef.current);
            }, 50);

            if (
                currentComponent &&
                name &&
                path &&
                (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) &&
                workflow.id
            ) {
                saveProperty({
                    fromAi: true,
                    includeInMetadata: true,
                    path,
                    type,
                    updateClusterElementParameterMutation,
                    updateWorkflowNodeParameterMutation,
                    value: fromAiExpression,
                    workflowId: workflow.id!,
                });
            }

            return;
        }

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

        const parentParameterValue = resolvePath(encodeParameters(currentComponent.parameters ?? {}), encodePath(path));

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

            isSavingRef.current = true;

            let actualValue: boolean | null | number | string = type === 'BOOLEAN' ? value === 'true' : value;

            if (type === 'INTEGER' && typeof mentionInputValue === 'string' && !mentionInputValue.includes('${')) {
                actualValue = parseInt(value);
            } else if (type === 'NUMBER' && !mentionInputValue.includes('${')) {
                actualValue = parseFloat(value);
            }

            if (value === 'null' || value === '') {
                if (property.defaultValue !== undefined) {
                    const defaultValueString = String(property.defaultValue);

                    let actualValue: boolean | null | number | string =
                        type === 'BOOLEAN' ? defaultValueString === 'true' : defaultValueString;

                    if (
                        type === 'INTEGER' &&
                        typeof mentionInputValue === 'string' &&
                        !mentionInputValue.includes('${')
                    ) {
                        actualValue = parseInt(defaultValueString);
                    } else if (type === 'NUMBER' && !mentionInputValue.includes('${')) {
                        actualValue = parseFloat(defaultValueString);
                    }

                    if (actualValue === propertyParameterValue) {
                        isSavingRef.current = false;

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

    const handleFromAiClick = useCallback(
        (fromAi: boolean) => {
            if (!path || !workflow.id) {
                return;
            }

            setControlledFromAi(fromAi);

            let value = propertyParameterValue;

            if (fromAi) {
                if (editorRef.current) {
                    editorRef.current.commands.setContent(fromAiExpression);
                    editorRef.current.setEditable(false);

                    value = fromAiExpression;
                }
            } else {
                if (editorRef.current) {
                    editorRef.current.setEditable(true);

                    editorRef.current.commands.focus();

                    setFocusedInput(editorRef.current);
                }
            }

            saveProperty({
                fromAi,
                includeInMetadata: custom || fromAi,
                path,
                type,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                value,
                workflowId: workflow.id,
            });
        },
        [
            custom,
            fromAiExpression,
            path,
            propertyParameterValue,
            setFocusedInput,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    const setIsFormulaMode: Dispatch<SetStateAction<boolean>> = useCallback(
        (value) => {
            if (property.controlType === 'FORMULA_MODE') {
                return;
            }

            setIsFormulaModeInternal(value);
        },
        [property.controlType]
    );

    // set default mentionInput state
    useEffect(() => {
        if (control || mentionInput) {
            return;
        }

        if (propertyParameterValue) {
            const isStringValue = typeof propertyParameterValue === 'string';

            const hasDataPill = isStringValue && propertyParameterValue.includes('${');
            const hasExpression = isStringValue && propertyParameterValue.startsWith('=');
            const hasFormula = isStringValue && propertyParameterValue.includes('#{');

            const shouldUseMentionInput = hasDataPill || hasExpression || hasFormula;

            if (shouldUseMentionInput) {
                setMentionInput(true);
                setMentionInputValue(hasExpression ? propertyParameterValue.substring(1) : propertyParameterValue);

                if (hasExpression) {
                    setIsFormulaMode(true);
                }

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
        if (control) {
            return;
        }

        if (!name || !currentComponent || !currentComponent.parameters) {
            return;
        }

        const {parameters} = currentComponent;

        const encodedParameters = encodeParameters(parameters);
        const encodedPath = path ? encodePath(path) : undefined;

        const isExpressionValue = typeof propertyParameterValue === 'string' && propertyParameterValue.startsWith('=');

        if (Object.keys(parameters).length && (!propertyParameterValue || propertyParameterValue === defaultValue)) {
            if (parameterValue === undefined) {
                if (!path || !encodedPath) {
                    setPropertyParameterValue(parameters[name]);

                    return;
                }

                const valueFromDefinition = resolvePath(encodedParameters, encodedPath);

                if (valueFromDefinition !== undefined && valueFromDefinition !== null) {
                    setPropertyParameterValue(valueFromDefinition);

                    if (typeof valueFromDefinition === 'string' && valueFromDefinition.startsWith('=')) {
                        setMentionInput(true);
                        setMentionInputValue(valueFromDefinition.substring(1));

                        setIsFormulaMode(true);
                    } else if (mentionInput && typeof valueFromDefinition === 'string') {
                        setMentionInputValue(valueFromDefinition);

                        mentionInputValueRef.current = valueFromDefinition ? valueFromDefinition : undefined;
                    }
                } else {
                    setPropertyParameterValue(encodedParameters[name]);
                }
            }
        } else if (isExpressionValue) {
            setMentionInput(true);
            setMentionInputValue(propertyParameterValue.substring(1));

            setIsFormulaMode(true);
        }

        const shouldSaveHiddenProperty =
            hidden &&
            encodedPath &&
            (objectName === undefined || dynamicPropertySource === objectName) &&
            (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) &&
            resolvePath(encodedParameters, encodedPath) !== defaultValue;

        if (shouldSaveHiddenProperty) {
            const saveDefaultValue = () => {
                saveProperty({
                    path: path!,
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

    useEffect(() => {
        if (control || !path || !currentComponent?.parameters) {
            return;
        }

        const previousPath = previousPropertyPathForParameterSyncRef.current;

        previousPropertyPathForParameterSyncRef.current = path;

        if (previousPath === undefined) {
            return;
        }

        if (previousPath === path) {
            return;
        }

        if (!Object.keys(currentComponent.parameters).length) {
            return;
        }

        const encodedParameters = encodeParameters(currentComponent.parameters);
        const encodedPath = encodePath(path);

        if (!encodedPath) {
            return;
        }

        const valueFromDefinition = resolvePath(encodedParameters, encodedPath);

        const effectiveValue = parameterValue !== undefined ? parameterValue : valueFromDefinition;

        if (effectiveValue !== undefined && effectiveValue !== null) {
            if (type === 'BOOLEAN' && typeof effectiveValue === 'boolean') {
                setPropertyParameterValue(effectiveValue.toString());

                return;
            }

            setPropertyParameterValue(effectiveValue as never);

            if (typeof effectiveValue === 'string' && effectiveValue.startsWith('=')) {
                setMentionInput(true);
                setMentionInputValue(effectiveValue.substring(1));

                setIsFormulaMode(true);

                return;
            }

            if (mentionInput && typeof effectiveValue === 'string') {
                setMentionInputValue(effectiveValue);

                mentionInputValueRef.current = effectiveValue ? effectiveValue : undefined;
            }

            return;
        }

        const fallbackParameterValue = parameterValue !== undefined ? parameterValue : defaultValue;

        setPropertyParameterValue(fallbackParameterValue as never);
    }, [
        control,
        currentComponent?.parameters,
        defaultValue,
        mentionInput,
        parameterValue,
        path,
        setIsFormulaMode,
        type,
    ]);

    // set error state for mention input
    useEffect(() => {
        if (!mentionInput) {
            return;
        }

        const stringValue = typeof mentionInputValue === 'string' ? mentionInputValue : '';

        const isExpression =
            typeof mentionInputValue === 'string' &&
            (mentionInputValue.startsWith('=') || mentionInputValue.includes('${'));

        if (!stringValue || isExpression) {
            setHasError(false);

            return;
        }

        const valueTooShort = minLength && stringValue.length < minLength;
        const valueTooLong = maxLength && stringValue.length > maxLength;

        let regexMismatch = false;

        if (regex) {
            try {
                regexMismatch = new RegExp(regex).test(stringValue);
            } catch {
                // Invalid regex from backend; skip regex validation
            }
        }

        const hasValidationError = !!valueTooShort || !!valueTooLong || regexMismatch;

        setHasError(hasValidationError);
        setErrorMessage(regexMismatch ? VALUE_DOES_NOT_MATCH_PATTERN : INCORRECT_VALUE);
    }, [INCORRECT_VALUE, mentionInput, mentionInputValue, maxLength, minLength, regex, VALUE_DOES_NOT_MATCH_PATTERN]);

    // Sync propertyParameterValue one-way into value states
    useEffect(() => {
        if (isSavingRef.current) {
            return;
        }

        if (propertyParameterValue === '' || propertyParameterValue === undefined) {
            if (mentionInput) {
                const userHasUnsavedInput = !mentionInputValueRef.current && mentionInputValue;

                if (!userHasUnsavedInput) {
                    setMentionInputValue('');

                    mentionInputValueRef.current = undefined;
                }
            } else {
                setInputValue('');
                setSelectValue('');
                setMultiSelectValue([]);

                setPropertyParameterValue('');
            }

            return;
        }

        if (typeof propertyParameterValue === 'string' && propertyParameterValue.startsWith('=')) {
            if (mentionInputValueRef.current !== propertyParameterValue) {
                setMentionInputValue(propertyParameterValue.substring(1));

                mentionInputValueRef.current = propertyParameterValue;
            }

            return;
        }

        const shouldSyncMentionInputFromPlainStringParameter =
            mentionInput &&
            mentionInputValueRef.current !== propertyParameterValue &&
            typeof propertyParameterValue === 'string' &&
            propertyParameterValue !== '';

        if (shouldSyncMentionInputFromPlainStringParameter) {
            setMentionInputValue(propertyParameterValue);

            mentionInputValueRef.current = propertyParameterValue;

            return;
        }

        const shouldApplyParameterValueToEmptyPlainInput =
            !mentionInput &&
            controlType &&
            INPUT_PROPERTY_CONTROL_TYPES.includes(controlType) &&
            propertyParameterValue;

        if (shouldApplyParameterValueToEmptyPlainInput) {
            setInputValue(propertyParameterValue);
        }

        if (!mentionInput && controlType === 'JSON_SCHEMA_BUILDER' && propertyParameterValue !== undefined) {
            setInputValue(propertyParameterValue);
        }

        if (controlType === 'SELECT' && propertyParameterValue !== undefined) {
            if (propertyParameterValue === null) {
                setSelectValue('null');
            } else {
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

        if (isNumericalInput && propertyParameterValue !== null && propertyParameterValue !== undefined) {
            setInputValue(propertyParameterValue);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps -- omit mentionInputValue/inputValue to avoid feedback loop
    }, [propertyParameterValue, mentionInput, controlType]);

    // set options lookup dependencies
    useEffect(() => {
        if (!optionsDataSource?.optionsLookupDependsOn) {
            return;
        }

        const parametersSource = currentComponent?.parameters || (control ? control._formValues : undefined);

        if (!parametersSource) {
            return;
        }

        const optionsLookupDependsOnValues: unknown[] = optionsDataSource.optionsLookupDependsOn.map(
            (optionLookupDependency) => {
                const resolvedValue = resolvePath(
                    parametersSource,
                    optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                );

                if (typeof resolvedValue === 'string' && resolvedValue.startsWith('=fromAi(')) {
                    return undefined;
                }

                return resolvedValue;
            }
        );

        setLookupDependsOnValues(optionsLookupDependsOnValues);
    }, [arrayIndex, control, currentComponent?.parameters, optionsDataSource?.optionsLookupDependsOn]);

    // set properties lookup dependencies
    useEffect(() => {
        if (!propertiesDataSource?.propertiesLookupDependsOn) {
            return;
        }

        const parametersSource = currentComponent?.parameters || (control ? control._formValues : undefined);

        if (!parametersSource) {
            return;
        }

        const propertiesLookupDependsOnValues: unknown[] = propertiesDataSource.propertiesLookupDependsOn.map(
            (propertyLookupDependency) => {
                const resolvedValue = resolvePath(
                    parametersSource,
                    propertyLookupDependency.replace('[index]', `[${arrayIndex}]`)
                );

                if (typeof resolvedValue === 'string' && resolvedValue.startsWith('=fromAi(')) {
                    return undefined;
                }

                return resolvedValue;
            }
        );

        setLookupDependsOnValues(propertiesLookupDependsOnValues);
    }, [arrayIndex, control, currentComponent?.parameters, propertiesDataSource?.propertiesLookupDependsOn]);

    // set showInputTypeSwitchButton state depending on the controlType
    useEffect(() => {
        if (control) {
            return;
        }

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

        if (controlType === 'FORMULA_MODE') {
            setShowInputTypeSwitchButton(false);
        }
    }, [control, controlType, expressionEnabled]);

    // Sync propertyParameterValue from workflow definition whenever it changes (including on mount,
    // so that remounted Property components pick up the latest saved values after tab switching).
    useEffect(() => {
        if (control) {
            return;
        }

        if (isSavingRef.current) {
            isSavingRef.current = false;

            return;
        }

        if (!workflow.definition || !currentNode?.name || !name || !path) {
            return;
        }

        const encodedParameters = encodeParameters(
            (memoizedWorkflowTask?.parameters || memoizedClusterElementTask?.parameters) ?? {}
        );

        const encodedPath = encodePath(path);

        const valueFromWorkflowDefinition = resolvePath(encodedParameters, encodedPath);

        const nextParameterValue =
            parameterValueRef.current !== undefined ? parameterValueRef.current : valueFromWorkflowDefinition;

        setPropertyParameterValue(nextParameterValue);
        // eslint-disable-next-line react-hooks/exhaustive-deps -- sync when workflow JSON changes; read latest parameterValue via ref
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

            mentionInputValueRef.current = undefined;

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

            if (isDisplayConditionsSuccess) {
                setIsFetchingCurrentDisplayCondition(false);
            }
        }
    }, [displayCondition, currentComponent?.displayConditions, isDisplayConditionsSuccess]);

    useEffect(() => {
        if (controlledDynamicMode && resetOnModeChangeRef.current && controlledDynamicOnChangeRef.current) {
            resetOnModeChangeRef.current = false;
            controlledDynamicOnChangeRef.current('=');
        }
    }, [controlledDynamicMode, resetOnModeChangeRef]);

    const isLoadingDisplayCondition = !!(
        displayCondition &&
        type !== 'ARRAY' &&
        type !== 'OBJECT' &&
        (isDisplayConditionsPending ||
            (displayConditionsQuery &&
                currentComponent?.displayConditions?.[displayCondition] &&
                isFetchingCurrentDisplayCondition))
    );

    return {
        calculatedPath: path,
        controlType,
        controlledBlurError,
        controlledDynamicMode,
        controlledDynamicOnChangeRef,
        controlledFromAi,
        currentComponent,
        currentNode,
        defaultValue,
        description,
        displayCondition,
        editorRef,
        errorMessage,
        expressionEnabled,
        formattedOptions,
        fromAiExpression,
        handleCodeEditorChange,
        handleControlledBlur,
        handleControlledModeSwitch,
        handleDeleteCustomPropertyClick,
        handleFromAiClick: hideFromAi ? undefined : handleFromAiClick,
        handleFromAiToggle,
        handleInputChange,
        handleInputTypeSwitchButtonClick,
        handleJsonSchemaBuilderChange,
        handleMentionInputValueChange,
        handleMultiSelectChange,
        handleSelectChange,
        hasError,
        hidden,
        inputRef,
        inputValue,
        isFormulaMode,
        isFromAi,
        isLoadingDisplayCondition,
        isNumericalInput,
        isToolsClusterElement,
        isValidControlType,
        label,
        languageId,
        lookupDependsOnValues,
        maxLength,
        maxValue,
        mentionInput,
        mentionInputValue,
        minLength,
        minValue,
        multiSelectValue,
        name,
        options,
        optionsDataSource,
        placeholder,
        propertiesDataSource,
        propertyParameterValue,
        required,
        resetOnModeChangeRef,
        selectValue,
        setControlledFromAi,
        setDataPillPanelOpen,
        setIsFormulaMode,
        setSelectValue,
        showInputTypeSwitchButton,
        type,
        typeIcon,
        validatePropertyValue,
        workflow,
    };
};

export default useProperty;
