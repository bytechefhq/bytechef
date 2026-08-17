import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Extension} from '@tiptap/core';
import {Plugin, PluginKey} from '@tiptap/pm/state';
import {ReactRenderer} from '@tiptap/react';
import tippy, {type Instance as TippyInstance} from 'tippy.js';

import FunctionSignatureTooltip from './FunctionSignatureTooltip';
import {findEnclosingFunctionCall, isFormulaModeActive} from './functionSuggestionUtils';

export const FunctionSignaturePluginKey = new PluginKey('functionSignature');

export const FunctionSignature = Extension.create({
    addProseMirrorPlugins() {
        const editor = this.editor;

        return [
            new Plugin({
                key: FunctionSignaturePluginKey,
                view() {
                    let component: ReactRenderer | undefined;
                    let isCreating = false;
                    let popup: TippyInstance | undefined;

                    // Each editor gets its OWN detached reference node rather than sharing document.body.
                    // tippy stamps the instance onto the reference as `reference._tippy` with no dedup
                    // (createTippy overwrites it), so every property input on the page creating against `body`
                    // clobbers the previous registration — and destroy() deletes `body._tippy`, leaving the
                    // other instance's popper mounted with nothing able to reach it again. That orphan is what
                    // showed as a signature tooltip surviving after its function text was deleted, and as two
                    // tooltips stacked side by side. The node is never inserted into the document: positioning
                    // comes entirely from getReferenceClientRect, and appendTo still puts the popper in <body>.
                    const referenceElement = document.createElement('div');

                    const hide = () => {
                        popup?.destroy();
                        component?.destroy();

                        popup = undefined;
                        component = undefined;
                    };

                    // The tippy popper is appended to document.body, so it outlives the editor's own DOM and
                    // is torn down only when `hide` runs. The plugin's `update` only fires on ProseMirror
                    // transactions/selection changes — a plain blur (clicking another field) dispatches no
                    // transaction, so without this listener the tooltip would persist past focus.
                    editor.on('blur', hide);

                    const update = () => {
                        const {state} = editor.view;
                        const {selection} = state;

                        if (!editor.isFocused || !selection.empty || !isFormulaModeActive(editor)) {
                            hide();

                            return;
                        }

                        const caret = selection.from;
                        const blockStart = selection.$from.start();
                        const blockEnd = selection.$from.end();
                        const textBeforeCaret = state.doc.textBetween(blockStart, caret, '\n', '');
                        const textAfterCaret = state.doc.textBetween(caret, blockEnd, '\n', '');

                        const call = findEnclosingFunctionCall(textBeforeCaret, textAfterCaret);

                        if (!call) {
                            hide();

                            return;
                        }

                        const definitions: EvaluatorFunctionDefinition[] =
                            editor.storage.FunctionSuggestion?.functionDefinitions ?? [];
                        const definition = definitions.find((candidate) => candidate.name === call.name);

                        if (!definition) {
                            hide();

                            return;
                        }

                        const coords = editor.view.coordsAtPos(caret);
                        const getReferenceClientRect = () =>
                            ({
                                bottom: coords.bottom,
                                height: 0,
                                left: coords.left,
                                right: coords.left,
                                toJSON() {
                                    return {};
                                },
                                top: coords.top,
                                width: 0,
                                x: coords.left,
                                y: coords.top,
                            }) as DOMRect;

                        const props = {activeArgIndex: call.argIndex, definition};

                        if (component) {
                            component.updateProps(props);

                            popup?.setProps({getReferenceClientRect});
                        } else {
                            // `new ReactRenderer` renders through flushSync, which drains React's pending
                            // effects — among them tiptap's own useEditor effect calling `editor.setOptions`,
                            // which runs `view.setProps` -> `updatePluginViews` -> back into this `update`
                            // before either assignment below has happened. That nested call would also find
                            // `component` unset, create a second renderer and popper, and then have both
                            // clobbered by the outer call's assignments — orphaning a popper in <body> with
                            // no instance left to hide or destroy it. The nested call has nothing to add
                            // over the creation already in flight, so it bails out here instead.
                            if (isCreating) {
                                return;
                            }

                            isCreating = true;

                            try {
                                component = new ReactRenderer(FunctionSignatureTooltip, {editor, props});

                                popup = tippy(referenceElement, {
                                    appendTo: () => document.body,
                                    content: component.element,
                                    getReferenceClientRect,
                                    placement: 'top-start',
                                    showOnCreate: true,
                                    trigger: 'manual',
                                });
                            } finally {
                                isCreating = false;
                            }
                        }
                    };

                    return {
                        destroy: () => {
                            editor.off('blur', hide);

                            hide();
                        },
                        update,
                    };
                },
            }),
        ];
    },
    name: 'FunctionSignature',
});
