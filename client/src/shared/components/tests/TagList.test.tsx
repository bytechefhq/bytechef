import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import TagList from '../TagList';

const defaultProps = {
    getRequest: (id: number, tags: Array<{name: string}>) => ({id, tags}),
    id: 1,
    remainingTags: [{id: 3, name: 'marketing'}],
    tags: [
        {id: 1, name: 'sales'},
        {id: 2, name: 'support'},
    ],
    updateTagsMutation: {mutate: vi.fn()},
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('TagList', () => {
    it('offers adding and removing tags by default', () => {
        render(
            <TooltipProvider>
                <TagList {...defaultProps} />
            </TooltipProvider>
        );

        expect(screen.getByText('sales')).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Remove tag'})).toHaveLength(2);
        expect(screen.getByRole('button', {name: 'Add new tag'})).toBeInTheDocument();
    });

    it('shows the tags without add or remove controls when read-only', () => {
        render(
            <TooltipProvider>
                <TagList {...defaultProps} readOnly />
            </TooltipProvider>
        );

        expect(screen.getByText('sales')).toBeInTheDocument();
        expect(screen.getByText('support')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Remove tag'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add new tag'})).not.toBeInTheDocument();
    });
});
