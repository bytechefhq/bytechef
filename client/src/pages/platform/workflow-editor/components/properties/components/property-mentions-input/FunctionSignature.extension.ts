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
                    let popup: TippyInstance | undefined;

                    const hide = () => {
                        popup?.destroy();
                        component?.destroy();

                        popup = undefined;
                        component = undefined;
                    };

                    // The tippy popper is appended to document.body, so it outlives the editor's own DOM and
                    // is torn down only when `hide` runs. The plugin's `update` only fires on ProseMirror
                    // transactions/selection changes — a plain blur (clicking another field) dispatches no
                    // transaction, so without this listener the tooltip would orphan in <body> and stack up
                    // behind the next field's tooltip (tippy('body') never dedups). Hiding on blur guarantees
                    // the signature tooltip never persists past focus.
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
                            component = new ReactRenderer(FunctionSignatureTooltip, {editor, props});

                            popup = tippy('body', {
                                appendTo: () => document.body,
                                content: component.element,
                                getReferenceClientRect,
                                placement: 'top-start',
                                showOnCreate: true,
                                trigger: 'manual',
                            })[0];
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
