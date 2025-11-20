import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import PropertyMentionsInputBubbleMenu from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputBubbleMenu';
import {getSuggestionOptions} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/propertyMentionsInputEditorSuggestionOptions';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    encodeParameters,
    encodePath,
    transformValueForObjectAccess,
} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {getTaskDispatcherTask} from '@/pages/platform/workflow-editor/utils/taskDispatcherConfig';
import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {ClusterElementItemType, DataPillType} from '@/shared/types';
import Document from '@tiptap/extension-document';
import {Mention} from '@tiptap/extension-mention';
import Paragraph from '@tiptap/extension-paragraph';
import Placeholder from '@tiptap/extension-placeholder';
import Text from '@tiptap/extension-text';
import {TextSelection} from '@tiptap/pm/state';
import {EditorView} from '@tiptap/pm/view';
import {Editor, EditorContent, Extension, mergeAttributes, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {decode} from 'html-entities';
import resolvePath from 'object-resolve-path';
import {ForwardedRef, MutableRefObject, forwardRef, useCallback, useEffect, useMemo, useState} from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import sanitizeHtml from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/shallow';

import {FormulaMode} from './FormulaMode.extension';
import {MentionStorage} from './MentionStorage.extension';

interface PropertyMentionsInputEditorProps {
    className?: string;
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    elementId?: string;
    isFormulaMode?: boolean;
    path?: string;
    onChange?: (value: string) => void;
    onFocus?: (editor: Editor) => void;
    placeholder?: string;
    setIsFormulaMode?: (isFormulaMode: boolean) => void;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
    type: string;
    value?: string | number;
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
            isFormulaMode,
            onChange,
            onFocus,
            path,
            placeholder,
            setIsFormulaMode,
            taskDispatcherDefinitions,
            type,
            value,
            workflow,
        },
        ref: ForwardedRef<Editor>
    ) => {
        const [editorValue, setEditorValue] = useState<string | number | undefined>(value);
        const [isLocalUpdate, setIsLocalUpdate] = useState(false);
        const [mentionOccurences, setMentionOccurences] = useState(0);

        const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);

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

        const {updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation} = useWorkflowEditor();
        const {rootClusterElementNodeData} = useWorkflowEditorStore(
            useShallow((state) => ({
                rootClusterElementNodeData: state.rootClusterElementNodeData,
            }))
        );

        const memoizedWorkflowTask = useMemo(
            () =>
                [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])].find(
                    (node) => node.name === currentNode?.name
                ),
            [workflow.triggers, workflow.tasks, currentNode?.name]
        );

        const memoizedClusterElementTask = useMemo((): ClusterElementItemType | undefined => {
            if (!currentNode?.name || !workflow.definition) {
                return undefined;
            }

            if (currentNode.clusterElementType) {
                const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

                const mainClusterRootTask = rootClusterElementNodeData?.workflowNodeName
                    ? getTaskDispatcherTask({
                          taskDispatcherId: rootClusterElementNodeData.workflowNodeName,
                          tasks: workflowDefinitionTasks,
                      })
                    : undefined;

                if (mainClusterRootTask?.clusterElements) {
                    return getClusterElementByName(mainClusterRootTask.clusterElements, currentNode.name);
                }
            }
        }, [currentNode, workflow.definition, rootClusterElementNodeData?.workflowNodeName]);

        const extensions = useMemo(() => {
            const extensions = [
                ...(controlType === 'RICH_TEXT' ? [StarterKit] : [Document, Paragraph, Text]),
                FormulaMode.configure({
                    saveNullValue: () => {
                        if (
                            !workflow.id ||
                            !(updateWorkflowNodeParameterMutation || updateClusterElementParameterMutation) ||
                            !path
                        ) {
                            return;
                        }

                        saveProperty({
                            includeInMetadata: true,
                            path,
                            type,
                            updateClusterElementParameterMutation,
                            updateWorkflowNodeParameterMutation,
                            value: null,
                            workflowId: workflow.id,
                        });
                    },
                    setIsFormulaMode: setIsFormulaMode || (() => {}),
                }),
                MentionStorage,
                Mention.configure({
                    HTMLAttributes: {
                        class: 'property-mention',
                    },
                    renderHTML({node, options}) {
                        const svg = getComponentIcon(node.attrs.label ?? node.attrs.id);

                        return [
                            'span',
                            mergeAttributes(options.HTMLAttributes, {
                                class: twMerge(
                                    'relative inline-flex items-center gap-0.5 not-prose bg-muted hover:bg-foreground/15 px-2 rounded-full',
                                    controlType !== 'RICH_TEXT' && controlType !== 'TEXT_AREA' && 'text-sm'
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

            if (controlType !== 'TEXT_AREA' && controlType !== 'RICH_TEXT') {
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

            let value = editorValue;

            if (
                !isFormulaMode &&
                (type === 'INTEGER' || type === 'NUMBER') &&
                typeof value === 'string' &&
                !value.startsWith('${')
            ) {
                value = parseInt(value);
            }

            if (typeof value === 'string') {
                if (controlType !== 'RICH_TEXT') {
                    value = decode(sanitizeHtml(value, {allowedTags: []}));
                }

                value = transformValueForObjectAccess(value);

                if (isFormulaMode && !value.startsWith('=')) {
                    value = `=${value}`;
                }
            }

            saveProperty({
                includeInMetadata: true,
                path,
                type,
                updateClusterElementParameterMutation,
                updateWorkflowNodeParameterMutation,
                value: value || null,
                workflowId: workflow.id,
            });
        }, 300);

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

                if (value !== editorValue) {
                    setEditorValue(value);
                }

                if (onChange) {
                    onChange(value);
                }

                const propertyMentions = value.match(/property-mention/g);

                setMentionOccurences(propertyMentions?.length || 0);

                saveMentionInputValue(value);
            },
            [editorValue, onChange, saveMentionInputValue]
        );

        const getContent = useCallback((value?: string) => {
            if (typeof value !== 'string') {
                return;
            }

            if (!value) {
                return '';
            }

            let content = value;

            if (value.includes('\n')) {
                const valueLines = value.split('\n');

                const paragraphedLines = valueLines.map((valueLine) => `<p>${valueLine}</p>`);

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
        }, []);

        const moveCursorToEnd = useCallback((view: EditorView, pos: number) => {
            const valueSize = view.state.doc.content.size;

            if (valueSize > 0 && pos === 0) {
                view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, valueSize)));
            }
        }, []);

        const editor = useEditor({
            coreExtensionOptions: {
                clipboardTextSerializer: {
                    blockSeparator: '\n',
                },
            },
            editorProps: {
                attributes: {
                    class: twMerge(
                        'text-sm outline-none max-w-full border-none ring-0 break-words whitespace-pre-wrap w-full min-w-0 break-all',
                        controlType === 'RICH_TEXT' && 'prose prose-sm sm:prose-base lg:prose-lg xl:prose-2xl',
                        className
                    ),
                    id: elementId ?? '',
                    path: path ?? '',
                    type: type ?? '',
                },
                handleClick: (view, pos) => moveCursorToEnd(view, pos),
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

        if (ref) {
            (ref as MutableRefObject<Editor | null>).current = editor;
        }

        useEffect(() => {
            if (editor) {
                editor.storage.MentionStorage.dataPills = dataPills;
            }
        }, [dataPills, editor]);

        // Update editor content when editorValue changes (but not during local updates)
        useEffect(() => {
            if (editor && !isLocalUpdate) {
                editor.commands.setContent(getContent(editorValue as string)!, false, {
                    preserveWhitespace: 'full',
                });
            }
        }, [editor, getContent, editorValue, isLocalUpdate]);

        // Set propertyParameterValue on workflow definition change
        useEffect(() => {
            if (!workflow.definition || !currentNode?.name || !path) {
                return;
            }

            const encodedParameters = encodeParameters(
                (memoizedWorkflowTask?.parameters || memoizedClusterElementTask?.parameters) ?? {}
            );
            const encodedPath = encodePath(path);

            const propertyValue = resolvePath(encodedParameters, encodedPath);

            if (typeof propertyValue === 'string' && propertyValue.startsWith('=')) {
                setEditorValue(propertyValue.substring(1));
            } else {
                setEditorValue(propertyValue);
            }
        }, [
            currentNode?.name,
            memoizedClusterElementTask?.parameters,
            memoizedWorkflowTask?.parameters,
            path,
            workflow.definition,
        ]);

        // Update editor content when editorValue changes (but not during local updates)
        useEffect(() => {
            if (editor && !isLocalUpdate && memoizedContent !== undefined) {
                editor.commands.setContent(memoizedContent, false, {
                    preserveWhitespace: 'full',
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

                if (editor.storage.formulaMode) {
                    editor.storage.formulaMode.isFormulaMode = isFormulaMode;
                }
            }
        }, [editor, value, isFormulaMode, setIsFormulaMode]);

        // Cleanup function to save mention input value on unmount
        useEffect(() => {
            return () => saveMentionInputValue.flush();
        }, [saveMentionInputValue]);

        return (
            <>
                <EditorContent
                    editor={editor}
                    id={elementId}
                    onChange={(event) => setEditorValue((event.target as HTMLInputElement).value)}
                    value={editorValue}
                />

                {controlType === 'RICH_TEXT' && editor && <PropertyMentionsInputBubbleMenu editor={editor} />}
            </>
        );
    }
);

PropertyMentionsInputEditor.displayName = 'PropertyMentionsInputEditor';

export default PropertyMentionsInputEditor;
