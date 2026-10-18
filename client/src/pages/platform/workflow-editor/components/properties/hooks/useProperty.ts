import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {useFormulaEnabledContext} from '@/pages/platform/workflow-editor/components/properties/FormulaEnabledContext';
import getInitialFormulaMode from '@/pages/platform/workflow-editor/components/properties/getInitialFormulaMode';
import {
    INPUT_PROPERTY_CONTROL_TYPES,
    ParameterValueContextI,
    getInitialPropertyValueState,
    propertyValueReducer,
} from '@/pages/platform/workflow-editor/components/properties/hooks/propertyValueReducer';
import {
    PropertyInputModeI,
    fromFormulaValue,
    getPropertyInputMode,
    isSingleDataPill,
    isStringSelect,
    shouldIncludeInMetadata,
    toFormulaValue,
} from '@/pages/platform/workflow-editor/components/properties/propertyInputMode';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import {
    decodePath,
    encodeParameters,
    encodePath,
    safeResolvePath,
} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import {resolveMainClusterRootName} from '@/pages/platform/workflow-editor/utils/resolveClusterRootId';
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
import {ArrayPropertyType, ClusterElementItemType, NodeDataType, PropertyAllType} from '@/shared/types';
import {UseQueryResult} from '@tanstack/react-query';
import {Editor} from '@tiptap/react';
import {usePrevious} from '@uidotdev/usehooks';
import {
    ChangeEvent,
    Dispatch,
    KeyboardEvent,
    ReactNode,
    RefObject,
    SetStateAction,
    useCallback,
    useEffect,
    useMemo,
    useReducer,
    useRef,
    useState,
} from 'react';
import {Control, FieldValues, FormState} from 'react-hook-form';
import {useDebouncedCallback} from 'use-debounce';

import {getTask} from '../../../utils/getTask';
import {computeFromAiToggle} from './fromAiToggle';

const isSavedFormulaValue = (value: unknown): boolean =>
    typeof value === 'string' && value.startsWith('=') && !value.startsWith('=fromAi(');

type UsePropertyReturnType = {
    calculatedPath: string | undefined;
    controlledBlurError: string | undefined;
    controlledExpressionExitRef: RefObject<boolean>;
    controlledFormulaOnChangeRef: RefObject<((value: string) => void) | null>;
    controlledFromAi: boolean | undefined;
    controlType?: ControlType;
    currentNode: NodeDataType | undefined;
    defaultValue: string;
    description?: string;
    displayCondition?: string;
    editorFocusRequest: {initialInput?: string; token: number} | undefined;
    editorPendingSaveCancelRef: RefObject<(() => void) | null>;
    editorRef: RefObject<Editor | null>;
    errorMessage: string;
    formattedOptions: Array<Option> | undefined;
    fromAiExpression: string;
    handleCodeEditorChange: (value?: string) => void;
    handleControlledBlur: (value: unknown) => void;
    handleControlledBuilderFormulaSwitch: () => void;
    handleControlledFormulaSwitch: (fieldValue: unknown, fieldOnChange: (value: unknown) => void) => void;
    handleControlledNativeKeyDown: (
        event: KeyboardEvent<HTMLInputElement>,
        fieldValue: unknown,
        fieldOnChange: (value: unknown) => void
    ) => void;
    handleDeleteCustomPropertyClick: (path: string) => void;
    handleDynamicSwitch: () => void;
    hasDynamicSwitch: boolean;
    handleFromAiClick: ((fromAi: boolean) => void) | undefined;
    handleFormulaSwitch: () => void;
    handleFromAiToggle: (fromAi: boolean, fieldOnChange: (value: string) => void) => void;
    handleControlledNativeFromAiClick: (fieldOnChange: (value: string) => void) => void;
    handleInputChange: (event: ChangeEvent<HTMLInputElement> | ChangeEvent<HTMLTextAreaElement>) => void;
    handleInputClear: () => void;
    handleJsonSchemaBuilderChange: (value?: SchemaRecordType) => void;
    handleMentionInputValueChange: (value: string | number) => void;
    handleMultiSelectChange: (value: string[]) => void;
    handleNativeKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void;
    handleSelectChange: (value: string, name: string) => void;
    handleSelectKeyDown: (event: KeyboardEvent<HTMLElement>) => void;
    handleSinglePillAbandoned: () => void;
    expressionEnabled: boolean | undefined;
    hasError: boolean;
    hidden: boolean | undefined;
    inputMode: PropertyInputModeI;
    inputRef: RefObject<HTMLInputElement | null>;
    inputValue: string;
    insertPillValue: (mentionId: string) => void;
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
    optionsLoadedDynamically?: boolean;
    placeholder: string;
    propertiesDataSource?: PropertiesDataSource;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    propertyParameterValue: any;
    required: boolean;
    resetOnModeChangeRef: RefObject<boolean>;
    selectValue: string;
    setControlledFromAi: Dispatch<SetStateAction<boolean | undefined>>;
    setIsFormulaMode: Dispatch<SetStateAction<boolean>>;
    setLookupDependsOnValues: Dispatch<SetStateAction<Array<unknown> | undefined>>;
    setSelectValue: (value: string) => void;
    showFormulaSwitch: boolean;
    startPillEntry: (initialInput?: string) => void;
    type?: PropertyAllType['type'];
    typeIcon: ReactNode;
    validateMentionInputValue: (value: string | number) => boolean;
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
    arrayName,
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
    const [editorFocusRequest, setEditorFocusRequest] = useState<{initialInput?: string; token: number} | undefined>();
    const [errorMessage, setErrorMessage] = useState('');
    const [formulaModeState, setFormulaModeState] = useState(() =>
        getInitialFormulaMode({
            control,
            controlPath,
            controlType: property.controlType,
            parameterValue,
            propertyName: property.name?.replace(/\s/g, '_'),
            propertyType: property.type,
        })
    );
    const [hasError, setHasError] = useState(false);
    const [lookupDependsOnValues, setLookupDependsOnValues] = useState<Array<unknown> | undefined>();
    const [pillEntry, setPillEntry] = useState(false);

    const [valueState, dispatchValueAction] = useReducer(
        propertyValueReducer,
        {controlType: property.controlType, defaultValue: property.defaultValue, hasControl: !!control, parameterValue},
        getInitialPropertyValueState
    );

    const {inputValue, mentionInputValue, multiSelectValue, propertyParameterValue, selectValue} = valueState;

    const [isFetchingCurrentDisplayCondition, setIsFetchingCurrentDisplayCondition] = useState(true);
    const [controlledBlurError, setControlledBlurError] = useState<string | undefined>();
    const [controlledFromAi, setControlledFromAi] = useState<boolean | undefined>(undefined);

    const controlledExpressionExitRef = useRef(false);
    const controlledFormulaOnChangeRef = useRef<((value: string) => void) | null>(null);
    const editorPendingSaveCancelRef = useRef<(() => void) | null>(null);
    const editorRef = useRef<Editor>(null!);

    const inputRef = useRef<HTMLInputElement>(null!);
    const latestValueRef = useRef<string | number | undefined>(property.defaultValue || '');
    const isSavingRef = useRef(false);
    const parameterValueRef = useRef(parameterValue);
    const pillEntryKeepsOptionRef = useRef(false);

    parameterValueRef.current = parameterValue;

    const previousPropertyPathForParameterSyncRef = useRef<string | undefined>(undefined);
    const resetOnModeChangeRef = useRef(false);

    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const formulaEnabledOverride = useFormulaEnabledContext();

    const isToolsClusterElement = !hideFromAi && (toolsMode || currentNode?.clusterElementType === 'tools');

    const {isPending: isDisplayConditionsPending, isSuccess: isDisplayConditionsSuccess} = displayConditionsQuery ?? {
        isPending: false,
        isSuccess: false,
    };

    const previousOperationName = usePrevious(currentNode?.operationName);

    const defaultValue = useMemo(() => property.defaultValue ?? '', [property.defaultValue]);

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
        optionsLoadedDynamically,
        placeholder = '',
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
            const defaultValueString = String(defaultValue);

            // Skip expression-shaped defaults (including prior fromAi output) so repeat
            // clicks don't keep nesting the previous expression as an escaped default.
            if (!defaultValueString.startsWith('=')) {
                const escapedDefault = defaultValueString.replace(/'/g, "''");

                mapEntries.push(`'defaultValue': '${escapedDefault}'`);
            }
        }

        if (formattedOptions != null && formattedOptions.length > 0) {
            const optionValues = formattedOptions
                .map((option) => `'${String(option?.value ?? '').replace(/'/g, "''")}'`)
                .join(', ');

            mapEntries.push(`'options': {${optionValues}}`);
        }

        mapEntries.push(`'required': ${required}`);

        // Array items have `name` equal to the index (e.g. "0"). Qualifying it with
        // the parent array's name gives the model a meaningful identifier.
        const qualifiedName = arrayName ? `${arrayName}_${name}` : name;

        return `=fromAi('${qualifiedName}', '${type}', {${mapEntries.join(', ')}})`;
    }, [arrayName, defaultValue, description, formattedOptions, name, required, type]);

    const isValidControlType = useMemo(
        () => controlType && INPUT_PROPERTY_CONTROL_TYPES.includes(controlType),
        [controlType]
    );

    const setInputValue = useCallback((value: string) => {
        dispatchValueAction({type: 'inputValueChanged', value});
    }, []);

    const setMentionInputValue = useCallback((value: string) => {
        dispatchValueAction({type: 'mentionInputValueChanged', value});
    }, []);

    const setSelectValue = useCallback((value: string) => {
        dispatchValueAction({type: 'selectValueChanged', value});
    }, []);

    const resolveParameterValue = useCallback(
        (value: unknown, options?: {authoritativeValue?: unknown; context?: Partial<ParameterValueContextI>}) => {
            dispatchValueAction({
                authoritativeValue: options?.authoritativeValue,
                context: {...parameterValueContextRef.current, ...options?.context},
                syncDisplayValues: !isSavingRef.current,
                type: 'parameterValueResolved',
                value,
            });
        },
        []
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

        if (path && currentNode?.metadata?.ui?.fromAi?.includes(path)) {
            return true;
        }

        return propertyParameterValue === fromAiExpression;
    }, [controlledFromAi, currentNode?.metadata?.ui?.fromAi, fromAiExpression, path, propertyParameterValue]);

    const inputMode: PropertyInputModeI = useMemo(
        () =>
            getPropertyInputMode({
                controlType,
                formulaMode: formulaModeState,
                hasControl: !!control,
                isFromAi: !control && isFromAi,
                pillEntry,
                type,
                value: control ? undefined : propertyParameterValue,
            }),
        [control, controlType, formulaModeState, isFromAi, pillEntry, propertyParameterValue, type]
    );

    const stringSelect = isStringSelect(controlType, type);

    // A select or a builder has nothing on screen to type `$` into, so it carries a Dynamic switch, and its pill
    // editor stays up until that switch is turned off. An array item's object has no label row to hold the switch.
    const hasDynamicSwitch =
        !control &&
        expressionEnabled !== false &&
        (controlType === 'SELECT' ||
            controlType === 'ARRAY_BUILDER' ||
            controlType === 'JSON_SCHEMA_BUILDER' ||
            (controlType === 'OBJECT_BUILDER' && name !== '__item'));

    const mentionInput = !control && inputMode.renderer === 'mentions';
    const isFormulaMode = inputMode.mode === 'formula';

    const formulaEnabled = formulaEnabledOverride ?? (control ? isToolsClusterElement : true);

    const showFormulaSwitch =
        formulaEnabled &&
        expressionEnabled !== false &&
        !isFromAi &&
        controlType !== 'FORMULA_MODE' &&
        controlType !== 'NULL' &&
        controlType !== 'CODE_EDITOR' &&
        controlType !== 'FILE_ENTRY' &&
        type !== 'DYNAMIC_PROPERTIES';

    const isNumericalInput = useMemo(
        () => !mentionInput && (controlType === 'INTEGER' || controlType === 'NUMBER'),
        [mentionInput, controlType]
    );

    const parameterValueContext = useMemo<ParameterValueContextI>(
        () => ({controlType, formulaMode: formulaModeState, isNumericalInput, mentionInput, type}),
        [controlType, formulaModeState, isNumericalInput, mentionInput, type]
    );

    const parameterValueContextRef = useRef(parameterValueContext);
    parameterValueContextRef.current = parameterValueContext;

    const liveValue = useMemo(() => {
        if (mentionInput) {
            return isFormulaMode && mentionInputValue ? `=${mentionInputValue}` : mentionInputValue;
        }

        return isValidControlType && inputValue !== '' ? inputValue : propertyParameterValue;
    }, [inputValue, isFormulaMode, isValidControlType, mentionInput, mentionInputValue, propertyParameterValue]);

    const setIsFormulaMode: Dispatch<SetStateAction<boolean>> = useCallback(
        (value) => {
            if (property.controlType === 'FORMULA_MODE') {
                return;
            }

            setFormulaModeState(value);

            if (value === false && !mentionInputValue.trim()) {
                dispatchValueAction({type: 'valueCleared'});
            }
        },
        [mentionInputValue, property.controlType]
    );

    const currentNodeName = currentNode?.name;
    const currentNodeClusterElementType = currentNode?.clusterElementType;

    const memoizedWorkflowTask = useMemo(() => {
        return (
            workflow.triggers?.find((node) => node.name === currentNodeName) ??
            workflow.tasks?.find((node) => node.name === currentNodeName)
        );
    }, [workflow.triggers, workflow.tasks, currentNodeName]);

    const memoizedClusterElementTask = useMemo((): ClusterElementItemType | undefined => {
        if (!currentNodeName || !workflow.definition || !currentNodeClusterElementType) {
            return undefined;
        }

        const mainClusterRootName = resolveMainClusterRootName(currentNode, rootClusterElementNodeData);

        if (!mainClusterRootName) {
            return undefined;
        }

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

        const mainClusterRootTask = getTask({
            tasks: workflowDefinitionTasks,
            workflowNodeName: mainClusterRootName,
        });

        if (mainClusterRootTask?.clusterElements) {
            return getClusterElementByName(mainClusterRootTask.clusterElements, currentNodeName);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        currentNodeClusterElementType,
        currentNodeName,
        currentNode?.clusterRoot,
        currentNode?.parentClusterRootId,
        currentNode?.topLevelClusterRootId,
        workflow.definition,
        rootClusterElementNodeData?.workflowNodeName,
    ]);

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
            !currentNode ||
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

        const isDateOrTimeControlType = controlType === 'DATE' || controlType === 'DATE_TIME' || controlType === 'TIME';

        let resolvedValue: unknown;

        if (valueToSave === '' && (isNumericalInput || isDateOrTimeControlType)) {
            resolvedValue = null;
        } else if (isNumericalInput) {
            resolvedValue = parseFloat(valueToSave as string);
        } else {
            resolvedValue = valueToSave;
        }

        saveProperty({
            includeInMetadata: custom,
            path,
            successCallback: () => (isSavingRef.current = false),
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            value: resolvedValue,
            workflowId: workflow.id!,
        });
    }, 600);

    const handleInputClear = useCallback(() => {
        setInputValue('');
        setHasError(false);
        setErrorMessage('');

        latestValueRef.current = '';

        saveInputValue();

        // Defer focus() so React commits value='' while PropertyInput is still unfocused —
        // otherwise onFocus flips isFocused=true before the sync effect runs, the clear never
        // reaches the input's internal localValue, and the stale time stays visible until blur.
        requestAnimationFrame(() => inputRef.current?.focus());
    }, [saveInputValue, setInputValue]);

    const handleCodeEditorChange = useDebouncedCallback((value?: string) => {
        if (
            !currentNode ||
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

    const handleFromAiToggle = useCallback(
        (fromAi: boolean, fieldOnChange: (value: string) => void) => {
            setControlledFromAi(fromAi);

            const {savePayload, value} = computeFromAiToggle({
                custom,
                fromAi,
                fromAiExpression,
                hasPath: !!path,
                hasWorkflowId: !!workflow.id,
            });

            fieldOnChange(value);

            if (
                !savePayload ||
                !path ||
                !workflow.id ||
                !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
            ) {
                return;
            }

            saveProperty({
                ...savePayload,
                path,
                type,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                workflowId: workflow.id,
            });
        },
        [
            custom,
            fromAiExpression,
            path,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    const handleControlledNativeFromAiClick = useCallback(
        (fieldOnChange: (value: string) => void) => {
            setIsFormulaMode(true);

            handleFromAiToggle(true, fieldOnChange);
        },
        [handleFromAiToggle, setIsFormulaMode]
    );

    const handleJsonSchemaBuilderChange = useDebouncedCallback((value?: SchemaRecordType) => {
        if (
            !currentNode ||
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

            const onlyNumericValue =
                type === 'NUMBER' ? value.replace(/(?!^-)[^0-9.]/g, '') : value.replace(/(?!^-)\D/g, '');

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

            if (typeof value === 'string' && value.includes('${')) {
                pillEntryKeepsOptionRef.current = false;
            }

            const keepsEditor = hasDynamicSwitch && !isFormulaMode;

            // Emptied, the editor of a field with a Dynamic switch stays up: only the switch takes it down.
            if (keepsEditor) {
                setPillEntry(true);
            }

            if (value === '' && pillEntryKeepsOptionRef.current) {
                editorPendingSaveCancelRef.current?.();

                return;
            }

            if (inputMode.singlePill) {
                if (value === '') {
                    dispatchValueAction({type: 'valueCleared'});

                    if (!keepsEditor) {
                        setPillEntry(false);

                        requestAnimationFrame(() => inputRef.current?.focus());
                    }

                    return;
                }

                if (typeof value === 'string' && isSingleDataPill(value)) {
                    if (!keepsEditor) {
                        setPillEntry(false);
                    }

                    dispatchValueAction({type: 'pillValueSet', value});

                    return;
                }
            }

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
        [
            hasDynamicSwitch,
            INCORRECT_VALUE,
            inputMode.singlePill,
            isFormulaMode,
            maxLength,
            minLength,
            regex,
            setMentionInputValue,
            VALUE_DOES_NOT_MATCH_PATTERN,
        ]
    );

    const requestEditorFocus = useCallback((initialInput?: string) => {
        setEditorFocusRequest({initialInput, token: Date.now()});
    }, []);

    const saveResolvedValue = useCallback(
        (value: unknown) => {
            if (
                !path ||
                !workflow.id ||
                !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
            ) {
                return;
            }

            saveProperty({
                includeInMetadata: shouldIncludeInMetadata(value, custom),
                path,
                type,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                value,
                workflowId: workflow.id,
            });
        },
        [custom, path, type, updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation, workflow.id]
    );

    const handleFormulaSwitch = useCallback(() => {
        saveInputValue.cancel();

        editorPendingSaveCancelRef.current?.();

        const toFormula = !isFormulaMode;

        pillEntryKeepsOptionRef.current = false;

        const convertedValue = toFormula ? toFormulaValue(liveValue, type) : fromFormulaValue(liveValue, type);

        setIsFormulaMode(toFormula);
        setPillEntry(false);

        if (convertedValue === undefined) {
            dispatchValueAction({type: 'valueCleared'});
        } else {
            dispatchValueAction({
                context: {...parameterValueContextRef.current, formulaMode: toFormula, mentionInput: toFormula},
                type: 'parameterValueResolved',
                value: convertedValue,
            });
        }

        saveResolvedValue(convertedValue === undefined ? null : convertedValue);

        const nextRenderer = getPropertyInputMode({
            controlType,
            formulaMode: toFormula,
            isFromAi: false,
            value: convertedValue ?? '',
        }).renderer;

        if (nextRenderer === 'mentions') {
            requestEditorFocus();
        } else {
            requestAnimationFrame(() => inputRef.current?.focus());
        }
    }, [
        controlType,
        isFormulaMode,
        liveValue,
        requestEditorFocus,
        saveInputValue,
        saveResolvedValue,
        setIsFormulaMode,
        type,
    ]);

    const insertPillValue = useCallback(
        (mentionId: string) => {
            const pillValue = `\${${mentionId}}`;

            saveInputValue.cancel();

            pillEntryKeepsOptionRef.current = false;

            setPillEntry(false);

            dispatchValueAction({type: 'pillValueSet', value: pillValue});

            saveResolvedValue(pillValue);

            requestEditorFocus();
        },
        [requestEditorFocus, saveInputValue, saveResolvedValue]
    );

    // `$` typed into a field carries over into the editor; the Dynamic switch opens it empty, for a pill from the panel.
    const startPillEntry = useCallback(
        (initialInput?: string) => {
            if (controlType === 'SELECT') {
                // A select always holds an option, which the pill entry replaces only once a pill is chosen.
                pillEntryKeepsOptionRef.current = true;

                dispatchValueAction({type: 'mentionInputSyncedFromValue', value: ''});
            }

            setPillEntry(true);

            requestEditorFocus(initialInput);
        },
        [controlType, requestEditorFocus]
    );

    const handleNativeKeyDown = useCallback(
        (event: KeyboardEvent<HTMLInputElement>) => {
            const isEmpty = (event.currentTarget as HTMLInputElement).value === '';

            if (
                !isNumericalInput ||
                !isEmpty ||
                expressionEnabled === false ||
                (event.key !== '$' && event.key !== '=')
            ) {
                return;
            }

            event.preventDefault();

            if (event.key === '=') {
                setIsFormulaMode(true);

                requestEditorFocus();

                return;
            }

            startPillEntry('$');
        },
        [expressionEnabled, isNumericalInput, requestEditorFocus, setIsFormulaMode, startPillEntry]
    );

    const handleSelectKeyDown = useCallback(
        (event: KeyboardEvent<HTMLElement>) => {
            if (event.key !== '$' || expressionEnabled === false) {
                return;
            }

            event.preventDefault();

            startPillEntry('$');
        },
        [expressionEnabled, startPillEntry]
    );

    // The editor lost focus without a pill and dropped whatever text it held.
    const handleSinglePillAbandoned = useCallback(() => {
        if (hasDynamicSwitch && !isFormulaMode) {
            // The editor stays up until the Dynamic switch is turned off; focus moves away when a pill is picked from
            // the panel, too. A select keeps its option until a pill replaces it.
            if (pillEntryKeepsOptionRef.current) {
                return;
            }

            dispatchValueAction({type: 'valueCleared'});

            // Text left without a pill is never saved (validateMentionInputValue), so the stored value may still
            // hold the last pill.
            if (typeof propertyParameterValue === 'string' && propertyParameterValue !== '') {
                saveResolvedValue(null);
            }

            return;
        }

        setPillEntry(false);

        dispatchValueAction({type: 'valueCleared'});

        requestAnimationFrame(() => inputRef.current?.focus());
    }, [hasDynamicSwitch, isFormulaMode, propertyParameterValue, saveResolvedValue]);

    const handleDynamicSwitch = useCallback(() => {
        if (!mentionInput) {
            startPillEntry();

            return;
        }

        editorPendingSaveCancelRef.current?.();

        setPillEntry(false);

        if (pillEntryKeepsOptionRef.current) {
            pillEntryKeepsOptionRef.current = false;

            return;
        }

        // Back to the native control: a pill (or text around one) has no constant to turn into.
        dispatchValueAction({type: 'valueCleared'});

        if (typeof propertyParameterValue === 'string' && propertyParameterValue !== '') {
            saveResolvedValue(null);
        }
    }, [mentionInput, propertyParameterValue, saveResolvedValue, startPillEntry]);

    const validateMentionInputValue = useCallback(
        (value: string | number) => {
            const holdsNoPill =
                stringSelect && typeof value === 'string' && !value.startsWith('=') && !value.includes('${');

            return !holdsNoPill && validatePropertyValue(value);
        },
        [stringSelect, validatePropertyValue]
    );

    const handleControlledFormulaSwitch = useCallback(
        (fieldValue: unknown, fieldOnChange: (value: unknown) => void) => {
            const isStringToolExpression =
                isToolsClusterElement &&
                type === 'STRING' &&
                typeof fieldValue === 'string' &&
                fieldValue.startsWith('=');

            const toFormula = !(isFormulaMode || isStringToolExpression);
            const wasFromAi = controlledFromAi === true;

            let convertedValue = toFormula ? toFormulaValue(fieldValue, type) : fromFormulaValue(fieldValue, type);

            if (
                !toFormula &&
                controlType === 'SELECT' &&
                (typeof convertedValue === 'boolean' || typeof convertedValue === 'number')
            ) {
                convertedValue = String(convertedValue);
            }

            setIsFormulaMode(toFormula);
            setControlledFromAi(undefined);

            fieldOnChange(convertedValue ?? (toFormula ? '=' : ''));

            if (toFormula) {
                requestEditorFocus();
            } else {
                controlledExpressionExitRef.current = true;
            }

            if (
                wasFromAi &&
                path &&
                workflow.id &&
                (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
            ) {
                saveProperty({
                    fromAi: false,
                    includeInMetadata: custom,
                    path,
                    type,
                    updateClusterElementParameterMutation,
                    updateWorkflowNodeParameterMutation,
                    value: convertedValue ?? null,
                    workflowId: workflow.id,
                });
            }
        },
        [
            controlType,
            controlledFromAi,
            custom,
            isFormulaMode,
            isToolsClusterElement,
            path,
            requestEditorFocus,
            setIsFormulaMode,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    const handleControlledBuilderFormulaSwitch = useCallback(() => {
        resetOnModeChangeRef.current = true;

        setIsFormulaMode(!isFormulaMode);
    }, [isFormulaMode, setIsFormulaMode]);

    const handleControlledNativeKeyDown = useCallback(
        (event: KeyboardEvent<HTMLInputElement>, fieldValue: unknown, fieldOnChange: (value: unknown) => void) => {
            const isEmpty = fieldValue === '' || fieldValue === null || fieldValue === undefined;

            if (event.key !== '=' || !isNumericalInput || !showFormulaSwitch || !isEmpty) {
                return;
            }

            event.preventDefault();

            setIsFormulaMode(true);

            fieldOnChange('=');

            requestEditorFocus();
        },
        [isNumericalInput, requestEditorFocus, setIsFormulaMode, showFormulaSwitch]
    );

    const handleSelectChange = useCallback(
        (value: string, name: string) => {
            if (
                !currentNode ||
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

            dispatchValueAction({type: 'selectValueChanged', value});

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

                    dispatchValueAction({type: 'selectValueChanged', value: defaultValueString});
                    dispatchValueAction({
                        context: parameterValueContextRef.current,
                        syncDisplayValues: false,
                        type: 'parameterValueResolved',
                        value: actualValue,
                    });

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
            currentNode,
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
                !currentNode ||
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

            dispatchValueAction({propertyParameterValue: value, type: 'multiSelectValueChanged', value});

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
            currentNode,
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

            const {savePayload, value} = computeFromAiToggle({
                custom,
                fromAi,
                fromAiExpression,
                hasPath: !!path,
                hasWorkflowId: !!workflow.id,
            });

            dispatchValueAction({
                context: parameterValueContextRef.current,
                syncDisplayValues: false,
                type: 'parameterValueResolved',
                value,
            });

            const editorContent = value.startsWith('=') ? value.substring(1) : value;

            if (fromAi) {
                editorRef.current?.commands.setContent(value);
                editorRef.current?.setEditable(false);
            } else {
                // "Customize AI generation": reveal the =fromAi(...) expression as an editable formula.
                setFormulaModeState(true);

                editorRef.current?.commands.setContent(editorContent);
                editorRef.current?.setEditable(true);
                editorRef.current?.commands.focus();
            }

            if (savePayload) {
                saveProperty({
                    ...savePayload,
                    path,
                    type,
                    updateClusterElementParameterMutation,
                    updateWorkflowNodeParameterMutation,
                    workflowId: workflow.id,
                });
            }
        },
        [
            custom,
            fromAiExpression,
            path,
            setFormulaModeState,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

    // A fresh array is built on every parameter change, so setting it unconditionally would
    // always change state identity and force another render pass even when the resolved
    // dependency values are unchanged.
    const setLookupDependsOnValuesIfChanged = useCallback((nextValues: Array<unknown>) => {
        setLookupDependsOnValues((previousValues) => {
            if (
                previousValues &&
                previousValues.length === nextValues.length &&
                previousValues.every((previousValue, index) => Object.is(previousValue, nextValues[index]))
            ) {
                return previousValues;
            }

            return nextValues;
        });
    }, []);

    // set error state
    useEffect(() => {
        if (formState && name && path) {
            setHasError(
                !!(
                    formState.touchedFields[path] &&
                    formState.touchedFields[path]![name] &&
                    formState.errors[path] &&
                    (formState.errors[path] as never)[name]
                )
            );
        }
    }, [formState, name, path]);

    // set propertyParameterValue on initial render
    useEffect(() => {
        if (control) {
            return;
        }

        if (!name || !currentNode || !currentNode.parameters) {
            return;
        }

        const {parameters} = currentNode;

        const encodedParameters = encodeParameters(parameters);
        const encodedPath = path ? encodePath(path) : undefined;

        const isExpressionValue = typeof propertyParameterValue === 'string' && propertyParameterValue.startsWith('=');

        if (Object.keys(parameters).length && (!propertyParameterValue || propertyParameterValue === defaultValue)) {
            if (parameterValue === undefined) {
                if (!path || !encodedPath) {
                    if (isSavedFormulaValue(parameters[name])) {
                        setFormulaModeState(true);
                    }

                    resolveParameterValue(parameters[name]);

                    return;
                }

                const valueFromDefinition = safeResolvePath(encodedParameters, encodedPath);

                if (valueFromDefinition !== undefined && valueFromDefinition !== null) {
                    if (isSavedFormulaValue(valueFromDefinition)) {
                        setFormulaModeState(true);
                    }

                    resolveParameterValue(valueFromDefinition);
                } else {
                    if (isSavedFormulaValue(encodedParameters[name])) {
                        setFormulaModeState(true);
                    }

                    resolveParameterValue(encodedParameters[name]);
                }
            }
        } else if (isExpressionValue) {
            setMentionInputValue(propertyParameterValue.substring(1));
        }

        const canSaveDefaultValue =
            !!encodedPath &&
            (objectName === undefined || dynamicPropertySource === objectName) &&
            !!(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation);

        const storedValue = encodedPath ? safeResolvePath(encodedParameters, encodedPath) : undefined;

        const shouldSaveHiddenProperty = hidden && canSaveDefaultValue && storedValue !== defaultValue;

        const shouldSaveRequiredDefaultValue =
            required &&
            !hidden &&
            canSaveDefaultValue &&
            (parameterValue === undefined || parameterValue === defaultValue) &&
            (storedValue === undefined || storedValue === null) &&
            defaultValue !== '';

        if (shouldSaveRequiredDefaultValue) {
            resolveParameterValue(defaultValue);
        }

        if (shouldSaveHiddenProperty || shouldSaveRequiredDefaultValue) {
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
        if (control || !path || !currentNode?.parameters) {
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

        if (!Object.keys(currentNode.parameters).length) {
            return;
        }

        const encodedParameters = encodeParameters(currentNode.parameters);
        const encodedPath = encodePath(path);

        if (!encodedPath) {
            return;
        }

        const valueFromDefinition = safeResolvePath(encodedParameters, encodedPath);

        const effectiveValue = parameterValue !== undefined ? parameterValue : valueFromDefinition;

        const nextFormulaMode = controlType === 'FORMULA_MODE' || isSavedFormulaValue(effectiveValue);

        setFormulaModeState(nextFormulaMode);

        const getNextContext = (nextValue: unknown): Partial<ParameterValueContextI> => {
            const nextMentionInput =
                getPropertyInputMode({controlType, formulaMode: nextFormulaMode, isFromAi: false, value: nextValue})
                    .renderer === 'mentions';

            return {
                formulaMode: nextFormulaMode,
                isNumericalInput: !nextMentionInput && (controlType === 'INTEGER' || controlType === 'NUMBER'),
                mentionInput: nextMentionInput,
            };
        };

        if (effectiveValue !== undefined && effectiveValue !== null) {
            if (type === 'BOOLEAN' && typeof effectiveValue === 'boolean') {
                resolveParameterValue(effectiveValue.toString(), {context: getNextContext(effectiveValue.toString())});

                return;
            }

            resolveParameterValue(effectiveValue, {context: getNextContext(effectiveValue)});

            return;
        }

        const fallbackParameterValue = parameterValue !== undefined ? parameterValue : defaultValue;

        dispatchValueAction({type: 'valueCleared'});

        resolveParameterValue(fallbackParameterValue, {context: getNextContext(fallbackParameterValue)});
    }, [
        control,
        controlType,
        currentNode?.parameters,
        defaultValue,
        parameterValue,
        path,
        resolveParameterValue,
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

    // The one-way distribution of propertyParameterValue into the display values now happens
    // inside propertyValueReducer's `parameterValueResolved` case, so resolving a parameter and
    // showing it is a single state transition instead of a set-then-sync effect round trip.

    // Set options lookup dependencies from the saved workflow parameters. When `control`
    // is provided (array item / dialog form), FormLookupValuesWatcher subscribes to
    // react-hook-form values instead so in-progress edits trigger a refetch immediately.
    useEffect(() => {
        if (control) {
            return;
        }

        if (!optionsDataSource?.optionsLookupDependsOn) {
            return;
        }

        if (!currentNode?.parameters) {
            return;
        }

        const optionsLookupDependsOnValues: unknown[] = optionsDataSource.optionsLookupDependsOn.map(
            (optionLookupDependency) => {
                const resolvedValue = safeResolvePath(
                    currentNode.parameters,
                    optionLookupDependency.replace('[index]', `[${arrayIndex}]`)
                );

                if (typeof resolvedValue === 'string' && resolvedValue.startsWith('=fromAi(')) {
                    return undefined;
                }

                return resolvedValue;
            }
        );

        setLookupDependsOnValuesIfChanged(optionsLookupDependsOnValues);
    }, [
        arrayIndex,
        control,
        currentNode?.parameters,
        optionsDataSource?.optionsLookupDependsOn,
        setLookupDependsOnValuesIfChanged,
    ]);

    // See comment above; same control-present carve-out.
    useEffect(() => {
        if (control) {
            return;
        }

        if (!propertiesDataSource?.propertiesLookupDependsOn) {
            return;
        }

        if (!currentNode?.parameters) {
            return;
        }

        const propertiesLookupDependsOnValues: unknown[] = propertiesDataSource.propertiesLookupDependsOn.map(
            (propertyLookupDependency) => {
                const resolvedValue = safeResolvePath(
                    currentNode.parameters,
                    propertyLookupDependency.replace('[index]', `[${arrayIndex}]`)
                );

                if (typeof resolvedValue === 'string' && resolvedValue.startsWith('=fromAi(')) {
                    return undefined;
                }

                return resolvedValue;
            }
        );

        setLookupDependsOnValuesIfChanged(propertiesLookupDependsOnValues);
    }, [
        arrayIndex,
        control,
        currentNode?.parameters,
        propertiesDataSource?.propertiesLookupDependsOn,
        setLookupDependsOnValuesIfChanged,
    ]);

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

        const valueFromWorkflowDefinition = safeResolvePath(encodedParameters, encodedPath);

        const nextParameterValue =
            parameterValueRef.current !== undefined ? parameterValueRef.current : valueFromWorkflowDefinition;

        resolveParameterValue(nextParameterValue, {authoritativeValue: parameterValueRef.current});
        // eslint-disable-next-line react-hooks/exhaustive-deps -- sync when workflow JSON changes; read latest parameterValue via ref
    }, [workflow.definition]);

    // reset all values when currentNode.operationName changes
    useEffect(() => {
        const parameterDefaultValue: string | string[] =
            property.defaultValue !== undefined ? property.defaultValue : '';

        if (previousOperationName) {
            dispatchValueAction({defaultValue: parameterDefaultValue, type: 'valuesResetToDefault'});
        }
    }, [currentNode?.operationName, previousOperationName, property.defaultValue]);

    // handle NULL type property saving
    useEffect(() => {
        if (
            type === 'NULL' &&
            propertyParameterValue === undefined &&
            currentNode &&
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
        if (displayCondition && currentNode?.displayConditions?.[displayCondition]) {
            setIsFetchingCurrentDisplayCondition(true);

            if (isDisplayConditionsSuccess) {
                setIsFetchingCurrentDisplayCondition(false);
            }
        }
    }, [displayCondition, currentNode?.displayConditions, isDisplayConditionsSuccess]);

    useEffect(() => {
        if (isFormulaMode && resetOnModeChangeRef.current && controlledFormulaOnChangeRef.current) {
            resetOnModeChangeRef.current = false;

            controlledFormulaOnChangeRef.current('=');
        }
    }, [isFormulaMode, resetOnModeChangeRef]);

    const isLoadingDisplayCondition = !!(
        displayCondition &&
        type !== 'ARRAY' &&
        type !== 'OBJECT' &&
        (isDisplayConditionsPending ||
            (displayConditionsQuery &&
                currentNode?.displayConditions?.[displayCondition] &&
                isFetchingCurrentDisplayCondition))
    );

    return {
        calculatedPath: path,
        controlType,
        controlledBlurError,
        controlledExpressionExitRef,
        controlledFormulaOnChangeRef,
        controlledFromAi,
        currentNode,
        defaultValue,
        description,
        displayCondition,
        editorFocusRequest,
        editorPendingSaveCancelRef,
        editorRef,
        errorMessage,
        expressionEnabled,
        formattedOptions,
        fromAiExpression,
        handleCodeEditorChange,
        handleControlledBlur,
        handleControlledBuilderFormulaSwitch,
        handleControlledFormulaSwitch,
        handleControlledNativeFromAiClick,
        handleControlledNativeKeyDown,
        handleDeleteCustomPropertyClick,
        handleDynamicSwitch,
        handleFormulaSwitch,
        handleFromAiClick: hideFromAi ? undefined : handleFromAiClick,
        handleFromAiToggle,
        handleInputChange,
        handleInputClear,
        handleJsonSchemaBuilderChange,
        handleMentionInputValueChange,
        handleMultiSelectChange,
        handleNativeKeyDown,
        handleSelectChange,
        handleSelectKeyDown,
        handleSinglePillAbandoned,
        hasDynamicSwitch,
        hasError,
        hidden,
        inputMode,
        inputRef,
        inputValue,
        insertPillValue,
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
        optionsLoadedDynamically,
        placeholder,
        propertiesDataSource,
        propertyParameterValue,
        required,
        resetOnModeChangeRef,
        selectValue,
        setControlledFromAi,
        setIsFormulaMode,
        setLookupDependsOnValues,
        setSelectValue,
        showFormulaSwitch,
        startPillEntry,
        type,
        typeIcon,
        validateMentionInputValue,
        validatePropertyValue,
        workflow,
    };
};

export default useProperty;
