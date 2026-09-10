import {Tag} from '@/shared/middleware/graphql';
import {createTestQueryClientWrapper, render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import AiSkillListItemTagList from '../AiSkillListItemTagList';

const {tagListPropsMock, updateTagsOptionsRef, useAiSkillTagsQueryMock, useUpdateAiSkillTagsMutationMock} = vi.hoisted(
    () => ({
        tagListPropsMock: vi.fn(),
        updateTagsOptionsRef: {current: undefined as {onError?: (error: Error) => void} | undefined},
        useAiSkillTagsQueryMock: vi.fn(),
        useUpdateAiSkillTagsMutationMock: vi.fn(),
    })
);

vi.mock('@/shared/components/TagList', () => ({
    default: (props: Record<string, unknown>) => {
        tagListPropsMock(props);

        return <div data-testid="tag-list" />;
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiSkillTagsQuery: useAiSkillTagsQueryMock,
    useUpdateAiSkillTagsMutation: useUpdateAiSkillTagsMutationMock,
}));

const {toastErrorMock} = vi.hoisted(() => ({toastErrorMock: vi.fn()}));

vi.mock('sonner', () => ({toast: {error: toastErrorMock}}));

const renderTagList = (tags: Tag[], allTags: Tag[]) => {
    useAiSkillTagsQueryMock.mockReturnValue({data: {aiSkillTags: allTags}});
    useUpdateAiSkillTagsMutationMock.mockImplementation((options: {onError?: (error: Error) => void}) => {
        updateTagsOptionsRef.current = options;

        return {mutate: vi.fn()};
    });

    render(<AiSkillListItemTagList skillId="42" tags={tags} />, {wrapper: createTestQueryClientWrapper()});

    return tagListPropsMock.mock.calls.at(-1)![0];
};

describe('AiSkillListItemTagList', () => {
    afterEach(() => {
        resetAll();

        updateTagsOptionsRef.current = undefined;
    });

    it('hands TagList the skill id as a number', () => {
        const props = renderTagList([], []);

        expect(props.id).toBe(42);
        expect(screen.getByTestId('tag-list')).toBeInTheDocument();
    });

    it('converts the skill tag ids from strings to numbers', () => {
        const props = renderTagList([{id: '1', name: 'Billing'} as Tag], []);

        expect(props.tags).toEqual([{id: 1, name: 'Billing'}]);
    });

    it('offers only the tags the skill does not already carry', () => {
        const props = renderTagList(
            [{id: '1', name: 'Billing'} as Tag],
            [{id: '1', name: 'Billing'} as Tag, {id: '2', name: 'Support'} as Tag]
        );

        expect(props.remainingTags).toEqual([{id: 2, name: 'Support'}]);
    });

    it('offers nothing extra while the tag query has not resolved', () => {
        useAiSkillTagsQueryMock.mockReturnValue({data: undefined});
        useUpdateAiSkillTagsMutationMock.mockReturnValue({mutate: vi.fn()});

        render(<AiSkillListItemTagList skillId="42" tags={[]} />, {wrapper: createTestQueryClientWrapper()});

        expect(tagListPropsMock.mock.calls.at(-1)![0].remainingTags).toEqual([]);
    });

    it('builds an update request carrying the skill id and the new tag set', () => {
        const props = renderTagList([], []);

        expect(props.getRequest(42, [{id: 5, name: 'Billing'}, {name: 'Fresh'}])).toEqual({
            id: '42',
            tags: [
                {id: '5', name: 'Billing'},
                {id: undefined, name: 'Fresh'},
            ],
        });
    });

    it('reports a failed tag update as a toast', () => {
        renderTagList([], []);

        updateTagsOptionsRef.current?.onError?.(new Error('server said no'));

        expect(toastErrorMock).toHaveBeenCalledWith('Failed to update skill tags', {description: 'server said no'});
    });
});
