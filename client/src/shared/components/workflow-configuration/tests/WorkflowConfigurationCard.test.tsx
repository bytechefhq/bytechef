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
});
