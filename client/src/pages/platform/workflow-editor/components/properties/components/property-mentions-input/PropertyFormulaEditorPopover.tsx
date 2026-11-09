import Button from '@/components/Button/Button';
import {Popover, PopoverAnchor, PopoverClose, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import ExpressionHelpNote from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/ExpressionHelpNote';
import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {MaximizeIcon, SquareFunctionIcon, XIcon} from 'lucide-react';
import {MutableRefObject, useCallback, useEffect, useRef, useState} from 'react';

interface PropertyFormulaEditorPopoverProps {
    cancelPendingSaveRef?: MutableRefObject<(() => void) | null>;
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    disableAutoSave?: boolean;
    expressionEnabled?: boolean;
    onEditorFocus: (editor: Editor) => void;
    onValueChange?: (value: string | number) => void;
    path?: string;
    placeholder?: string;
    setIsFormulaMode?: (isFormulaMode: boolean) => void;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
    title: string;
    toolProperty?: boolean;
    type: string;
    validateBeforeSave?: (value: string | number) => boolean;
    value?: string;
    workflow: Workflow;
}

interface OutsideInteractionEventI {
    preventDefault: () => void;
    target: EventTarget | null;
}

const PROPERTY_OVERLAP_MARGIN = 6;

const keepOpenForDataPillPanel = (event: OutsideInteractionEventI) => {
    if (event.target instanceof Element && event.target.closest('[data-data-pill-panel]')) {
        event.preventDefault();
    }
};

const PropertyFormulaEditorPopover = ({
    cancelPendingSaveRef,
    componentDefinitions,
    controlType,
    dataPills,
    disableAutoSave,
    expressionEnabled,
    onEditorFocus,
    onValueChange,
    path,
    placeholder,
    setIsFormulaMode,
    taskDispatcherDefinitions,
    title,
    toolProperty,
    type,
    validateBeforeSave,
    value,
    workflow,
}: PropertyFormulaEditorPopoverProps) => {
    const [anchorHeight, setAnchorHeight] = useState(0);
    const [open, setOpen] = useState(false);

    const anchorRef = useRef<HTMLDivElement>(null);
    const editorRef = useRef<Editor | null>(null);
    const focusedEditorRef = useRef<Editor | null>(null);

    const clearPillTarget = useWorkflowNodeDetailsPanelStore((state) => state.clearPillTarget);

    const releasePillTarget = useCallback(() => {
        if (focusedEditorRef.current) {
            clearPillTarget(focusedEditorRef.current);

            focusedEditorRef.current = null;
        }
    }, [clearPillTarget]);

    const handleEditorFocus = (editor: Editor) => {
        focusedEditorRef.current = editor;

        onEditorFocus(editor);
    };

    const handleEscapeKeyDown = (event: KeyboardEvent) => {
        if (editorRef.current?.storage.MentionStorage?.suggestionOpen) {
            event.preventDefault();
        }
    };

    const handleOpenChange = (nextOpen: boolean) => {
        if (!nextOpen) {
            releasePillTarget();
        }

        setOpen(nextOpen);
    };

    useEffect(() => {
        const anchorElement = anchorRef.current;

        if (!open || !anchorElement) {
            return;
        }

        setAnchorHeight(anchorElement.getBoundingClientRect().height);

        const resizeObserver = new ResizeObserver(() => {
            setAnchorHeight(anchorElement.getBoundingClientRect().height);
        });

        resizeObserver.observe(anchorElement);

        return () => resizeObserver.disconnect();
    }, [open]);

    useEffect(() => releasePillTarget, [releasePillTarget]);

    return (
        <Popover onOpenChange={handleOpenChange} open={open}>
            <PopoverTrigger asChild>
                <Button
                    aria-label="Open formula editor"
                    className="-my-0.5"
                    icon={<MaximizeIcon />}
                    size="iconXs"
                    title="Open formula editor"
                    variant="secondary"
                />
            </PopoverTrigger>

            <PopoverAnchor className="pointer-events-none absolute inset-0" ref={anchorRef} />

            <PopoverContent
                align="start"
                alignOffset={-12}
                className="flex w-[calc(var(--radix-popover-trigger-width)+1.5rem)] min-w-80 flex-col gap-2 p-3"
                onEscapeKeyDown={handleEscapeKeyDown}
                onFocusOutside={keepOpenForDataPillPanel}
                onOpenAutoFocus={(event) => event.preventDefault()}
                onPointerDownOutside={keepOpenForDataPillPanel}
                sideOffset={-(anchorHeight + PROPERTY_OVERLAP_MARGIN)}
            >
                <header className="flex items-center gap-2 text-sm font-medium text-content-neutral-primary">
                    <SquareFunctionIcon className="size-4 text-violet-700 dark:text-violet-300" />

                    <span className="flex-1">{title}</span>

                    <PopoverClose asChild>
                        <Button
                            aria-label="Close formula editor"
                            icon={<XIcon />}
                            size="iconXs"
                            title="Close formula editor"
                            variant="ghost"
                        />
                    </PopoverClose>
                </header>

                <div className="property-mentions-editor property-mentions-editor--formula-mode h-60 overflow-auto rounded-md border border-violet-500 bg-surface-neutral-primary dark:border-violet-400 dark:bg-surface-neutral-secondary">
                    <PropertyMentionsInputEditor
                        autoFocus
                        cancelPendingSaveRef={cancelPendingSaveRef}
                        className="min-h-full p-2"
                        componentDefinitions={componentDefinitions}
                        controlType={controlType}
                        dataPills={dataPills}
                        disableAutoSave={disableAutoSave}
                        expressionEnabled={expressionEnabled}
                        isFormulaMode
                        onFocus={handleEditorFocus}
                        onValueChange={onValueChange}
                        path={path}
                        placeholder={placeholder}
                        ref={editorRef}
                        setIsFormulaMode={setIsFormulaMode}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                        toolProperty={toolProperty}
                        type={type}
                        validateBeforeSave={validateBeforeSave}
                        value={value}
                        workflow={workflow}
                    />
                </div>

                <ExpressionHelpNote />
            </PopoverContent>
        </Popover>
    );
};

export default PropertyFormulaEditorPopover;
