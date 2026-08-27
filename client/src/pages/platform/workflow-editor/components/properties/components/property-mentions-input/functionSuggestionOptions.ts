import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Editor} from '@tiptap/react';
import {SuggestionOptions} from '@tiptap/suggestion';

import FunctionSuggestionList from './FunctionSuggestionList';
import {
    buildFunctionInsertion,
    filterFunctionDefinitions,
    findFunctionSuggestionMatch,
    isFormulaModeActive,
} from './functionSuggestionUtils';
import {createSuggestionPopupRenderer} from './suggestionPopupRenderer';

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
        render: createSuggestionPopupRenderer<EvaluatorFunctionDefinition>(FunctionSuggestionList),
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
