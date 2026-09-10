import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import FilterBadges from '../FilterBadges';
import {type FilterGroupI} from '../FilterMenu';

const createGroup = (
    key: string,
    label: string,
    value: string,
    onChange = vi.fn(),
    allValue = 'ALL'
): FilterGroupI => ({
    allValue,
    key,
    label,
    onChange,
    options: [
        {label: `All ${label.toLowerCase()}`, value: allValue},
        {label: 'Billing', value: '1'},
    ],
    value,
});

describe('FilterBadges', () => {
    afterEach(() => {
        resetAll();
    });

    it('renders nothing while no facet is filtered', () => {
        const {container} = render(<FilterBadges groups={[createGroup('tags', 'Tags', 'ALL')]} />);

        expect(container).toBeEmptyDOMElement();
    });

    it('names the filtered facet and its selected option', () => {
        render(<FilterBadges groups={[createGroup('tags', 'Tags', '1')]} />);

        expect(screen.getByText('Tags: Billing')).toBeInTheDocument();
    });

    it('renders a chip only for the active facets', () => {
        render(<FilterBadges groups={[createGroup('tags', 'Tags', '1'), createGroup('owners', 'Owners', 'ALL')]} />);

        expect(screen.getByText('Tags: Billing')).toBeInTheDocument();
        expect(screen.queryByText(/^Owners:/)).not.toBeInTheDocument();
    });

    it('clears one facet from its own chip', () => {
        const onChange = vi.fn();

        render(<FilterBadges groups={[createGroup('tags', 'Tags', '1', onChange)]} />);

        screen.getByLabelText('Clear tags filter').click();

        expect(onChange).toHaveBeenCalledWith('ALL');
    });

    it('clears every active facet at once', () => {
        const tagsOnChange = vi.fn();
        const ownersOnChange = vi.fn();

        render(
            <FilterBadges
                groups={[
                    createGroup('tags', 'Tags', '1', tagsOnChange),
                    createGroup('owners', 'Owners', '1', ownersOnChange),
                ]}
            />
        );

        screen.getByText('Clear all').click();

        expect(tagsOnChange).toHaveBeenCalledWith('ALL');
        expect(ownersOnChange).toHaveBeenCalledWith('ALL');
    });
});
