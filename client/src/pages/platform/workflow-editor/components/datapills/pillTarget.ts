import {Editor} from '@tiptap/react';

export interface PillTargetI {
    acceptsPill: () => boolean;
    insertPill: (mentionId: string) => void;
    owner: unknown;
}

interface CreateEditorPillTargetPropsI {
    acceptsPill: () => boolean;
    editor: Editor;
    singlePill?: boolean;
}

export function createEditorPillTarget({
    acceptsPill,
    editor,
    singlePill = false,
}: CreateEditorPillTargetPropsI): PillTargetI {
    return {
        acceptsPill,
        insertPill: (mentionId) => {
            const mention = {attrs: {id: mentionId}, type: 'mention'};

            if (singlePill) {
                editor.chain().focus().selectAll().insertContent(mention).run();

                return;
            }

            editor.chain().focus().insertContent(mention).run();
        },
        owner: editor,
    };
}
