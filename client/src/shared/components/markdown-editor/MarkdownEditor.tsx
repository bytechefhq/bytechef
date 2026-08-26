import MarkdownEditorToolbar from '@/shared/components/markdown-editor/MarkdownEditorToolbar';
import {Placeholder} from '@tiptap/extension-placeholder';
import {type Editor, EditorContent, useEditor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {type ChangeEvent, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {Markdown} from 'tiptap-markdown';

import './MarkdownEditor.css';

interface MarkdownStorageI {
    markdown: {
        getMarkdown: () => string;
    };
}

interface MarkdownEditorProps {
    ariaLabel?: string;
    className?: string;
    onBlur?: (markdown: string) => void;
    onChange?: (markdown: string) => void;
    placeholder?: string;
    value: string;
}

const getMarkdown = (editor: Editor): string => (editor.storage as unknown as MarkdownStorageI).markdown.getMarkdown();

/**
 * A WYSIWYG editor over Markdown: the value in and out is Markdown text, but the user sees rendered
 * headings, lists, emphasis and code rather than the raw markup. Markdown shortcuts ({@code # },
 * {@code - }, {@code **bold**}) transform as they are typed, and the toolbar's last control swaps the
 * rendered surface for the Markdown source.
 *
 * <p>
 * {@code className} lands on the editable content element, not the bordered container, so a caller
 * sizing the field with {@code min-h-*} sizes the click target too.
 * </p>
 */
const MarkdownEditor = ({ariaLabel, className, onBlur, onChange, placeholder, value}: MarkdownEditorProps) => {
    const [rawValue, setRawValue] = useState<string | null>(null);

    const propValueRef = useRef(value);

    const editor = useEditor({
        content: value,
        editorProps: {
            attributes: {
                ...(ariaLabel ? {'aria-label': ariaLabel} : {}),
                class: twMerge('prose prose-sm max-w-none focus:outline-none dark:prose-invert', className),
            },
        },
        extensions: [StarterKit, Markdown, Placeholder.configure({placeholder})],
        onBlur: ({editor: blurredEditor}) => onBlur?.(getMarkdown(blurredEditor)),
        onUpdate: ({editor: updatedEditor}) => onChange?.(getMarkdown(updatedEditor)),
    });

    const handleRawChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        const markdown = event.target.value;

        setRawValue(markdown);

        onChange?.(markdown);
    };

    const handleToggleRaw = (raw: boolean) => {
        if (!editor) {
            return;
        }

        if (raw) {
            setRawValue(getMarkdown(editor));

            return;
        }

        const markdown = rawValue ?? '';

        editor.commands.setContent(markdown, {emitUpdate: false});

        setRawValue(null);
    };

    useEffect(() => {
        const isExternalChange = value !== propValueRef.current;

        propValueRef.current = value;

        if (!isExternalChange || !editor || rawValue !== null || editor.isFocused || value === getMarkdown(editor)) {
            return;
        }

        editor.commands.setContent(value, {emitUpdate: false});
    }, [editor, rawValue, value]);

    return (
        <div className="markdown-editor w-full rounded-md border border-input bg-transparent text-base shadow-xs transition-[color,box-shadow] focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50 md:text-sm dark:bg-input/30">
            <MarkdownEditorToolbar editor={editor} isRaw={rawValue !== null} onToggleRaw={handleToggleRaw} />

            {rawValue === null ? (
                <div className="px-3 py-2">
                    <EditorContent editor={editor} />
                </div>
            ) : (
                <textarea
                    aria-label={ariaLabel}
                    className={twMerge(
                        'w-full resize-y bg-transparent px-3 py-2 font-mono text-sm outline-none',
                        className
                    )}
                    onBlur={(event) => onBlur?.(event.target.value)}
                    onChange={handleRawChange}
                    placeholder={placeholder}
                    value={rawValue}
                />
            )}
        </div>
    );
};

export default MarkdownEditor;
