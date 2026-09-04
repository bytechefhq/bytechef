import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowExecutionContent from '../WorkflowExecutionContent';

vi.mock('@/shared/components/JsonView', () => ({
    default: () => <div data-testid="json-view" />,
}));

describe('WorkflowExecutionContent', () => {
    it('says so when a trigger ran with no input rather than rendering an empty viewer', () => {
        render(<WorkflowExecutionContent input={{}} />);

        expect(screen.getByText('No input data')).toBeInTheDocument();
        expect(screen.queryByTestId('json-view')).not.toBeInTheDocument();
    });

    it('renders the input viewer once the trigger has input', () => {
        render(<WorkflowExecutionContent input={{orderId: '42'}} />);

        expect(screen.getByTestId('json-view')).toBeInTheDocument();
        expect(screen.queryByText('No input data')).not.toBeInTheDocument();
    });

    it('says so when an execution produced no output', () => {
        render(<WorkflowExecutionContent output={{}} />);

        expect(screen.getByText('No output data')).toBeInTheDocument();
        expect(screen.queryByTestId('json-view')).not.toBeInTheDocument();
    });

    it('renders the output viewer once the execution has output', () => {
        render(<WorkflowExecutionContent output={{status: 'ok'}} />);

        expect(screen.getByTestId('json-view')).toBeInTheDocument();
        expect(screen.queryByText('No output data')).not.toBeInTheDocument();
    });

    it('shows the failure message of a failed trigger', () => {
        render(<WorkflowExecutionContent error={{message: 'Signature check failed', stackTrace: []}} />);

        expect(screen.getByText('Signature check failed')).toBeInTheDocument();
    });
});
