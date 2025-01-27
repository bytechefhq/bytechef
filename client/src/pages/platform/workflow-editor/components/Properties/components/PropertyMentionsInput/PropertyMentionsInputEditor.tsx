import PropertyMentionsInputBubbleMenu from '@/pages/platform/workflow-editor/components/Properties/components/PropertyMentionsInput/PropertyMentionsInputBubbleMenu';
import {getSuggestionOptions} from '@/pages/platform/workflow-editor/components/Properties/components/PropertyMentionsInput/propertyMentionsInputEditorSuggestionOptions';
import {ComponentDefinitionBasic, Workflow} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import Document from '@tiptap/extension-document';
import {Mention} from '@tiptap/extension-mention';
import Paragraph from '@tiptap/extension-paragraph';
import Placeholder from '@tiptap/extension-placeholder';
import Text from '@tiptap/extension-text';
import {Editor, EditorContent, Extension, mergeAttributes, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {EditorView} from 'prosemirror-view';
import {ForwardedRef, MutableRefObject, forwardRef, useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const defaultIcon =
    '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8Z"/><path d="M15 3v4a2 2 0 0 0 2 2h4"/></svg>';

interface PropertyMentionsInputEditorProps {
    className?: string;
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    elementId?: string;
    path?: string;
    onChange: (value: string) => void;
    onClose?: () => void;
    onFocus?: (editor: Editor) => void;
    placeholder?: string;
    type: string;
    value?: string;
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
            onChange,
            onFocus,
            path,
            placeholder,
            type,
            value,
            workflow,
        },
        ref: ForwardedRef<Editor>
    ) => {
        const [mentionOccurences, setMentionOccurences] = useState(0);

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

        const extensions = useMemo(() => {
            const extensions = [
                ...(controlType === 'RICH_TEXT' ? [StarterKit] : [Document, Paragraph, Text]),
                Mention.configure({
                    HTMLAttributes: {
                        class: 'property-mention',
                    },
                    renderHTML({node, options}) {
                        let svg = getComponentIcon(node.attrs.label ?? node.attrs.id);

                        svg =
                            'data:image/svg+xml;utf8,' +
                            svg
                                .replaceAll(/#/g, '%23')
                                .replaceAll(/"/g, "'")
                                .replaceAll(/&/g, '&amp;')
                                .replaceAll(/\n/g, '');

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

        const onUpdate = useCallback(
            ({editor}: {editor: Editor}) => {
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

                onChange(value);

                const propertyMentions = value.match(/property-mention/g);

                setMentionOccurences(propertyMentions?.length || 0);
            },
            [onChange]
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
                        'text-sm outline-none max-w-full',
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
            if (value && editor) {
                editor.commands.setContent(getContent(value)!);
            }
        }, [editor, getContent, value]);

        return (
            <>
                <EditorContent editor={editor} id={elementId} />

                {controlType === 'RICH_TEXT' && editor && <PropertyMentionsInputBubbleMenu editor={editor} />}
            </>
        );
    }
);

PropertyMentionsInputEditor.displayName = 'PropertyMentionsInputEditor';

export default PropertyMentionsInputEditor;
