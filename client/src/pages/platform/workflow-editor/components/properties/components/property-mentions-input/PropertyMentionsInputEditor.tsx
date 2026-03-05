import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {canInsertMentionForProperty} from '@/pages/platform/workflow-editor/components/datapills/DataPill';
import FromAiToggleButton from '@/pages/platform/workflow-editor/components/properties/components/FromAiToggleButton';
import PropertyMentionsInputBubbleMenu from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputBubbleMenu';
import {getSuggestionOptions} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/propertyMentionsInputEditorSuggestionOptions';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    escapeHtmlForParagraph,
    transformValueForObjectAccess,
} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {ClusterElementItemType, DataPillDragPayloadType, DataPillType} from '@/shared/types';
import {Extension, mergeAttributes} from '@tiptap/core';
import Document from '@tiptap/extension-document';
import {Mention} from '@tiptap/extension-mention';
import {Paragraph} from '@tiptap/extension-paragraph';
import {Placeholder} from '@tiptap/extension-placeholder';
import {Text} from '@tiptap/extension-text';
import {TextSelection} from '@tiptap/pm/state';
import {EditorView} from '@tiptap/pm/view';
import {Editor, EditorContent, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {decode} from 'html-entities';
import {ForwardedRef, MutableRefObject, forwardRef, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import sanitizeHtml from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/react/shallow';

import {FormulaMode} from './FormulaMode.extension';
import {FromAi} from './FromAi.extension';
import {MentionStorage} from './MentionStorage.extension';

interface PropertyMentionsInputEditorProps {
    className?: string;
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    elementId?: string;
    handleFromAiClick?: (fromAi: boolean) => void;
    isFormulaMode?: boolean;
    isFromAi?: boolean;
    labelId?: string;
    path?: string;
    onChange?: (value: string) => void;
    onFocus?: (editor: Editor) => void;
    onValueChange?: (value: string | number) => void;
    placeholder?: string;
    setIsFormulaMode?: (isFormulaMode: boolean) => void;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
    type: string;
    value?: string | number;
    validateBeforeSave?: (value: string | number) => boolean;
    workflow: Workflow;
}

const PropertyMentionsInputEditor = forwardRef<Editor, PropertyMentionsInputEditorProps>(
    (
        {
            className,
            componentDefinitions,
            controlType,
            dataPills,
            elementId,
            handleFromAiClick,
            isFormulaMode,
            isFromAi = false,
            labelId,
            onChange,
            onFocus,
            onValueChange,
            path,
            placeholder,
            setIsFormulaMode,
            taskDispatcherDefinitions,
            type,
            validateBeforeSave,
            value,
            workflow,
        },
        ref: ForwardedRef<Editor>
    ) => {
        const [editorValue, setEditorValue] = useState<string | number | undefined>(
            typeof value === 'string' && value.startsWith('=') ? value.substring(1) : value
        );
        const [isLocalUpdate, setIsLocalUpdate] = useState(false);
        const [mentionOccurences, setMentionOccurences] = useState(0);

        const lastSavedRef = useRef<string | number | null | undefined>(undefined);
        const savingRef = useRef<Promise<void> | null>(null);
        const pendingValueRef = useRef<string | number | null | undefined>(undefined);
        const editorValueRef = useRef(editorValue);

        editorValueRef.current = editorValue;

        const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                currentComponent: state.currentComponent,
                currentNode: state.currentNode,
            }))
        );

        const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

        const {updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation} = useWorkflowEditor();

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

        const getComponentIcon = useCallback(
            (mentionValue: string) => {
                let componentName = mentionValue?.split('_')[0].replace('${', '');

                if (componentName === 'trigger') {
                    componentName = workflow.workflowTriggerComponentNames?.[0] || '';
                }

                if (TASK_DISPATCHER_NAMES.includes(componentName)) {
                    return taskDispatcherDefinitions.find((component) => component.name === componentName)?.icon;
                }

                const componentIcon = componentDefinitions.find((component) => component.name === componentName)?.icon;

                if (componentIcon) {
                    return componentIcon;
                }

                const svgString = renderToStaticMarkup(TYPE_ICONS.STRING);

                return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgString)}`;
            },
            [componentDefinitions, taskDispatcherDefinitions, workflow.workflowTriggerComponentNames]
        );

        const extensions = useMemo(() => {
            const extensions = [
                ...(controlType === 'RICH_TEXT' ? [StarterKit] : [Document, Paragraph, Text]),
                ...(memoizedClusterElementTask
                    ? [
                          FromAi.configure({
                              setFromAi: () => {},
                          }),
                      ]
                    : []),
                FormulaMode.configure({
                    saveNullValue: () => {
                        if (
                            !workflow.id ||
                            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
                            !path
                        ) {
                            return;
                        }

                        const workflowId = workflow.id as string;

                        saveProperty({
                            includeInMetadata: true,
                            path,
                            type,
                            updateClusterElementParameterMutation,
                            updateWorkflowNodeParameterMutation,
                            value: null,
                            workflowId,
                        });
                    },
                    setIsFormulaMode: setIsFormulaMode || (() => {}),
                }),
                MentionStorage,
                Mention.configure({
                    HTMLAttributes: {
                        class: 'property-mention',
                    },
                    deleteTriggerWithBackspace: true,
                    renderHTML({node, options}) {
                        const svg = getComponentIcon(node.attrs.label ?? node.attrs.id);

                        return [
                            'span',
                            mergeAttributes(options.HTMLAttributes, {
                                class: twMerge(
                                    'relative inline-flex items-center gap-0.5 not-prose bg-muted hover:bg-foreground/15 px-2 rounded-full',
                                    controlType !== 'RICH_TEXT' &&
                                        controlType !== 'TEXT_AREA' &&
                                        controlType !== 'FORMULA_MODE' &&
                                        'text-sm'
                                ),
                            }),
                            [
                                'img',
                                {
                                    class: 'size-4 absolute',
                                    src: svg,
                                },
                            ],
                            [
                                'span',
                                {
                                    class: 'ml-5',
                                },
                                `${node.attrs.label ?? node.attrs.id}`,
                            ],
                        ];
                    },
                    renderText({node}) {
                        return `\${${node.attrs.label ?? node.attrs.id}}`;
                    },
                    suggestion: getSuggestionOptions(),
                }),
                Placeholder.configure({
                    placeholder: placeholder ? placeholder : "Use '$' for data pills and '=' for an expression",
                }),
            ];

            if (controlType !== 'TEXT_AREA' && controlType !== 'RICH_TEXT' && controlType !== 'FORMULA_MODE') {
                extensions.push(
                    Extension.create({
                        addKeyboardShortcuts(this) {
                            return {
                                Enter: () => true,
                            };
                        },
                    })
                );
            }

            return extensions;
        }, [
            controlType,
            getComponentIcon,
            memoizedClusterElementTask,
            path,
            placeholder,
            setIsFormulaMode,
            type,
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            workflow.id,
        ]);

        const saveMentionInputValue = useDebouncedCallback((editorValue: string | number) => {
            if (
                !workflow.id ||
                !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
                !path
            ) {
                return;
            }

            const valueForValidation =
                isFormulaMode && typeof editorValue === 'string' && !editorValue.startsWith('=')
                    ? `=${editorValue}`
                    : editorValue;

            if (validateBeforeSave && editorValue !== '' && !validateBeforeSave(valueForValidation)) {
                return;
            }

            const workflowId = workflow.id as string;

            let transformedValue: string | number | null = editorValue;

            if (
                !isFormulaMode &&
                (type === 'INTEGER' || type === 'NUMBER') &&
                typeof transformedValue === 'string' &&
                !transformedValue.includes('${')
            ) {
                transformedValue = parseInt(transformedValue);
            }

            if (typeof transformedValue === 'string') {
                if (controlType !== 'RICH_TEXT') {
                    transformedValue = decode(sanitizeHtml(transformedValue, {allowedTags: []}));
                }

                transformedValue = transformValueForObjectAccess(transformedValue);

                if (isFormulaMode && !transformedValue.startsWith('=')) {
                    transformedValue = `=${transformedValue}`;
                }
            }

            if (isFromAi) {
                return;
            }

            const normalizedValue: string | number | null = transformedValue ? transformedValue : null;

            if (normalizedValue === lastSavedRef.current || normalizedValue === pendingValueRef.current) {
                return;
            }

            pendingValueRef.current = normalizedValue;

            if (!savingRef.current) {
                const runSaveProperty = () => {
                    const valueToSave = pendingValueRef.current;

                    pendingValueRef.current = undefined;

                    if (valueToSave === undefined) {
                        savingRef.current = null;

                        return;
                    }

                    savingRef.current = Promise.resolve(
                        saveProperty({
                            includeInMetadata: true,
                            path,
                            type,
                            updateClusterElementParameterMutation,
                            updateWorkflowNodeParameterMutation,
                            value: valueToSave,
                            workflowId,
                        })
                    )
                        .then(() => {
                            lastSavedRef.current = valueToSave;
                        })
                        .catch(() => {})
                        .finally(() => {
                            savingRef.current = null;

                            if (pendingValueRef.current !== undefined) {
                                runSaveProperty();
                            }
                        });
                };

                runSaveProperty();
            }
        }, 600);

        const onUpdate = useCallback(
            ({editor}: {editor: Editor}) => {
                setIsLocalUpdate(true);

                let value = editor.getHTML();

                value = value.replace(/\r\n/g, '\n');

                const paragraphMatchRegex = /<p>(.*?)<\/p>/g;

                const matchedParagraphs = value.match(paragraphMatchRegex);

                if (matchedParagraphs) {
                    value = matchedParagraphs.map((match) => match.replace(/<\/?p>/g, '')).join('\n');
                }

                const mentionSpanRegex = /<span data-type="mention"[^>]*data-id="([^"]+)"[^>]*>.*?<\/span>/g;

                const foundMentions = value.match(mentionSpanRegex);

                if (foundMentions) {
                    const dataIdRegex = /data-id="([^"]+)"/;

                    foundMentions.forEach((match) => {
                        value = value.replace(match, `\${${match.match(dataIdRegex)?.[1]}}`);
                    });
                }

                const valueChanged = value !== editorValue;

                if (valueChanged) {
                    setEditorValue(value);
                    saveMentionInputValue(value);
                }

                if (onChange) {
                    onChange(value);
                }

                if (onValueChange) {
                    onValueChange(value);
                }

                const propertyMentions = value.match(/property-mention/g);

                setMentionOccurences(propertyMentions?.length || 0);
            },
            [editorValue, onChange, onValueChange, saveMentionInputValue]
        );

        const getContent = useCallback(
            (value?: string) => {
                if (typeof value !== 'string') {
                    return;
                }

                if (!value) {
                    return '';
                }

                let content = value;
                let contentIsDecodedHtml = false;

                if (
                    controlType === 'RICH_TEXT' &&
                    (content.includes('&lt;') || content.includes('&gt;') || content.includes('&amp;'))
                ) {
                    content = decode(content);

                    content = sanitizeHtml(content);

                    contentIsDecodedHtml = true;
                }

                if (!contentIsDecodedHtml && content.includes('\n')) {
                    const valueLines = content.split('\n');

                    const paragraphedLines =
                        controlType === 'TEXT_AREA' || controlType === 'TEXT' || controlType === 'FORMULA_MODE'
                            ? valueLines.map((valueLine) => `<p>${escapeHtmlForParagraph(valueLine)}</p>`)
                            : valueLines.map((valueLine) => `<p>${valueLine}</p>`);

                    content = paragraphedLines.join('');
                }

                const dataPillRegex = /\${([^}]+)}/g;

                const matches = value.match(dataPillRegex)?.map((match) => match.slice(2, -1));

                if (matches) {
                    for (const match of matches) {
                        content = content.replace(
                            `\${${match}}`,
                            `<span data-type="mention" class="property-mention" data-id="${match}"></span>`
                        );
                    }
                }

                return content;
            },
            [controlType]
        );

        const moveCursorToEnd = useCallback((view: EditorView, pos: number) => {
            const valueSize = view.state.doc.content.size;

            if (valueSize > 0 && pos === 0) {
                view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, valueSize)));
            }
        }, []);

        const editorRef = useRef<Editor | null>(null);

        const handleDrop = useCallback(
            (view: EditorView, event: DragEvent, _slice: unknown, moved: boolean): boolean => {
                if (moved) {
                    return false;
                }

                if (isFromAi) {
                    return false;
                }

                const rawPayload = event.dataTransfer?.getData('application/bytechef-datapill');

                if (!rawPayload) {
                    return false;
                }

                event.preventDefault();

                let payload: DataPillDragPayloadType;

                try {
                    payload = JSON.parse(rawPayload);
                } catch {
                    return false;
                }

                if (!payload?.mentionId) {
                    return false;
                }

                const attributes = view.props.attributes as Record<string, string>;
                const parameters = currentComponent?.parameters || {};

                if (!canInsertMentionForProperty(attributes.type, parameters, attributes.path)) {
                    return true;
                }

                const coordinates = view.posAtCoords({
                    left: event.clientX,
                    top: event.clientY,
                });

                const insertPosition = coordinates?.pos ?? view.state.doc.content.size;

                editorRef.current
                    ?.chain()
                    .insertContentAt(insertPosition, {
                        attrs: {id: payload.mentionId},
                        type: 'mention',
                    })
                    .focus()
                    .run();

                return true;
            },
            [currentComponent?.parameters, isFromAi]
        );

        const editor = useEditor({
            coreExtensionOptions: {
                clipboardTextSerializer: {
                    blockSeparator: '\n',
                },
            },
            editorProps: {
                attributes: {
                    ...(labelId ? {'aria-labelledby': labelId} : {}),
                    'aria-multiline': 'true',
                    class: twMerge(
                        'text-sm outline-none max-w-full border-none ring-0 break-words whitespace-pre-wrap w-full min-w-0 break-all',
                        controlType === 'RICH_TEXT' && 'prose prose-sm sm:prose-base lg:prose-lg xl:prose-2xl',
                        className
                    ),
                    id: elementId ?? '',
                    path: path ?? '',
                    role: 'textbox',
                    type: type ?? '',
                },
                handleClick: (view, pos) => moveCursorToEnd(view, pos),
                handleDrop,
                handleKeyPress: (editor: EditorView, event: KeyboardEvent) => {
                    const isEditorEmpty = editor.state.doc.textContent.length === 0;

                    if ((event.key === '=' && isEditorEmpty) || isFormulaMode) {
                        return;
                    }

                    if (type !== 'STRING' && (mentionOccurences || event.key !== '$')) {
                        event.preventDefault();
                    }
                },
            },
            extensions,
            immediatelyRender: false,
            onFocus: () => {
                if (onFocus && editor) {
                    onFocus(editor);
                }
            },
            onUpdate,
        });

        const memoizedContent = useMemo(() => {
            if (editorValue === undefined || typeof editorValue !== 'string') {
                return '';
            }

            return getContent(editorValue);
        }, [editorValue, getContent]);

        const fromAiExtension = useMemo(
            () => editor?.extensionManager.extensions.find((extension) => extension.name === 'fromAi'),
            [editor]
        );

        // When the value prop changes externally, sync it into editor state unless it already matches the current editor value (to avoid overwriting in-flight local updates such as datapill insertions).
        useEffect(() => {
            if (value === undefined || value === editorValueRef.current) {
                return;
            }

            const strippedValue = typeof value === 'string' && value.startsWith('=') ? value.substring(1) : value;

            setIsLocalUpdate(false);
            setEditorValue(strippedValue);
        }, [value]);

        // Sync ref when editor changes - handle both callback and object refs
        useEffect(() => {
            editorRef.current = editor;
        }, [editor]);

        useEffect(() => {
            if (!ref) {
                return;
            }

            if (typeof ref === 'function') {
                ref(editor);
            } else if (ref && 'current' in ref) {
                (ref as MutableRefObject<Editor | null>).current = editor;
            }

            return () => {
                if (typeof ref === 'function') {
                    ref(null);
                } else if (ref && 'current' in ref) {
                    (ref as MutableRefObject<Editor | null>).current = null;
                }
            };
        }, [editor, ref]);

        // Update data pills in MentionStorage when they change
        useEffect(() => {
            if (editor) {
                editor.storage.MentionStorage.dataPills = dataPills;
            }
        }, [dataPills, editor]);

        // Update editor content when editorValue changes (but not during local updates)
        useEffect(() => {
            if (editor && !isLocalUpdate && memoizedContent !== undefined) {
                editor.commands.setContent(memoizedContent, {
                    emitUpdate: false,
                    parseOptions: {preserveWhitespace: 'full'},
                });
            }
        }, [editor, memoizedContent, isLocalUpdate]);

        // Set formula mode based on value and sync with editor storage
        useEffect(() => {
            if (!editor) {
                return;
            }

            if (typeof value === 'string' && value.startsWith('=') && setIsFormulaMode) {
                setIsFormulaMode(true);
            }

            if (isFormulaMode !== undefined) {
                editor.commands.toggleFormulaMode(isFormulaMode);

                editor.storage.FormulaMode.isFormulaMode = isFormulaMode;
            }
        }, [editor, value, isFormulaMode, setIsFormulaMode]);

        // Set fromAi based on metadata and sync with editor storage
        useEffect(() => {
            if (!editor || !path || isFromAi === undefined || !currentComponent?.metadata?.ui?.fromAi?.includes(path)) {
                return;
            }

            if (currentComponent?.metadata?.ui?.fromAi?.includes(path)) {
                editor.commands.setFromAi(isFromAi);
            }
        }, [currentComponent, currentComponent?.metadata?.ui?.fromAi, editor, isFromAi, path]);

        // Set editable based on isFromAi
        useEffect(() => {
            if (path && !currentComponent?.metadata?.ui?.fromAi?.includes(path)) {
                return;
            }

            if (editor && isFromAi !== undefined) {
                editor.setEditable(!isFromAi);
            }
        }, [currentComponent?.metadata?.ui?.fromAi, editor, isFromAi, path]);

        // Cleanup function to save mention input value on unmount
        useEffect(() => {
            return () => saveMentionInputValue.flush();
        }, [saveMentionInputValue]);

        return (
            <>
                <EditorContent
                    className={twMerge(isFromAi && 'pointer-events-none cursor-not-allowed')}
                    disabled={isFromAi}
                    editor={editor}
                    onChange={(event) => setEditorValue((event.target as HTMLInputElement).value)}
                    value={editorValue}
                />

                {fromAiExtension && handleFromAiClick && currentNode?.clusterElementType === 'tools' && (
                    <FromAiToggleButton isFromAi={isFromAi} onToggle={handleFromAiClick} />
                )}

                {controlType === 'RICH_TEXT' && editor && <PropertyMentionsInputBubbleMenu editor={editor} />}
            </>
        );
    }
);

PropertyMentionsInputEditor.displayName = 'PropertyMentionsInputEditor';

export default PropertyMentionsInputEditor;
