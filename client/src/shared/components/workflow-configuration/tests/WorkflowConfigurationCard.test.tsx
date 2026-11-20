import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowConfigurationCard from '../WorkflowConfigurationCard';

const defaultProps = {
    connectionCount: 1,
    connectionsContent: <div>connections body</div>,
    enabled: true,
    inputCount: 2,
    inputsContent: <div>inputs body</div>,
    label: 'My workflow',
    onEnabledChange: vi.fn(),
    showWorkflowToggle: true,
    switchId: 'switch-id',
};

describe('WorkflowConfigurationCard', () => {
    it('does not render a tool tab by default', () => {
        render(<WorkflowConfigurationCard {...defaultProps} />);

        expect(screen.getAllByRole('tab')).toHaveLength(2);
        expect(screen.queryByRole('tab', {name: /tool/i})).not.toBeInTheDocument();
    });

    it('renders the tool tab when tool content is provided', () => {
        render(<WorkflowConfigurationCard {...defaultProps} toolContent={<div>tool body</div>} />);

        expect(screen.getAllByRole('tab')).toHaveLength(3);
        expect(screen.getByRole('tab', {name: /tool/i})).toBeInTheDocument();
    });
    it('mounts only the active tab content by default', () => {
        render(<WorkflowConfigurationCard {...defaultProps} />);

        expect(screen.getByText('connections body')).toBeInTheDocument();
        expect(screen.queryByText('inputs body')).not.toBeInTheDocument();
    });

    it('keeps the inactive tab content mounted but hidden when asked to', () => {
        render(<WorkflowConfigurationCard {...defaultProps} forceMountContent />);

        expect(screen.getByText('inputs body').closest('[role="tabpanel"]')).toHaveClass(
            'data-[state=inactive]:hidden'
        );
        expect(screen.getByText('inputs body').closest('[role="tabpanel"]')).toHaveAttribute('data-state', 'inactive');
    });

    it('shows a badge next to the label and hides the tabs when showTabs is false', () => {
        render(<WorkflowConfigurationCard {...defaultProps} badge={<span>Provisioned</span>} showTabs={false} />);

        expect(screen.getByRole('group', {name: 'My workflow'})).toHaveTextContent('Provisioned');
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    });

    it('marks the Connections tab when a connection is missing', () => {
        render(<WorkflowConfigurationCard {...defaultProps} connectionsWarning />);

        expect(screen.getByRole('img', {name: 'Missing connection'})).toBeInTheDocument();
    });
});
