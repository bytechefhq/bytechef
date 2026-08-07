import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Editor, ReactRenderer} from '@tiptap/react';
import {SuggestionOptions} from '@tiptap/suggestion';
import tippy, {type Instance as TippyInstance} from 'tippy.js';

import FunctionSuggestionList, {FunctionSuggestionListRefType} from './FunctionSuggestionList';
import {
    buildFunctionInsertion,
    filterFunctionDefinitions,
    findFunctionSuggestionMatch,
    isFormulaModeActive,
} from './functionSuggestionUtils';

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

export function getFunctionSuggestionOptions(): Omit<SuggestionOptions<EvaluatorFunctionDefinition>, 'editor'> {
    return {
        // Stay open while already active even if the editor briefly loses focus — clicking a function
        // in the tippy popup blurs the editor, and a focus-only guard would tear the popup down before
        // the click lands. Mirrors the datapill suggestion's isActive survival check.
        allow: ({editor, isActive}) => (editor.isFocused || isActive === true) && isFormulaModeActive(editor),
        char: '',
        command: ({editor, props, range}) => {
            const {caretOffset, content} = buildFunctionInsertion(props.name);

            editor.chain().focus().insertContentAt(range, content).run();

            editor.commands.setTextSelection(range.from + caretOffset);
        },
        findSuggestionMatch: findFunctionSuggestionMatch,
        items: ({editor, query}): EvaluatorFunctionDefinition[] => {
            const definitions: EvaluatorFunctionDefinition[] =
                editor.storage.FunctionSuggestion?.functionDefinitions ?? [];

            return filterFunctionDefinitions(definitions, query);
        },
        render: () => {
            let component: ReactRenderer<FunctionSuggestionListRefType> | undefined;
            let popup: TippyInstance | undefined;
            let lastValidRect: DOMRect = DOM_RECT_FALLBACK;
            let wheelAbortController: AbortController | undefined;

            return {
                onExit() {
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

                onStart: (props) => {
                    if (!props.editor.isFocused) {
                        return;
                    }

                    component = new ReactRenderer(FunctionSuggestionList, {
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
        },
        // Don't activate the suggestion (and therefore don't open a popup) unless at least one function
        // matches. The trailing-word match fires on any word — including plain argument values like
        // "true" — so without this the popup would appear empty ("No functions found.") next to the
        // signature tooltip.
        shouldShow: ({editor, query}: {editor: Editor; query: string}) => {
            const definitions: EvaluatorFunctionDefinition[] =
                editor.storage.FunctionSuggestion?.functionDefinitions ?? [];

            return filterFunctionDefinitions(definitions, query).length > 0;
        },
    };
}
