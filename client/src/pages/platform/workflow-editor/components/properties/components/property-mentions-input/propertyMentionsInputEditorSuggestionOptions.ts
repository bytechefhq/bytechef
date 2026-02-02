import PropertyMentionsInputEditorSuggestionList, {
    PropertyMentionsInputListRefType,
} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditorSuggestionList';
import {DataPillType} from '@/shared/types';
import {MentionOptions} from '@tiptap/extension-mention';
import {Editor, ReactRenderer} from '@tiptap/react';
import tippy, {type Instance as TippyInstance} from 'tippy.js';

/**
 * Workaround for the current typing incompatibility between Tippy.js and Tiptap
 * Suggestion utility.
 *
 * @see https://github.com/ueberdosis/tiptap/issues/2795#issuecomment-1160623792
 *
 * Adopted from
 * https://github.com/Doist/typist/blob/a1726a6be089e3e1452def641dfcfc622ac3e942/stories/typist-editor/constants/suggestions.ts#L169-L186
 */
const DOM_RECT_FALLBACK: DOMRect = {
    bottom: 0,
    height: 0,
    left: 0,
    right: 0,
    toJSON() {
        return {};
    },
    top: 0,
    width: 0,
    x: 0,
    y: 0,
};

export function getSuggestionOptions(): MentionOptions['suggestion'] {
    return {
        allow: ({editor, range}) => {
            const editorContent = editor.state.doc.textContent;

            if (range.from === 2) {
                return editorContent.charAt(0) !== '#';
            }

            return true;
        },
        allowedPrefixes: null,
        char: '$',
        // Prevent space insertion after adding mention
        command: ({editor, props, range}) => {
            editor
                .chain()
                .focus()
                .insertContentAt(range, [
                    {
                        attrs: props,
                        type: 'mention',
                    },
                ])
                .run();
        },
        items: ({editor, query}: {editor: Editor; query: string}): DataPillType[] => {
            const dataPills: DataPillType[] = editor.storage.MentionStorage.dataPills ?? [];

            return dataPills.filter((dataPill) => dataPill.value.toLowerCase().startsWith(query.toLowerCase()));
        },
        render: () => {
            let component: ReactRenderer<PropertyMentionsInputListRefType> | undefined;
            let popup: TippyInstance | undefined;

            return {
                onExit() {
                    popup?.destroy();
                    component?.destroy();
                },

                onKeyDown(props) {
                    if (props.event.key === 'Escape') {
                        popup?.hide();

                        return true;
                    }

                    if (!component?.ref) {
                        return false;
                    }

                    return component?.ref.onKeyDown(props);
                },

                onStart: (props) => {
                    component = new ReactRenderer(PropertyMentionsInputEditorSuggestionList, {
                        editor: props.editor,
                        props,
                    });

                    if (!props.clientRect) {
                        return;
                    }

                    popup = tippy('body', {
                        appendTo: () => document.body,
                        content: component.element,
                        getReferenceClientRect: () => props.clientRect?.() ?? DOM_RECT_FALLBACK,
                        interactive: true,
                        placement: 'bottom-start',
                        showOnCreate: true,
                        trigger: 'manual',
                    })[0];
                },

                onUpdate(props) {
                    component?.updateProps(props);

                    if (!props.clientRect) {
                        return;
                    }

                    popup?.setProps({
                        getReferenceClientRect: () => props.clientRect?.() ?? DOM_RECT_FALLBACK,
                    });
                },
            };
        },
    };
}
