import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import deleteProperty from '@/pages/platform/workflow-editor/utils/deleteProperty';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {
    ControlType,
    DeleteClusterElementParameter200Response,
    DeleteClusterElementParameterOperationRequest,
    DeleteWorkflowNodeParameterRequest,
    GetClusterElementParameterDisplayConditions200Response,
    Option,
    OptionsDataSource,
    PropertiesDataSource,
    UpdateClusterElementParameterOperationRequest,
    UpdateWorkflowNodeParameterRequest,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {ArrayPropertyType, ClusterElementItemType, ComponentType, NodeDataType, PropertyAllType} from '@/shared/types';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {Editor} from '@tiptap/react';
import {usePrevious} from '@uidotdev/usehooks';
import resolvePath from 'object-resolve-path';
import {ChangeEvent, ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Control, FieldValues, FormState} from 'react-hook-form';
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

export const MENTION_INPUT_PROPERTY_CONTROL_TYPES = [
    'EMAIL',
    'PHONE',
    'RICH_TEXT',
    'TEXT',
    'TEXT_AREA',
    'URL',
] as const;

const isString = (value: unknown): value is string => typeof value === 'string';
const isStringArray = (value: unknown): value is string[] =>
    Array.isArray(value) && value.every((item) => typeof item === 'string');

interface GetFetchedStateProps {
    currentComponent?: ComponentType;
    displayCondition?: string;
    displayConditionsQuery: UseQueryResult<GetClusterElementParameterDisplayConditions200Response, Error> | undefined;
    isDisplayConditionsPending: boolean;
    isFetchingCurrentDisplayCondition: boolean;
    type?: string;
}

const getFetchedState = ({
    currentComponent,
    displayCondition,
    displayConditionsQuery,
    isDisplayConditionsPending,
    isFetchingCurrentDisplayCondition,
    type,
}: GetFetchedStateProps) => {
    if (displayCondition && isDisplayConditionsPending && type !== 'ARRAY' && type !== 'OBJECT') {
        return true;
    }

    if (
        displayConditionsQuery &&
        displayCondition &&
        currentComponent?.displayConditions?.[displayCondition] &&
        (isFetchingCurrentDisplayCondition || isDisplayConditionsPending) &&
        type !== 'ARRAY' &&
        type !== 'OBJECT'
    ) {
        return true;
    }

    return false;
};

type UsePropertyReturnType = {
    controlType?: ControlType;
    currentComponent: ComponentType | undefined;
    currentNode: NodeDataType | undefined;
    custom?: boolean;
    defaultValue: string;
    deleteClusterElementParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteClusterElementParameterOperationRequest,
        unknown
    >;
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteWorkflowNodeParameterRequest,
        unknown
    >;
    description?: string;
    displayCondition?: string;
    editorRef: React.RefObject<Editor>;
    errorMessage: string;
    expressionEnabled?: boolean;
    formattedOptions: Array<Option>;
    handleCodeEditorChange: (value?: string) => void;
    handleDeleteCustomPropertyClick: (path: string) => void;
    handleInputChange: (event: ChangeEvent<HTMLInputElement> | ChangeEvent<HTMLTextAreaElement>) => void;
    handleInputTypeSwitchButtonClick: () => void;
    handleJsonSchemaBuilderChange: (value?: SchemaRecordType) => void;
    handleMultiSelectChange: (value: string[]) => void;
    handleSelectChange: (value: string, name: string) => void;
    hasError: boolean;
    hidden?: boolean;
    inputRef: React.RefObject<HTMLInputElement>;
    inputValue: string;
    isDisplayConditionsFetched: boolean;
    isDisplayConditionsPending: boolean;
    isFetchingCurrentDisplayCondition: boolean;
    isFormulaMode: boolean;
    isNumericalInput: boolean;
    isValidControlType: boolean;
    label?: string;
    languageId?: string;
    lookupDependsOnValues?: Array<unknown>;
    maxLength?: number;
    maxValue?: number;
    memoizedPath?: string;
    mentionInput: boolean;
    mentionInputValue: string;
    minLength?: number;
    minValue?: number;
    multiSelectValue: string[];
    name: string;
    numberPrecision?: number;
    options?: PropertyAllType['options'];
    optionsDataSource?: OptionsDataSource;
    placeholder: string;
    properties?: PropertyAllType['properties'];
    propertiesDataSource?: PropertiesDataSource;
    propertyParameterValue: unknown;
    required: boolean;
    selectValue: string;
    setIsFormulaMode: React.Dispatch<React.SetStateAction<boolean>>;
    setSelectValue: React.Dispatch<React.SetStateAction<string>>;
    showInputTypeSwitchButton: boolean;
    getFetchedState: (props: GetFetchedStateProps) => boolean;
    type?: PropertyAllType['type'];
    typeIcon: ReactNode;
    updateClusterElementParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response & {workflowNodeName?: string},
        Error,
        UpdateClusterElementParameterOperationRequest,
        unknown
    >;
    updateWorkflowNodeParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response & {workflowNodeName?: string},
        Error,
        UpdateWorkflowNodeParameterRequest,
        unknown
    >;
    workflow: Workflow;
};

interface UsePropertyProps {
    arrayIndex?: number;
    arrayName?: string;
    control?: Control<FieldValues, FieldValues>;
    controlPath?: string;
    displayConditionsQuery?: UseQueryResult<GetClusterElementParameterDisplayConditions200Response, Error>;
    formState?: FormState<FieldValues>;
    objectName?: string;
    operationName?: string;
    parameterValue?: unknown;
    parentArrayItems?: Array<ArrayPropertyType>;
    path?: string;
    property: PropertyAllType;
}

export const useProperty = ({
    arrayIndex,
    control,
    controlPath = 'parameters',
    displayConditionsQuery,
    formState,
    objectName,
    parameterValue,
    path,
    property,
}: UsePropertyProps): UsePropertyReturnType => {
    const [errorMessage, setErrorMessage] = useState('');
    const [hasError, setHasError] = useState(false);
    const [inputValue, setInputValue] = useState(() => {
        if (
            !control &&
            property.controlType &&
            MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(
                property.controlType as (typeof MENTION_INPUT_PROPERTY_CONTROL_TYPES)[number]
            )
        ) {
            return '';
        }

        if (!property.controlType || !INPUT_PROPERTY_CONTROL_TYPES.includes(property.controlType)) {
            return '';
        }

        return property.defaultValue || '';
    });
    const [isFormulaMode, setIsFormulaMode] = useState(false);
    const [lookupDependsOnValues, setLookupDependsOnValues] = useState<Array<unknown> | undefined>();
    const [mentionInputValue, setMentionInputValue] = useState(property.defaultValue || '');
    const [mentionInput, setMentionInput] = useState(
        !control &&
            property.controlType &&
            MENTION_INPUT_PROPERTY_CONTROL_TYPES.includes(
                property.controlType as (typeof MENTION_INPUT_PROPERTY_CONTROL_TYPES)[number]
            )
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

    const memoizedPath = useMemo(() => {
        let computedPath = path;

        if (!computedPath && name) {
            computedPath = name;
        }

        if (control) {
            computedPath = `${controlPath}.${name}`;
        }

        if (computedPath === objectName) {
            computedPath = `${computedPath}.${name}`;
        }

        if (objectName && !computedPath?.includes(objectName)) {
            computedPath = `${objectName}.${computedPath}`;
        }

        if (computedPath) {
            computedPath = decodePath(computedPath);
        }

        return computedPath;
    }, [path, name, control, controlPath, objectName]);

    const formattedOptions = useMemo(
        () =>
            options
                ?.map((option) => {
                    if (option.value === '') {
                        return null;
                    }

                    return option;
                })
                .filter((option) => option !== null) || [],
        [options]
    );

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

    if (displayCondition) {
        const displayConditionIndexes: number[] = [];
        const bracketedNumberRegex = /\[(\d+)\]/g;
        let match;

        while ((match = bracketedNumberRegex.exec(memoizedPath!)) !== null) {
            displayConditionIndexes.push(parseInt(match[1], 10));
        }

        displayConditionIndexes.forEach((index) => {
            displayCondition = displayCondition!.replace(`[index]`, `[${index}]`);
        });
    }

    const memoizedWorkflowTask = useMemo(() => {
        if (!currentNode?.name || !workflow.triggers || !workflow.tasks) {
            return null;
        }

        return [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])].find((node) => node.name === currentNode.name);
    }, [currentNode?.name, workflow.triggers, workflow.tasks]);

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

    const saveInputValue = useDebouncedCallback(() => {
        if (
            !currentComponent ||
            !workflow ||
            !name ||
            !memoizedPath ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
        ) {
            return;
        }

        const valueToSave = latestValueRef.current;

        isSavingRef.current = true;

        saveProperty({
            includeInMetadata: custom,
            path: memoizedPath,
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
            !memoizedPath ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
            return;
        }

        saveProperty({
            includeInMetadata: custom,
            path: memoizedPath,
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
            !memoizedPath ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
            return;
        }

        saveProperty({
            includeInMetadata: property.custom,
            path: memoizedPath,
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
            !memoizedPath ||
            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
            !workflow.id
        ) {
            return;
        }

        const parentParameterValue = resolvePath(currentComponent.parameters ?? {}, memoizedPath);

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
            path: memoizedPath,
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
                !memoizedPath ||
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
                        path: memoizedPath,
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
                        memoizedPath,
                        deleteWorkflowNodeParameterMutation!,
                        deleteClusterElementParameterMutation
                    );
                }

                return;
            }

            saveProperty({
                includeInMetadata: custom,
                path: memoizedPath,
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
            memoizedPath,
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
                !memoizedPath ||
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
                path: memoizedPath,
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
            memoizedPath,
            propertyParameterValue,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]
    );

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
        if (formState && name && memoizedPath) {
            setHasError(
                formState.touchedFields[memoizedPath] &&
                    formState.touchedFields[memoizedPath]![name] &&
                    formState.errors[memoizedPath] &&
                    (formState.errors[memoizedPath] as never)[name]
            );
        }
    }, [formState, name, memoizedPath]);

    // set propertyParameterValue on initial render
    useEffect(() => {
        if (!name || !currentComponent || !currentComponent.parameters) {
            return;
        }

        const {parameters} = currentComponent;

        if (Object.keys(parameters).length && (!propertyParameterValue || propertyParameterValue === defaultValue)) {
            if (!memoizedPath) {
                setPropertyParameterValue(parameters[name]);

                return;
            }

            const encodedParameters = encodeParameters(parameters);
            const encodedPath = encodePath(memoizedPath);

            const paramValue = resolvePath(encodedParameters, encodedPath);

            if (paramValue !== undefined || paramValue !== null) {
                setPropertyParameterValue(paramValue);

                if (typeof paramValue === 'string' && paramValue.startsWith('=')) {
                    setMentionInput(true);
                    setIsFormulaMode(true);
                }
            } else {
                const paramValue = encodedParameters[name];
                if (paramValue !== undefined && paramValue !== null) {
                    setPropertyParameterValue(paramValue);
                }
            }
        }

        // save hidden property to definition on render
        if (
            hidden &&
            currentComponent &&
            memoizedPath &&
            objectName === undefined &&
            (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) &&
            resolvePath(currentComponent.parameters ?? {}, memoizedPath) !== defaultValue
        ) {
            const saveDefaultValue = () => {
                saveProperty({
                    path: memoizedPath,
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

        if (isString(propertyParameterValue) && propertyParameterValue.startsWith('=')) {
            setMentionInputValue(propertyParameterValue.substring(1));
        }

        if (
            !mentionInput &&
            controlType &&
            INPUT_PROPERTY_CONTROL_TYPES.includes(controlType) &&
            inputValue === '' &&
            propertyParameterValue
        ) {
            setInputValue(String(propertyParameterValue));
        }

        if (!mentionInput && controlType === 'JSON_SCHEMA_BUILDER' && propertyParameterValue !== undefined) {
            setInputValue(String(propertyParameterValue));
        }

        if (controlType === 'SELECT' && propertyParameterValue !== undefined) {
            if (propertyParameterValue === null) {
                setSelectValue('null');
            } else if (propertyParameterValue !== undefined) {
                if (type === 'BOOLEAN') {
                    setSelectValue(String(propertyParameterValue));
                } else {
                    setSelectValue(String(propertyParameterValue));
                }
            }
        }

        if (controlType === 'MULTI_SELECT' && propertyParameterValue !== undefined) {
            if (propertyParameterValue === null) {
                setMultiSelectValue([]);
            } else if (isStringArray(propertyParameterValue)) {
                setMultiSelectValue(propertyParameterValue);
            }
        }

        if (
            isNumericalInput &&
            (inputValue === null || inputValue === undefined) &&
            (propertyParameterValue !== null || propertyParameterValue !== undefined) &&
            parameterValue
        ) {
            setInputValue(String(propertyParameterValue));
        }

        if (
            isNumericalInput &&
            (inputValue !== null || inputValue !== undefined) &&
            (propertyParameterValue !== null || propertyParameterValue !== undefined) &&
            propertyParameterValue !== inputValue
        ) {
            setInputValue(String(propertyParameterValue));
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
        if (!workflow.definition || !currentNode?.name || !name || !memoizedPath) {
            return;
        }

        const encodedParameters = encodeParameters(
            (memoizedWorkflowTask?.parameters || memoizedClusterElementTask?.parameters) ?? {}
        );

        const encodedPath = encodePath(memoizedPath);

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
            memoizedPath &&
            (updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation)
        ) {
            const saveDefaultValue = () => {
                saveProperty({
                    includeInMetadata: custom,
                    path: memoizedPath,
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

    return {
        controlType,
        currentComponent,
        currentNode,
        custom,
        defaultValue,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        description,
        displayCondition,
        editorRef,
        errorMessage,
        expressionEnabled,
        formattedOptions: formattedOptions || [],
        getFetchedState,
        handleCodeEditorChange,
        handleDeleteCustomPropertyClick,
        handleInputChange,
        handleInputTypeSwitchButtonClick,
        handleJsonSchemaBuilderChange,
        handleMultiSelectChange,
        handleSelectChange,
        hasError,
        hidden,
        inputRef,
        inputValue,
        isDisplayConditionsFetched,
        isDisplayConditionsPending,
        isFetchingCurrentDisplayCondition,
        isFormulaMode,
        isNumericalInput: isNumericalInput || false,
        isValidControlType: isValidControlType || false,
        label,
        languageId,
        lookupDependsOnValues,
        maxLength,
        maxValue,
        memoizedPath,
        mentionInput: mentionInput || false,
        mentionInputValue,
        minLength,
        minValue,
        multiSelectValue,
        name: name || '',
        numberPrecision,
        options,
        optionsDataSource,
        placeholder,
        properties,
        propertiesDataSource,
        propertyParameterValue,
        required,
        selectValue,
        setIsFormulaMode,
        setSelectValue,
        showInputTypeSwitchButton,
        type,
        typeIcon,
        updateClusterElementParameterMutation,
        updateWorkflowNodeParameterMutation,
        workflow,
    };
};
