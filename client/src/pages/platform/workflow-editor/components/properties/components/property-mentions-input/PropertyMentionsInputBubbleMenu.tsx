import './PropertyMentionsInputBubbleMenu.css';

import {Editor, useEditorState} from '@tiptap/react';
import {BubbleMenu} from '@tiptap/react/menus';

const PropertyMentionsInputBubbleMenu = ({editor}: {editor: Editor}) => {
    const editorState = useEditorState({
        editor,
        // This function will be called every time the editor state changes
        selector: ({editor}: {editor: Editor}) => ({
            isBold: editor.isActive('bold'),
            isBulletList: editor.isActive('bulletList'),
            isCode: editor.isActive('code'),
            isCodeBlock: editor.isActive('codeBlock'),
            isHeading1: editor.isActive('heading', {level: 1}),
            isHeading2: editor.isActive('heading', {level: 2}),
            isHeading3: editor.isActive('heading', {level: 3}),
            isHeading4: editor.isActive('heading', {level: 4}),
            isHeading5: editor.isActive('heading', {level: 5}),
            isHeading6: editor.isActive('heading', {level: 6}),
            isItalic: editor.isActive('italic'),
            isOrderedList: editor.isActive('orderedList'),
            isStrike: editor.isActive('strike'),
        }),
    });

    return (
        <BubbleMenu className="property-bubble-menu" editor={editor}>
            <div className="flex flex-col">
                <div className="flex">
                    <button
                        className={editorState.isBold ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleBold().run()}
                    >
                        Bold
                    </button>

                    <button
                        className={editorState.isItalic ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleItalic().run()}
                    >
                        Italic
                    </button>

                    <button
                        className={editorState.isStrike ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleStrike().run()}
                    >
                        Strike
                    </button>

                    <button
                        className={editorState.isCode ? 'is-active' : ''}
                        disabled={!editor.can().chain().focus().toggleCode().run()}
                        onClick={() => editor.chain().focus().toggleCode().run()}
                    >
                        Code
                    </button>

                    <button
                        className={editorState.isHeading1 ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleHeading({level: 1}).run()}
                    >
                        H1
                    </button>

                    <button
                        className={editorState.isHeading2 ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleHeading({level: 2}).run()}
                    >
                        H2
                    </button>

                    <button
                        className={editorState.isHeading3 ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleHeading({level: 3}).run()}
                    >
                        H3
                    </button>

                    <button
                        className={editorState.isHeading4 ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleHeading({level: 4}).run()}
                    >
                        H4
                    </button>

                    <button
                        className={editorState.isHeading5 ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleHeading({level: 5}).run()}
                    >
                        H5
                    </button>

                    <button
                        className={editorState.isHeading6 ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleHeading({level: 6}).run()}
                    >
                        H6
                    </button>
                </div>

                <div className="flex">
                    <button
                        className={editorState.isBulletList ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleBulletList().run()}
                    >
                        Bullet list
                    </button>

                    <button
                        className={editorState.isOrderedList ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleOrderedList().run()}
                    >
                        Ordered list
                    </button>

                    <button
                        className={editorState.isCodeBlock ? 'is-active' : ''}
                        onClick={() => editor.chain().focus().toggleCodeBlock().run()}
                    >
                        Code block
                    </button>
                </div>
            </div>
        </BubbleMenu>
    );
};

export default PropertyMentionsInputBubbleMenu;
