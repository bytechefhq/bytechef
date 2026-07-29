import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {handleSubmitMock, useApprovalFormMock} = vi.hoisted(() => ({
    handleSubmitMock: vi.fn(),
    useApprovalFormMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useSearchParams: () => [new URLSearchParams('approved=true'), vi.fn()],
}));

vi.mock('@/shared/hooks/useApprovalForm', () => ({
    default: useApprovalFormMock,
}));

vi.mock('@/components/ui/form', () => ({
    Form: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/components/form/renderFormField', () => ({
    renderFormField: (_form: unknown, _input: unknown, name: string) => <div data-testid={`field-${name}`} />,
}));

import ApprovalForm from '../ApprovalForm';

const formStub = {
    getValues: () => ({}),
    handleSubmit: (onValid: (values: Record<string, unknown>) => void) => () => onValid({}),
};

const returnFromHook = (overrides: Record<string, unknown>) => ({
    approved: null,
    definition: {},
    error: null,
    form: formStub,
    handleSubmit: handleSubmitMock,
    loading: false,
    submitError: null,
    submitted: false,
    submitting: false,
    uiDefinition: {description: '', inputs: [], title: 'Approve budget'},
    ...overrides,
});

describe('ApprovalForm', () => {
    beforeEach(() => {
        handleSubmitMock.mockReset();
        useApprovalFormMock.mockReset();
    });

    it('does NOT submit on load for a one-click ?approved= link — only on the Confirm click', () => {
        useApprovalFormMock.mockReturnValue(returnFromHook({}));

        render(<ApprovalForm id="abc123" />);

        // Security-critical: link scanners/preview bots follow the URL, so an on-load submit would resolve the
        // approval without a human. The preset only pre-selects a Confirm view.
        expect(handleSubmitMock).not.toHaveBeenCalled();

        const confirmButton = screen.getByRole('button', {name: 'Confirm approval'});

        fireEvent.click(confirmButton);

        expect(handleSubmitMock).toHaveBeenCalledTimes(1);
        expect(handleSubmitMock.mock.calls[0][1]).toBe(true);
    });

    it('ignores the ?approved= shortcut when the approval has form fields', () => {
        useApprovalFormMock.mockReturnValue(
            returnFromHook({
                uiDefinition: {description: '', inputs: [{fieldName: 'amount', fieldType: 0}], title: 'Approve budget'},
            })
        );

        render(<ApprovalForm id="abc123" />);

        expect(screen.queryByRole('button', {name: 'Confirm approval'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Approve'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Discard'})).toBeInTheDocument();
        expect(handleSubmitMock).not.toHaveBeenCalled();
    });

    it('suppresses the built-in comment box when a user-defined "comment" field exists', () => {
        useApprovalFormMock.mockReturnValue(
            returnFromHook({
                uiDefinition: {
                    description: '',
                    inputs: [{fieldName: 'comment', fieldType: 0}],
                    title: 'Approve budget',
                },
            })
        );

        const {container} = render(<ApprovalForm id="abc123" />);

        expect(container.querySelector('#approval-comment')).toBeNull();
    });

    it('renders a submit error inline without unmounting the form', () => {
        useApprovalFormMock.mockReturnValue(
            returnFromHook({submitError: 'This approval has expired or was already resolved.'})
        );

        render(<ApprovalForm id="abc123" />);

        expect(screen.getByRole('alert')).toHaveTextContent('expired');
        // The Confirm button is still there — the error did not replace the form.
        expect(screen.getByRole('button', {name: 'Confirm approval'})).toBeInTheDocument();
    });
});
