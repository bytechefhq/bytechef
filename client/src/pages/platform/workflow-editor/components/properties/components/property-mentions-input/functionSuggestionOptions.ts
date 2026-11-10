import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Node as ProseMirrorNode} from '@tiptap/pm/model';
import {Editor} from '@tiptap/react';
import {SuggestionOptions} from '@tiptap/suggestion';

import FunctionSuggestionList from './FunctionSuggestionList';
import {
    buildFunctionInsertion,
    filterFunctionDefinitions,
    filterFunctionDefinitionsForArgument,
    findEnclosingFunctionCallChain,
    findForcedFunctionSuggestionMatch,
    findFunctionSuggestionMatch,
    getPropertyFunctionType,
    isFormulaModeActive,
} from './functionSuggestionUtils';
import {createSuggestionPopupRenderer} from './suggestionPopupRenderer';

export const FORCED_FUNCTION_SUGGESTION_META = 'forcedFunctionSuggestion';

interface ContextFunctionDefinitionsPropsI {
    caret: number;
    document: ProseMirrorNode;
    editor: Editor;
    query: string;
    wordFrom: number;
}

function getContextFunctionDefinitions({
    caret,
    document,
    editor,
    query,
    wordFrom,
}: ContextFunctionDefinitionsPropsI): EvaluatorFunctionDefinition[] {
    const definitions: EvaluatorFunctionDefinition[] = editor.storage.FunctionSuggestion?.functionDefinitions ?? [];
    const $caret = document.resolve(caret);
    const textBeforeWord = document.textBetween($caret.start(), wordFrom, '\n', '');
    const textAfterCaret = document.textBetween(caret, $caret.end(), '\n', '');

    const {calls, isOutermostCallIncluded} = findEnclosingFunctionCallChain(textBeforeWord, textAfterCaret);
    const expressionType = isOutermostCallIncluded
        ? getPropertyFunctionType(editor.storage.MentionStorage?.propertyType)
        : null;

    return filterFunctionDefinitions(filterFunctionDefinitionsForArgument(definitions, calls, expressionType), query);
}

export interface ForcedFunctionSuggestionI {
    clear: () => void;
    getFrom: () => number | null;
}

export function getFunctionSuggestionOptions(
    forcedFunctionSuggestion?: ForcedFunctionSuggestionI
): Omit<SuggestionOptions<EvaluatorFunctionDefinition>, 'editor'> {
    const renderPopup = createSuggestionPopupRenderer<EvaluatorFunctionDefinition>(FunctionSuggestionList);

    return {
        // Stay open while already active even if the editor briefly loses focus — clicking a function
        // in the tippy popup blurs the editor, and a focus-only guard would tear the popup down before
        // the click lands. Mirrors the datapill suggestion's isActive survival check.
        allow: ({editor, isActive}) => (editor.isFocused || isActive === true) && isFormulaModeActive(editor),
        char: '',
        command: ({editor, props, range}) => {
            const textAfterRange = editor.state.doc.resolve(range.to).nodeAfter?.text ?? '';
            const {caretOffset, content} = buildFunctionInsertion(props.name, textAfterRange);

            editor.chain().focus().insertContentAt(range, content).run();

            editor.commands.setTextSelection(range.from + caretOffset);
        },
        findSuggestionMatch: (config) => {
            const forcedFrom = forcedFunctionSuggestion?.getFrom() ?? null;

            if (forcedFrom !== null) {
                const forcedMatch = findForcedFunctionSuggestionMatch(config, forcedFrom);

                if (forcedMatch) {
                    return forcedMatch;
                }

                forcedFunctionSuggestion?.clear();
            }

            return findFunctionSuggestionMatch(config);
        },
        items: ({editor, query}): EvaluatorFunctionDefinition[] => {
            const {doc, selection} = editor.state;

            return getContextFunctionDefinitions({
                caret: selection.from,
                document: doc,
                editor,
                query,
                wordFrom: selection.from - query.length,
            });
        },
        render: () => {
            const popupLifecycle = renderPopup();

            return {
                ...popupLifecycle,
                onExit: (props) => {
                    forcedFunctionSuggestion?.clear();

                    popupLifecycle.onExit?.(props);
                },
            };
        },
        shouldResetDismissed: ({transaction}) => transaction.getMeta(FORCED_FUNCTION_SUGGESTION_META) === true,
        shouldShow: ({editor, query, range, transaction}) =>
            getContextFunctionDefinitions({
                caret: transaction.selection.from,
                document: transaction.doc,
                editor,
                query,
                wordFrom: range.from,
            }).length > 0,
    };
}
