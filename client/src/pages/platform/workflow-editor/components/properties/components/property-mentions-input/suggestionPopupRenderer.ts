import {Editor, ReactRenderer} from '@tiptap/react';
import {SuggestionKeyDownProps, SuggestionOptions, SuggestionProps} from '@tiptap/suggestion';
import {ForwardRefExoticComponent, PropsWithoutRef, RefAttributes} from 'react';
import tippy, {type Instance as TippyInstance} from 'tippy.js';

import {setMentionSuggestionOpen} from './MentionStorage.extension';

export type SuggestionListRefType = {
    onKeyDown: (props: SuggestionKeyDownProps) => boolean;
};

type SuggestionListComponentType<ItemType> = ForwardRefExoticComponent<
    PropsWithoutRef<SuggestionProps<ItemType>> & RefAttributes<SuggestionListRefType>
>;

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

/**
 * The tippy popup lifecycle shared by the editor's suggestion sources — `$` datapills and, in formula
 * mode, evaluator functions. Both anchor a keyboard-navigable list at the caret in a manually
 * triggered tippy, so only the list component differs.
 */
export function createSuggestionPopupRenderer<ItemType>(
    listComponent: SuggestionListComponentType<ItemType>
): NonNullable<SuggestionOptions<ItemType>['render']> {
    return () => {
        let component: ReactRenderer<SuggestionListRefType> | undefined;
        let popup: TippyInstance | undefined;
        let lastValidRect: DOMRect = DOM_RECT_FALLBACK;
        let wheelAbortController: AbortController | undefined;
        // Held so `onExit` can clear the open flag. It has no props of its own, and it also runs
        // for a suggestion `onStart` declined to show, where there is nothing to clear.
        let openedEditor: Editor | undefined;

        return {
            onExit() {
                if (openedEditor) {
                    setMentionSuggestionOpen(openedEditor, false);

                    openedEditor = undefined;
                }

                wheelAbortController?.abort();
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

                return component.ref.onKeyDown(props);
            },

            onStart(props) {
                if (!props.editor.isFocused) {
                    return;
                }

                component = new ReactRenderer(listComponent, {
                    editor: props.editor,
                    props,
                });

                const initialRect = props.clientRect?.();

                if (!initialRect) {
                    component.destroy();
                    component = undefined;

                    return;
                }

                lastValidRect = initialRect;

                popup = tippy('body', {
                    appendTo: () => document.body,
                    content: component.element,
                    getReferenceClientRect: () => lastValidRect,
                    interactive: true,
                    placement: 'bottom-start',
                    showOnCreate: true,
                    trigger: 'manual',
                })[0];

                // Inside a Radix modal dialog, react-remove-scroll blocks wheel events outside the
                // dialog content. Intercept them on the popup before they reach the document
                // listener that would preventDefault on them. Pointer events are re-enabled by the
                // menu's own stylesheet, since tippy resets the popper's inline value on setProps.
                wheelAbortController = new AbortController();

                popup.popper.addEventListener('wheel', (event) => event.stopPropagation(), {
                    capture: true,
                    passive: true,
                    signal: wheelAbortController.signal,
                });

                openedEditor = props.editor;

                setMentionSuggestionOpen(props.editor, true);
            },

            onUpdate(props) {
                component?.updateProps(props);

                const updatedRect = props.clientRect?.();

                if (!updatedRect) {
                    return;
                }

                lastValidRect = updatedRect;

                popup?.setProps({
                    getReferenceClientRect: () => lastValidRect,
                });
            },
        };
    };
}
