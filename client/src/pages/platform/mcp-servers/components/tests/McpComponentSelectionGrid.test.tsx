import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import McpComponentSelectionGrid from '../McpComponentSelectionGrid';

const defaultProps = {
    components: [],
    isLoading: false,
    onComponentSelect: vi.fn(),
    onSearchTermChange: vi.fn(),
    searchTerm: '',
};

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpComponentSelectionGrid', () => {
    it('should render the components', () => {
        render(
            <McpComponentSelectionGrid
                {...defaultProps}
                components={[{description: 'Team chat', name: 'slack', title: 'Slack', version: 1}]}
            />
        );

        expect(screen.getByText('Slack')).toBeInTheDocument();
        expect(screen.queryByText('No components found')).not.toBeInTheDocument();
    });

    it('should name the search term when it matches no component', () => {
        render(<McpComponentSelectionGrid {...defaultProps} searchTerm="eeee" />);

        expect(screen.getByText('No components found')).toBeInTheDocument();
        expect(screen.getByText('No components match "eeee". Try a different search term.')).toBeInTheDocument();
    });

    it('should explain an empty list without a search term', () => {
        render(<McpComponentSelectionGrid {...defaultProps} />);

        expect(screen.getByText('There are no components with tools available.')).toBeInTheDocument();
    });

    it('should not show the empty state while loading', () => {
        render(<McpComponentSelectionGrid {...defaultProps} isLoading />);

        expect(screen.queryByText('No components found')).not.toBeInTheDocument();
    });
});
