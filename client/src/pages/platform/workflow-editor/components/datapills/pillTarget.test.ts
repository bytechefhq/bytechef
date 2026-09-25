import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Editor} from '@tiptap/core';
import Document from '@tiptap/extension-document';
import {Mention} from '@tiptap/extension-mention';
import Paragraph from '@tiptap/extension-paragraph';
import Text from '@tiptap/extension-text';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {createEditorPillTarget} from './pillTarget';

const createEditor = (content: string) =>
    new Editor({content, extensions: [Document, Paragraph, Text, Mention.configure({})]});

const mentionIds = (editor: Editor) => {
    const ids: string[] = [];

    editor.state.doc.descendants((node) => {
        if (node.type.name === 'mention') {
            ids.push(node.attrs.id);
        }
    });

    return ids;
};

describe('pill target', () => {
    let editor: Editor;

    beforeEach(() => {
        useWorkflowNodeDetailsPanelStore.setState({pillTarget: null});
    });

    afterEach(() => {
        editor?.destroy();
    });

    it('inserts a pill next to existing content', () => {
        editor = createEditor('<p>hello </p>');

        createEditorPillTarget({acceptsPill: () => true, editor}).insertPill('trigger_1.id');

        expect(mentionIds(editor)).toEqual(['trigger_1.id']);
        expect(editor.state.doc.textContent).toContain('hello');
    });

    it('replaces the whole content in single-pill mode', () => {
        editor = createEditor('<p><span data-type="mention" data-id="old.pill"></span></p>');

        createEditorPillTarget({acceptsPill: () => true, editor, singlePill: true}).insertPill('new.pill');

        expect(mentionIds(editor)).toEqual(['new.pill']);
    });

    it('clears the target only for its own owner', () => {
        editor = createEditor('<p></p>');

        const pillTarget = createEditorPillTarget({acceptsPill: () => true, editor});

        useWorkflowNodeDetailsPanelStore.getState().setPillTarget(pillTarget);
        useWorkflowNodeDetailsPanelStore.getState().clearPillTarget({});

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget).toBe(pillTarget);

        useWorkflowNodeDetailsPanelStore.getState().clearPillTarget(editor);

        expect(useWorkflowNodeDetailsPanelStore.getState().pillTarget).toBeNull();
    });
});
