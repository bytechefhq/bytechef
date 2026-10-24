import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {type WorkflowTestNodeStateI} from '../../stores/useWorkflowEditorStore';
import WorkflowTestStatusLegend from '../WorkflowTestStatusLegend';

const hoisted = vi.hoisted(() => ({
    nodeStates: {} as Record<string, WorkflowTestNodeStateI>,
}));

vi.mock('../../hooks/useWorkflowTestNodeStates', () => ({
    default: () => hoisted.nodeStates,
}));

describe('WorkflowTestStatusLegend', () => {
    beforeEach(() => {
        hoisted.nodeStates = {};
    });

    it('renders nothing when the canvas shows no run states', () => {
        const {container} = render(<WorkflowTestStatusLegend />);

        expect(container).toBeEmptyDOMElement();
    });

    it('lists only the states on the canvas, in a fixed order', () => {
        hoisted.nodeStates = {
            aiAgent_1: {status: 'COMPLETED'},
            keywords_1: {clusterElement: true, status: 'BLOCKED'},
            openAi_3: {clusterElement: true, status: 'FAILED'},
            pii_1: {clusterElement: true, status: 'REDACTED'},
            sanitizeText_1: {clusterElement: true, status: 'REDACTED'},
        };

        render(<WorkflowTestStatusLegend />);

        const labels = screen.getAllByTestId('workflow-test-status-legend-item').map((item) => item.textContent);

        expect(labels).toEqual(['Completed', 'Failed', 'Blocked', 'Redacted']);
    });

    it('names each state and uses its canvas colour', () => {
        hoisted.nodeStates = {
            checkForViolations_1: {clusterElement: true, status: 'WITHHELD'},
            var_1: {status: 'RUNNING'},
        };

        render(<WorkflowTestStatusLegend />);

        expect(screen.getByText('Running').previousElementSibling).toHaveClass('bg-blue-500');
        expect(screen.getByText('Withheld').previousElementSibling).toHaveClass('bg-amber-500');
    });
});
