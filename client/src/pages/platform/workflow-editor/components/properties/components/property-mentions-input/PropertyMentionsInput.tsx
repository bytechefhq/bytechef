import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {getRandomId} from '@/shared/util/random-utils';
import {
    DragEvent,
    ForwardedRef,
    ReactNode,
    Suspense,
    forwardRef,
    lazy,
    memo,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react';

import './PropertyMentionsInput.css';

import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import InputTypeSwitchButton from '@/pages/platform/workflow-editor/components/properties/components/InputTypeSwitchButton';
import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {ControlType} from '@/shared/middleware/platform/configuration';
import {Editor} from '@tiptap/react';
import {CircleQuestionMarkIcon, EqualIcon, TriangleAlertIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const PropertyMentionsInputEditorSheet = lazy(
    () =>
        import('@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditorSheet')
);

interface PropertyMentionsInputProps {
    className?: string;
    controlType?: ControlType;
    defaultValue?: string;
    deletePropertyButton?: ReactNode;
    description?: string;
    error?: boolean;
    errorMessage?: string;
    handleFromAiClick?: (fromAi: boolean) => void;
    handleInputTypeSwitchButtonClick?: () => void;
    isFromAi?: boolean;
    isFormulaMode?: boolean;
    label?: string;
    leadingIcon?: ReactNode;
    onValueChange?: (value: string | number) => void;
    path?: string;
    placeholder?: string;
    required?: boolean;
    setIsFormulaMode?: (isFormulaMode: boolean) => void;
    showInputTypeSwitchButton?: boolean;
    type?: string;
    value?: string;
}

const PropertyMentionsInput = forwardRef<Editor, PropertyMentionsInputProps>(
    (
        {
            className,
            controlType,
            defaultValue,
            deletePropertyButton,
            description,
            error,
            errorMessage,
            handleFromAiClick,
            handleInputTypeSwitchButtonClick,
            isFormulaMode,
            isFromAi,
            label,
            leadingIcon,
            onValueChange,
            path,
            placeholder,
            required = false,
            setIsFormulaMode,
            showInputTypeSwitchButton = false,
            type = 'STRING',
            value,
        },
        ref: ForwardedRef<Editor>
    ) => {
        const [isFocused, setIsFocused] = useState(false);
        const isInitialLoadRef = useRef(true);
        const localEditorRef = useRef<Editor | null>(null);

        const {componentDefinitions, dataPills, taskDispatcherDefinitions, workflow} = useWorkflowDataStore(
            useShallow((state) => ({
                componentDefinitions: state.componentDefinitions,
                dataPills: state.dataPills,
                taskDispatcherDefinitions: state.taskDispatcherDefinitions,
                workflow: state.workflow,
            }))
        );

        const {focusedInput, setFocusedInput, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                focusedInput: state.focusedInput,
                setFocusedInput: state.setFocusedInput,
                workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
            }))
        );

        const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);

        const onFocus = (editor: Editor) => {
            setFocusedInput(editor);

            if (workflowNodeDetailsPanelOpen) {
                setDataPillPanelOpen(true);
            }
        };

        const elementId = useMemo(() => `mentions-input-${getRandomId()}`, []);
        const labelId = useMemo(() => `${elementId}-label`, [elementId]);

        const handleEditorValueChange = useCallback(
            (newValue?: string | number) => {
                if (typeof newValue === 'string') {
                    const startsWithEquals = newValue.trim().startsWith('=');

                    if (startsWithEquals && setIsFormulaMode) {
                        setIsFormulaMode(true);

                        const processedValue = newValue.trim().substring(1);

                        localEditorRef.current?.commands?.setContent(processedValue);

                        return false;
                    }
                }

                return true;
            },
            [setIsFormulaMode]
        );

        const getPropertyMentionsInputEditorRef = useCallback(
            (instance: Editor | null) => {
                localEditorRef.current = instance;

                if (typeof ref === 'function') {
                    ref(instance);
                } else if (ref && 'current' in ref) {
                    ref.current = instance;
                }
            },
            [ref]
        );

        const handleDragOver = useCallback((event: DragEvent<HTMLDivElement>) => {
            if (event.dataTransfer.types.includes('application/bytechef-datapill')) {
                event.preventDefault();
                event.dataTransfer.dropEffect = 'copy';
            }
        }, []);

        const handleDragEnter = useCallback((event: DragEvent<HTMLDivElement>) => {
            if (event.dataTransfer.types.includes('application/bytechef-datapill')) {
                event.preventDefault();
            }
        }, []);

        // Ensure localEditorRef stays in sync with parent ref
        useEffect(() => {
            if (ref && typeof ref !== 'function' && 'current' in ref && ref.current && !localEditorRef.current) {
                localEditorRef.current = ref.current;
            }
        }, [ref]);

        useEffect(() => {
            if (!focusedInput || !localEditorRef.current) {
                setIsFocused(false);

                return;
            }

            setIsFocused(focusedInput === localEditorRef.current);
        }, [focusedInput]);

        // Check initial value for formula mode
        useEffect(() => {
            if (isInitialLoadRef.current && setIsFormulaMode) {
                const initialValue = value || defaultValue;

                if (typeof initialValue === 'string' && initialValue.trim().startsWith('=')) {
                    setIsFormulaMode(true);
                }

                isInitialLoadRef.current = false;
            }
        }, [value, defaultValue, setIsFormulaMode]);

        return (
            <fieldset className={twMerge('w-full', label && 'space-y-1')}>
                {(label || description || showInputTypeSwitchButton) && (
                    <div className={twMerge('flex w-full items-center justify-between', !label && 'justify-end')}>
                        {label && (
                            <div className="flex items-center">
                                <Label
                                    className={twMerge(description && 'mr-1', 'leading-normal')}
                                    htmlFor={elementId}
                                    id={labelId}
                                >
                                    {label}

                                    {required && <RequiredMark />}
                                </Label>

                                {description && (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <CircleQuestionMarkIcon className="size-4 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-tooltip-sm">{description}</TooltipContent>
                                    </Tooltip>
                                )}
                            </div>
                        )}

                        <div className="flex items-center gap-1">
                            {(controlType === 'RICH_TEXT' ||
                                controlType === 'TEXT_AREA' ||
                                controlType === 'FORMULA_MODE') && (
                                <Suspense fallback={<Skeleton className="size-6" />}>
                                    <PropertyMentionsInputEditorSheet
                                        componentDefinitions={componentDefinitions}
                                        controlType={controlType}
                                        dataPills={dataPills}
                                        path={path}
                                        placeholder={placeholder}
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                        title={label ?? ''}
                                        type={type}
                                        value={value}
                                        workflow={workflow}
                                    />
                                </Suspense>
                            )}

                            {showInputTypeSwitchButton && handleInputTypeSwitchButtonClick && (
                                <InputTypeSwitchButton handleClick={handleInputTypeSwitchButtonClick} mentionInput />
                            )}

                            {deletePropertyButton}
                        </div>
                    </div>
                )}

                <div
                    className={twMerge(
                        'relative flex items-center rounded-md border-gray-200 shadow-sm transition-colors',
                        error && 'border-rose-300 text-rose-900 ring-rose-300 focus-within:ring-rose-300',
                        isFocused && 'ring-2 ring-blue-500',
                        label && 'mt-1',
                        leadingIcon && 'rounded-md border'
                    )}
                    onDragEnter={handleDragEnter}
                    onDragOver={handleDragOver}
                    title={controlType}
                >
                    {leadingIcon && (
                        <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center rounded-l-md border-r bg-gray-100 px-3">
                            {isFormulaMode ? <EqualIcon className="size-4" /> : leadingIcon}
                        </span>
                    )}

                    <div
                        className={twMerge(
                            'property-mentions-editor flex h-full min-h-[34px] w-full rounded-md bg-white',
                            leadingIcon && 'border-0 pl-10 pr-0.5',
                            isFromAi && 'is-from-ai',
                            className
                        )}
                    >
                        <PropertyMentionsInputEditor
                            className="px-2 py-[0.44rem]"
                            componentDefinitions={componentDefinitions}
                            controlType={controlType}
                            dataPills={dataPills}
                            elementId={elementId}
                            handleFromAiClick={handleFromAiClick}
                            isFormulaMode={isFormulaMode}
                            isFromAi={isFromAi}
                            labelId={labelId}
                            onChange={(editorValue) => handleEditorValueChange(editorValue)}
                            onFocus={onFocus}
                            onValueChange={onValueChange}
                            path={path}
                            placeholder={placeholder}
                            ref={getPropertyMentionsInputEditorRef}
                            setIsFormulaMode={setIsFormulaMode}
                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                            type={type}
                            value={value || defaultValue}
                            workflow={workflow}
                        />
                    </div>

                    {error && (
                        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                            <TriangleAlertIcon aria-hidden="true" className="size-5 text-rose-500" />
                        </div>
                    )}
                </div>

                {error && (
                    <p className="mt-2 text-sm text-rose-600" role="alert">
                        {errorMessage || ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED}
                    </p>
                )}
            </fieldset>
        );
    }
);

PropertyMentionsInput.displayName = 'PropertyMentionsInput';

export default memo(PropertyMentionsInput);
