import {act, renderHook} from '@testing-library/react';
import {isValidElement} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useApprovalTasksStore} from '../../../stores/useApprovalTasksStore';
import {type ApprovalTaskI} from '../../../types/types';
import {useApprovalTaskSearch} from '../useApprovalTaskSearch';

import type {KeyboardEvent} from 'react';

const createApprovalTask = (overrides: Partial<ApprovalTaskI>): ApprovalTaskI => ({
    assignee: 'Nobody',
    attachments: [],
    comments: [],
    createdAt: '2026-01-01T00:00:00.000Z',
    dependencies: [],
    description: '',
    priority: 'medium',
    status: 'open',
    title: 'Untitled',
    ...overrides,
});

const createKeyboardEvent = (key: string) =>
    ({
        key,
        preventDefault: vi.fn(),
    }) as unknown as KeyboardEvent & {preventDefault: ReturnType<typeof vi.fn>};

const getSuggestionTexts = (suggestions: Array<{text: string}>) => suggestions.map((suggestion) => suggestion.text);

describe('useApprovalTaskSearch', () => {
    let searchInput: HTMLInputElement;

    beforeEach(() => {
        useApprovalTasksStore.setState({
            approvalTasks: [],
            filters: {assignee: 'all', priority: 'all', status: 'all'},
            searchQuery: '',
        });

        searchInput = document.createElement('input');

        document.body.appendChild(searchInput);
    });

    afterEach(() => {
        searchInput.remove();
    });

    const renderSearchHook = () => {
        const hookResult = renderHook(() => useApprovalTaskSearch());

        hookResult.result.current.searchInputRef.current = searchInput;

        return hookResult;
    };

    describe('suggestions', () => {
        it('returns no suggestions for an empty or whitespace-only query', () => {
            useApprovalTasksStore.setState({
                approvalTasks: [createApprovalTask({assignee: 'Alice', title: 'Review contract'})],
                searchQuery: '   ',
            });

            const {result} = renderSearchHook();

            expect(result.current.suggestions).toEqual([]);
        });

        it('suggests titles before assignees, matching case-insensitively', () => {
            useApprovalTasksStore.setState({
                approvalTasks: [
                    createApprovalTask({assignee: 'Revan Smith', title: 'Approve budget'}),
                    createApprovalTask({assignee: 'Alice', title: 'Review contract'}),
                    createApprovalTask({assignee: 'Bob', title: 'Unrelated'}),
                ],
                searchQuery: 'REV',
            });

            const {result} = renderSearchHook();

            expect(getSuggestionTexts(result.current.suggestions)).toEqual(['Review contract', 'Revan Smith']);
        });

        it('omits a suggestion that is identical to the query', () => {
            useApprovalTasksStore.setState({
                approvalTasks: [
                    createApprovalTask({assignee: 'Alice', title: 'review'}),
                    createApprovalTask({assignee: 'Alice', title: 'Review contract'}),
                ],
                searchQuery: 'Review',
            });

            const {result} = renderSearchHook();

            expect(getSuggestionTexts(result.current.suggestions)).toEqual(['Review contract']);
        });

        it('only suggests approval tasks that match the active status, priority and assignee filters', () => {
            useApprovalTasksStore.setState({
                approvalTasks: [
                    createApprovalTask({assignee: 'Alice', priority: 'high', status: 'open', title: 'Task one'}),
                    createApprovalTask({assignee: 'Alice', priority: 'low', status: 'open', title: 'Task two'}),
                    createApprovalTask({assignee: 'Alice', priority: 'high', status: 'completed', title: 'Task three'}),
                    createApprovalTask({assignee: 'Bob', priority: 'high', status: 'open', title: 'Task four'}),
                ],
                filters: {assignee: 'Alice', priority: 'high', status: 'open'},
                searchQuery: 'task',
            });

            const {result} = renderSearchHook();

            expect(getSuggestionTexts(result.current.suggestions)).toEqual(['Task one']);
        });

        it('limits the list to five suggestions', () => {
            useApprovalTasksStore.setState({
                approvalTasks: Array.from({length: 7}, (_, index) =>
                    createApprovalTask({assignee: 'Bob', title: `Task ${index + 1}`})
                ),
                searchQuery: 'task',
            });

            const {result} = renderSearchHook();

            expect(getSuggestionTexts(result.current.suggestions)).toEqual([
                'Task 1',
                'Task 2',
                'Task 3',
                'Task 4',
                'Task 5',
            ]);
        });

        it('removes duplicates before applying the five-suggestion limit', () => {
            useApprovalTasksStore.setState({
                approvalTasks: [
                    ...Array.from({length: 5}, () => createApprovalTask({assignee: 'Bob', title: 'Weekly review'})),
                    createApprovalTask({assignee: 'Bob', title: 'Quarterly review'}),
                    createApprovalTask({assignee: 'Reviewer Carol', title: 'Other'}),
                ],
                searchQuery: 'review',
            });

            const {result} = renderSearchHook();

            expect(getSuggestionTexts(result.current.suggestions)).toEqual([
                'Weekly review',
                'Quarterly review',
                'Reviewer Carol',
            ]);
        });

        it('highlights the matching part of each suggestion', () => {
            useApprovalTasksStore.setState({
                approvalTasks: [createApprovalTask({title: 'Review contract'})],
                searchQuery: 'contract',
            });

            const {result} = renderSearchHook();

            const highlighted = result.current.suggestions[0].highlighted as Array<unknown>;

            expect(highlighted[0]).toBe('Review ');
            expect(isValidElement(highlighted[1])).toBe(true);
        });
    });

    describe('handleSearchChange', () => {
        it('stores the query and opens the suggestions for a non-empty value', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('rev');
            });

            expect(useApprovalTasksStore.getState().searchQuery).toBe('rev');
            expect(result.current.searchQuery).toBe('rev');
            expect(result.current.showSuggestions).toBe(true);
            expect(result.current.selectedSuggestionIndex).toBe(-1);
        });

        it('closes the suggestions when the value is cleared', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('rev');
            });

            act(() => {
                result.current.handleSearchChange('');
            });

            expect(result.current.showSuggestions).toBe(false);
        });
    });

    describe('handleKeyDown', () => {
        beforeEach(() => {
            useApprovalTasksStore.setState({
                approvalTasks: [
                    createApprovalTask({title: 'Task one'}),
                    createApprovalTask({title: 'Task two'}),
                    createApprovalTask({title: 'Task three'}),
                ],
            });
        });

        it('does nothing while the suggestions are hidden', () => {
            useApprovalTasksStore.setState({searchQuery: 'task'});

            const {result} = renderSearchHook();

            const arrowDownEvent = createKeyboardEvent('ArrowDown');

            act(() => {
                result.current.handleKeyDown(arrowDownEvent);
            });

            expect(arrowDownEvent.preventDefault).not.toHaveBeenCalled();
            expect(result.current.selectedSuggestionIndex).toBe(-1);
        });

        it('does nothing when there are no suggestions', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('missing');
            });

            const arrowDownEvent = createKeyboardEvent('ArrowDown');

            act(() => {
                result.current.handleKeyDown(arrowDownEvent);
            });

            expect(arrowDownEvent.preventDefault).not.toHaveBeenCalled();
        });

        it('moves the selection down and stops at the last suggestion', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('task');
            });

            for (let pressCount = 0; pressCount < 5; pressCount++) {
                act(() => {
                    result.current.handleKeyDown(createKeyboardEvent('ArrowDown'));
                });
            }

            expect(result.current.selectedSuggestionIndex).toBe(2);
        });

        it('moves the selection up and back to no selection', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('task');
            });

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowDown'));
            });

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowDown'));
            });

            const arrowUpEvent = createKeyboardEvent('ArrowUp');

            act(() => {
                result.current.handleKeyDown(arrowUpEvent);
            });

            expect(arrowUpEvent.preventDefault).toHaveBeenCalled();
            expect(result.current.selectedSuggestionIndex).toBe(0);

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowUp'));
            });

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowUp'));
            });

            expect(result.current.selectedSuggestionIndex).toBe(-1);
        });

        it('applies the selected suggestion on Enter and focuses the input', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('task');
            });

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowDown'));
            });

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowDown'));
            });

            const enterEvent = createKeyboardEvent('Enter');

            act(() => {
                result.current.handleKeyDown(enterEvent);
            });

            expect(enterEvent.preventDefault).toHaveBeenCalled();
            expect(useApprovalTasksStore.getState().searchQuery).toBe('Task two');
            expect(result.current.showSuggestions).toBe(false);
            expect(result.current.selectedSuggestionIndex).toBe(-1);
            expect(document.activeElement).toBe(searchInput);
        });

        it('keeps the typed query on Enter when nothing is selected', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('task');
            });

            const enterEvent = createKeyboardEvent('Enter');

            act(() => {
                result.current.handleKeyDown(enterEvent);
            });

            expect(enterEvent.preventDefault).toHaveBeenCalled();
            expect(useApprovalTasksStore.getState().searchQuery).toBe('task');
            expect(result.current.showSuggestions).toBe(true);
        });

        it('closes the suggestions on Escape', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('task');
            });

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowDown'));
            });

            const escapeEvent = createKeyboardEvent('Escape');

            act(() => {
                result.current.handleKeyDown(escapeEvent);
            });

            expect(escapeEvent.preventDefault).not.toHaveBeenCalled();
            expect(result.current.showSuggestions).toBe(false);
            expect(result.current.selectedSuggestionIndex).toBe(-1);
        });
    });

    describe('handleSuggestionClick', () => {
        it('stores the suggestion, closes the list and focuses the input', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('rev');
            });

            act(() => {
                result.current.handleSuggestionClick('Review contract');
            });

            expect(useApprovalTasksStore.getState().searchQuery).toBe('Review contract');
            expect(result.current.showSuggestions).toBe(false);
            expect(document.activeElement).toBe(searchInput);
        });
    });

    describe('handleInputFocus', () => {
        it('reopens the suggestions when a query is present', () => {
            useApprovalTasksStore.setState({searchQuery: 'task'});

            const {result} = renderSearchHook();

            act(() => {
                result.current.handleInputFocus();
            });

            expect(result.current.showSuggestions).toBe(true);
        });

        it('keeps the suggestions closed when the query is empty', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleInputFocus();
            });

            expect(result.current.showSuggestions).toBe(false);
        });
    });

    describe('click outside', () => {
        it('closes the suggestions on a mousedown outside the input', () => {
            useApprovalTasksStore.setState({approvalTasks: [createApprovalTask({title: 'Task one'})]});

            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('task');
            });

            act(() => {
                result.current.handleKeyDown(createKeyboardEvent('ArrowDown'));
            });

            act(() => {
                document.body.dispatchEvent(new MouseEvent('mousedown', {bubbles: true}));
            });

            expect(result.current.showSuggestions).toBe(false);
            expect(result.current.selectedSuggestionIndex).toBe(-1);
        });

        it('keeps the suggestions open on a mousedown inside the input', () => {
            const {result} = renderSearchHook();

            act(() => {
                result.current.handleSearchChange('task');
            });

            act(() => {
                searchInput.dispatchEvent(new MouseEvent('mousedown', {bubbles: true}));
            });

            expect(result.current.showSuggestions).toBe(true);
        });

        it('stops listening for mousedown after unmount', () => {
            const removeEventListenerSpy = vi.spyOn(document, 'removeEventListener');

            const {unmount} = renderSearchHook();

            unmount();

            expect(removeEventListenerSpy).toHaveBeenCalledWith('mousedown', expect.any(Function));

            removeEventListenerSpy.mockRestore();
        });
    });
});
