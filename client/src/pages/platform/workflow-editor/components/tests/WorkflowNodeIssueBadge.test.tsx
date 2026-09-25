import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowNodeIssueBadge from '../WorkflowNodeIssueBadge';

vi.mock('../../hooks/useNodeIssues', () => ({
    default: () => ({count: 1, severity: 'WARNING', title: 'References disabled node action_1'}),
}));

vi.mock('../../providers/workflowEditorReadOnlyContext', () => ({
    useWorkflowEditorReadOnly: () => false,
}));

describe('WorkflowNodeIssueBadge', () => {
    it('draws a warning in the warning colour meant for a neutral background', () => {
        render(<WorkflowNodeIssueBadge nodeName="action_2" />);

        const icon = screen.getByLabelText('1 issue').querySelector('svg');

        expect(icon).toHaveClass('text-content-warning-primary');
        expect(icon).not.toHaveClass('text-content-onwarning');
    });
});
