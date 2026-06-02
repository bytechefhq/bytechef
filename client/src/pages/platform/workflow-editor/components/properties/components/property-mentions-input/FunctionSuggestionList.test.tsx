import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {SuggestionProps} from '@tiptap/suggestion';
import {type Ref, createRef} from 'react';
import {describe, expect, it, vi} from 'vitest';

import FunctionSuggestionList, {FunctionSuggestionListRefType} from './FunctionSuggestionList';

function definition(name: string, overrides: Partial<EvaluatorFunctionDefinition> = {}): EvaluatorFunctionDefinition {
    return {
        category: 'STRING',
        description: `${name} description`,
        example: `${name}(...)`,
        name,
        parameters: [],
        returnType: 'STRING',
        title: name,
        ...overrides,
    } as EvaluatorFunctionDefinition;
}

// The list only consumes command and items; the remaining SuggestionProps fields are irrelevant here.
function renderFunctionSuggestionList(
    items: EvaluatorFunctionDefinition[],
    command: SuggestionProps<EvaluatorFunctionDefinition>['command'],
    ref?: Ref<FunctionSuggestionListRefType>
) {
    const props = {command, items} as unknown as SuggestionProps<EvaluatorFunctionDefinition>;

    return render(<FunctionSuggestionList {...props} ref={ref} />);
}

describe('FunctionSuggestionList', () => {
    it('renders the function names and shows detail for the highlighted row only', () => {
        renderFunctionSuggestionList([definition('concat'), definition('contains')], vi.fn());

        expect(screen.getByText('concat')).toBeInTheDocument();
        expect(screen.getByText('contains')).toBeInTheDocument();
        // First row is highlighted by default -> its description is shown.
        expect(screen.getByText('concat description')).toBeInTheDocument();
        // Non-highlighted row detail is not rendered.
        expect(screen.queryByText('contains description')).not.toBeInTheDocument();
    });

    it('renders an empty state when there are no items', () => {
        renderFunctionSuggestionList([], vi.fn());

        expect(screen.getByText('No functions found.')).toBeInTheDocument();
    });

    it('calls command with the clicked definition', () => {
        const command = vi.fn();
        const items = [definition('concat'), definition('contains')];

        renderFunctionSuggestionList(items, command);

        fireEvent.click(screen.getByText('contains'));

        expect(command).toHaveBeenCalledWith(items[1]);
    });

    it('selects the highlighted item on Enter via the ref handler', () => {
        const command = vi.fn();
        const items = [definition('concat'), definition('contains')];
        const ref = createRef<FunctionSuggestionListRefType>();

        renderFunctionSuggestionList(items, command, ref);

        act(() => {
            ref.current!.onKeyDown({event: new KeyboardEvent('keydown', {key: 'ArrowDown'})} as never);
        });

        act(() => {
            ref.current!.onKeyDown({event: new KeyboardEvent('keydown', {key: 'Enter'})} as never);
        });

        expect(command).toHaveBeenCalledWith(items[1]);
    });
});
