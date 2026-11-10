import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import WorkflowExecutionContent from '../WorkflowExecutionContent';

const {copyToClipboardMock} = vi.hoisted(() => ({copyToClipboardMock: vi.fn()}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, copyToClipboardMock],
}));

vi.mock('@/shared/components/JsonView', () => ({
    default: () => <div data-testid="json-view" />,
}));

describe('WorkflowExecutionContent', () => {
    it('copies a failure message with its stack trace as plain text when asked to offer a copy button', async () => {
        render(
            <WorkflowExecutionContent
                error={{message: 'Unable to perform oauth token refresh', stackTrace: ['at A.run(A.java:1)']}}
                showErrorClipboardButton
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Copy to clipboard'}));

        expect(copyToClipboardMock).toHaveBeenCalledWith('Unable to perform oauth token refresh\nat A.run(A.java:1)');
    });

    it('offers no copy button for a failure unless asked to', () => {
        render(<WorkflowExecutionContent error={{message: 'failed', stackTrace: []}} />);

        expect(screen.queryByRole('button', {name: 'Copy to clipboard'})).not.toBeInTheDocument();
    });

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

    it('wraps a long single-line string output instead of scrolling it sideways', () => {
        const longHtml =
            '<!DOCTYPE html> <html lang="hr"> <head> <meta name="viewport" content="width=device-width,height=device-height"> </head> </html>';

        render(<WorkflowExecutionContent output={longHtml as unknown as object} />);

        const rendered = screen.getByText(longHtml);

        expect(rendered).toHaveClass('whitespace-pre-wrap');
        expect(rendered).not.toHaveClass('text-nowrap');
        expect(rendered.parentElement).not.toHaveClass('text-nowrap');
    });

    it('shows the failure message of a failed trigger', () => {
        render(<WorkflowExecutionContent error={{message: 'Signature check failed', stackTrace: []}} />);

        expect(screen.getByText('Signature check failed')).toBeInTheDocument();
    });
});
