import PropertyMentionsInputBubbleMenu from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputBubbleMenu';
import {getSuggestionOptions} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/propertyMentionsInputEditorSuggestionOptions';
import {useWorkflowNodeParameterMutation} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {encodeParameters, encodePath} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {ComponentDefinitionBasic, Workflow} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import Document from '@tiptap/extension-document';
import {Mention} from '@tiptap/extension-mention';
import Paragraph from '@tiptap/extension-paragraph';
import Placeholder from '@tiptap/extension-placeholder';
import Text from '@tiptap/extension-text';
import {Editor, EditorContent, Extension, mergeAttributes, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {decode} from 'html-entities';
import resolvePath from 'object-resolve-path';
import {EditorView} from 'prosemirror-view';
import {ForwardedRef, MutableRefObject, forwardRef, useCallback, useEffect, useMemo, useState} from 'react';
import sanitizeHtml from 'sanitize-html';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';

const defaultIcon =
    '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8Z"/><path d="M15 3v4a2 2 0 0 0 2 2h4"/></svg>';

interface PropertyMentionsInputEditorProps {
    className?: string;
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    elementId?: string;
    path?: string;
    onClose?: () => void;
    onFocus?: (editor: Editor) => void;
    placeholder?: string;
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
            onFocus,
            path,
            placeholder,
            type,
            value,
            workflow,
        },
        ref: ForwardedRef<Editor>
    ) => {
        const [editorValue, setEditorValue] = useState<string | number | undefined>(value);
        const [isLocalUpdate, setIsLocalUpdate] = useState(false);
        const [mentionOccurences, setMentionOccurences] = useState(0);

        const {currentNode} = useWorkflowNodeDetailsPanelStore();

        const getComponentIcon = useCallback(
            (mentionValue: string) => {
                let componentName = mentionValue?.split('_')[0].replace('${', '');

                if (componentName === 'trigger') {
                    componentName = workflow.workflowTriggerComponentNames?.[0] || '';
                }

                return componentDefinitions.find((component) => component.name === componentName)?.icon || defaultIcon;
            },
            [componentDefinitions, workflow.workflowTriggerComponentNames]
        );

        const memoizedWorkflowTask = useMemo(() => {
            return [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])].find(
                (node) => node.name === currentNode?.name
            );
        }, [workflow.triggers, workflow.tasks, currentNode?.name]);

        const extensions = useMemo(() => {
            const extensions = [
                ...(controlType === 'RICH_TEXT' ? [StarterKit] : [Document, Paragraph, Text]),
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
                                    class: 'h-4 absolute',
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
                    suggestion: getSuggestionOptions(dataPills),
                }),
                Placeholder.configure({
                    placeholder: placeholder
                        ? `${placeholder} (Show data pills using '{')`
                        : "Show data pills using '{'",
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
        }, [controlType, dataPills, getComponentIcon, placeholder]);

        const {updateWorkflowNodeParameterMutation} = useWorkflowNodeParameterMutation();

        const saveMentionInputValue = useDebouncedCallback(() => {
            if (!workflow.id || !updateWorkflowNodeParameterMutation || !path) {
                return;
            }

            let value = editorValue;

            if ((type === 'INTEGER' || type === 'NUMBER') && typeof value === 'string' && !value.startsWith('${')) {
                value = parseInt(value);
            }

            if (typeof value === 'string') {
                if (controlType !== 'RICH_TEXT') {
                    value = decode(sanitizeHtml(value, {allowedTags: []}));
                } else {
                    value = decode(value);
                }
            }

            saveProperty({
                includeInMetadata: true,
                path,
                type,
                updateWorkflowNodeParameterMutation,
                value: value || null,
                workflowId: workflow.id,
            });
        }, 1000);

        const onUpdate = useCallback(
            ({editor}: {editor: Editor}) => {
                setIsLocalUpdate(true);

                let value = editor.getHTML();

                value = value.replace(/\r\n/g, '\n');
                value = value.replace(/\s+/g, ' ').trim();

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

                setEditorValue(value);

                const propertyMentions = value.match(/property-mention/g);

                setMentionOccurences(propertyMentions?.length || 0);

                saveMentionInputValue();
            },
            [saveMentionInputValue]
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

        const editor = useEditor({
            editorProps: {
                attributes: {
                    class: twMerge(
                        'text-sm outline-none max-w-full border-none ring-0',
                        controlType === 'RICH_TEXT' && 'prose prose-sm sm:prose-base lg:prose-lg xl:prose-2xl',
                        className
                    ),
                    id: elementId ?? '',
                    path: path ?? '',
                    type: type ?? '',
                },
                handleKeyPress: (_: EditorView, event: KeyboardEvent) => {
                    if (type !== 'STRING' && (mentionOccurences || event.key !== '{')) {
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

        if (ref) {
            (ref as MutableRefObject<Editor | null>).current = editor;
        }

        useEffect(() => {
            if (editor && !isLocalUpdate) {
                editor.commands.setContent(getContent(editorValue as string)!, false, {
                    preserveWhitespace: 'full',
                });
            }
        }, [editor, getContent, editorValue, isLocalUpdate]);

        // set propertyParameterValue on workflow definition change
        useEffect(() => {
            if (!workflow.definition || !currentNode?.name || !path) {
                return;
            }

            const encodedParameters = encodeParameters(memoizedWorkflowTask?.parameters ?? {});
            const encodedPath = encodePath(path);

            const propertyValue = resolvePath(encodedParameters, encodedPath);

            setEditorValue(propertyValue);
        }, [currentNode?.name, memoizedWorkflowTask?.parameters, path, workflow.definition]);

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
