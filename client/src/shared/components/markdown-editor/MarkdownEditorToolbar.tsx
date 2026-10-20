import {Separator} from '@/components/ui/separator';
import {Toggle} from '@/components/ui/toggle';
import {type Editor, useEditorState} from '@tiptap/react';
import {
    BoldIcon,
    CodeIcon,
    CodeXmlIcon,
    Heading1Icon,
    Heading2Icon,
    Heading3Icon,
    ItalicIcon,
    ListIcon,
    ListOrderedIcon,
    type LucideIcon,
    QuoteIcon,
    SquareCodeIcon,
    StrikethroughIcon,
} from 'lucide-react';
import {useMemo} from 'react';

interface ToolbarButtonI {
    icon: LucideIcon;
    isActive: boolean;
    label: string;
    onToggle: () => void;
}

interface MarkdownEditorToolbarProps {
    editor: Editor | null;
    isRaw: boolean;
    onToggleRaw: (raw: boolean) => void;
}

/**
 * Formatting controls for {@link MarkdownEditor}, plus the toggle between the rendered surface and the
 * Markdown source. The formatting controls are disabled while the source is showing, since they act on the
 * rendered document rather than on the text.
 */
const MarkdownEditorToolbar = ({editor, isRaw, onToggleRaw}: MarkdownEditorToolbarProps) => {
    const activeState = useEditorState({
        editor,
        selector: ({editor: currentEditor}) => ({
            blockquote: currentEditor?.isActive('blockquote') ?? false,
            bold: currentEditor?.isActive('bold') ?? false,
            bulletList: currentEditor?.isActive('bulletList') ?? false,
            code: currentEditor?.isActive('code') ?? false,
            codeBlock: currentEditor?.isActive('codeBlock') ?? false,
            heading1: currentEditor?.isActive('heading', {level: 1}) ?? false,
            heading2: currentEditor?.isActive('heading', {level: 2}) ?? false,
            heading3: currentEditor?.isActive('heading', {level: 3}) ?? false,
            italic: currentEditor?.isActive('italic') ?? false,
            orderedList: currentEditor?.isActive('orderedList') ?? false,
            strike: currentEditor?.isActive('strike') ?? false,
        }),
    });

    const buttonGroups = useMemo<ToolbarButtonI[][]>(() => {
        if (!editor || !activeState) {
            return [];
        }

        return [
            [
                {
                    icon: BoldIcon,
                    isActive: activeState.bold,
                    label: 'Bold',
                    onToggle: () => editor.chain().focus().toggleBold().run(),
                },
                {
                    icon: ItalicIcon,
                    isActive: activeState.italic,
                    label: 'Italic',
                    onToggle: () => editor.chain().focus().toggleItalic().run(),
                },
                {
                    icon: StrikethroughIcon,
                    isActive: activeState.strike,
                    label: 'Strikethrough',
                    onToggle: () => editor.chain().focus().toggleStrike().run(),
                },
                {
                    icon: CodeIcon,
                    isActive: activeState.code,
                    label: 'Inline code',
                    onToggle: () => editor.chain().focus().toggleCode().run(),
                },
            ],
            [
                {
                    icon: Heading1Icon,
                    isActive: activeState.heading1,
                    label: 'Heading 1',
                    onToggle: () => editor.chain().focus().toggleHeading({level: 1}).run(),
                },
                {
                    icon: Heading2Icon,
                    isActive: activeState.heading2,
                    label: 'Heading 2',
                    onToggle: () => editor.chain().focus().toggleHeading({level: 2}).run(),
                },
                {
                    icon: Heading3Icon,
                    isActive: activeState.heading3,
                    label: 'Heading 3',
                    onToggle: () => editor.chain().focus().toggleHeading({level: 3}).run(),
                },
            ],
            [
                {
                    icon: ListIcon,
                    isActive: activeState.bulletList,
                    label: 'Bulleted list',
                    onToggle: () => editor.chain().focus().toggleBulletList().run(),
                },
                {
                    icon: ListOrderedIcon,
                    isActive: activeState.orderedList,
                    label: 'Numbered list',
                    onToggle: () => editor.chain().focus().toggleOrderedList().run(),
                },
                {
                    icon: QuoteIcon,
                    isActive: activeState.blockquote,
                    label: 'Quote',
                    onToggle: () => editor.chain().focus().toggleBlockquote().run(),
                },
                {
                    icon: SquareCodeIcon,
                    isActive: activeState.codeBlock,
                    label: 'Code block',
                    onToggle: () => editor.chain().focus().toggleCodeBlock().run(),
                },
            ],
        ];
    }, [activeState, editor]);

    if (!editor) {
        return null;
    }

    return (
        <div className="flex flex-wrap items-center gap-0.5 border-b border-input px-1 py-1">
            {buttonGroups.map((group, groupIndex) => (
                <div className="flex items-center gap-0.5" key={group[0].label}>
                    {groupIndex > 0 && <Separator className="mx-1 h-5" orientation="vertical" />}

                    {group.map((button) => (
                        <Toggle
                            aria-label={button.label}
                            disabled={isRaw}
                            key={button.label}
                            onMouseDown={(event) => event.preventDefault()}
                            onPressedChange={button.onToggle}
                            pressed={button.isActive}
                            size="sm"
                            title={button.label}
                        >
                            <button.icon />
                        </Toggle>
                    ))}
                </div>
            ))}

            <Toggle
                aria-label={isRaw ? 'Show formatted text' : 'Show Markdown source'}
                className="ml-auto"
                onPressedChange={onToggleRaw}
                pressed={isRaw}
                size="sm"
                title={isRaw ? 'Show formatted text' : 'Show Markdown source'}
            >
                <CodeXmlIcon />
            </Toggle>
        </div>
    );
};

export default MarkdownEditorToolbar;
