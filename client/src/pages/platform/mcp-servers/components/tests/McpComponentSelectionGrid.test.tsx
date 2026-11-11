import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentSelectionGrid from '../McpComponentSelectionGrid';

const defaultProps = {
    components: [],
    isLoading: false,
    onComponentSelect: vi.fn(),
    onSearchTermChange: vi.fn(),
    searchTerm: '',
};

beforeEach(() => {
    windowResizeObserver();
});

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

    it('should render components that share a name and version by their id', async () => {
        const onComponentSelect = vi.fn();
        const codeComponent = {id: '6', name: 'googleMail', title: 'Gmail Code', version: 1};

        render(
            <McpComponentSelectionGrid
                {...defaultProps}
                components={[{id: '5', name: 'googleMail', title: 'Gmail', version: 1}, codeComponent]}
                onComponentSelect={onComponentSelect}
            />
        );

        expect(screen.getByText('Gmail')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Gmail Code'));

        expect(onComponentSelect).toHaveBeenCalledWith(codeComponent);
    });

    it('should show an added component as disabled and ignore clicks on it', async () => {
        const onComponentSelect = vi.fn();
        const gmail = {name: 'googleMail', title: 'Gmail', version: 1};
        const slack = {name: 'slack', title: 'Slack', version: 1};

        render(
            <McpComponentSelectionGrid
                {...defaultProps}
                components={[gmail, slack]}
                isComponentAdded={(component) => component.name === 'googleMail'}
                onComponentSelect={onComponentSelect}
            />
        );

        expect(screen.getByText('Already added')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Gmail'));

        expect(onComponentSelect).not.toHaveBeenCalled();

        await userEvent.click(screen.getByText('Slack'));

        expect(onComponentSelect).toHaveBeenCalledWith(slack);
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

    it('should show skeleton component cards while loading', () => {
        render(<McpComponentSelectionGrid {...defaultProps} isLoading />);

        expect(screen.getAllByTestId('mcp-component-card-skeleton')).toHaveLength(6);
    });

    it('should show no skeleton component cards once loaded', () => {
        render(
            <McpComponentSelectionGrid
                {...defaultProps}
                components={[{description: 'Team chat', name: 'slack', title: 'Slack', version: 1}]}
            />
        );

        expect(screen.queryByTestId('mcp-component-card-skeleton')).not.toBeInTheDocument();
    });
});
