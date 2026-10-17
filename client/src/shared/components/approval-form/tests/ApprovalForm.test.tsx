import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {handleSubmitMock, searchParamsState, useApprovalFormMock} = vi.hoisted(() => ({
    handleSubmitMock: vi.fn(),
    searchParamsState: {query: 'approved=true'},
    useApprovalFormMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useSearchParams: () => [new URLSearchParams(searchParamsState.query), vi.fn()],
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
        searchParamsState.query = 'approved=true';
        handleSubmitMock.mockReset();
        useApprovalFormMock.mockReset();
    });

    it('does NOT submit on load for a one-click ?approved= link — only on the Confirm click', () => {
        useApprovalFormMock.mockReturnValue(returnFromHook({}));

        render(<ApprovalForm id="abc123" />);

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

        render(<ApprovalForm id="abc123" />);

        expect(screen.queryByLabelText('Comment (optional)')).not.toBeInTheDocument();
    });

    it('binds each one-click form comment label to its own textarea when several forms render', () => {
        useApprovalFormMock.mockReturnValue(returnFromHook({}));

        render(
            <>
                <ApprovalForm id="first" />

                <ApprovalForm id="second" />
            </>
        );

        const [firstComment, secondComment] = screen.getAllByLabelText('Comment (optional)');

        expect(firstComment.tagName).toBe('TEXTAREA');
        expect(secondComment.tagName).toBe('TEXTAREA');
        expect(firstComment).not.toBe(secondComment);
    });

    it('binds each form comment label to its own textarea when several forms with fields render', () => {
        useApprovalFormMock.mockReturnValue(
            returnFromHook({
                uiDefinition: {description: '', inputs: [{fieldName: 'amount', fieldType: 0}], title: 'Approve budget'},
            })
        );

        render(
            <>
                <ApprovalForm id="first" />

                <ApprovalForm id="second" />
            </>
        );

        const [firstComment, secondComment] = screen.getAllByLabelText('Comment (optional)');

        expect(firstComment.tagName).toBe('TEXTAREA');
        expect(secondComment.tagName).toBe('TEXTAREA');
        expect(firstComment).not.toBe(secondComment);
    });

    it('renders a submit error inline without unmounting the form', () => {
        useApprovalFormMock.mockReturnValue(
            returnFromHook({submitError: 'This approval has expired or was already resolved.'})
        );

        render(<ApprovalForm id="abc123" />);

        expect(screen.getByRole('alert')).toHaveTextContent('expired');
        expect(screen.getByRole('button', {name: 'Confirm approval'})).toBeInTheDocument();
    });

    it('shows a loading state while the form definition loads', () => {
        useApprovalFormMock.mockReturnValue(returnFromHook({loading: true}));

        render(<ApprovalForm id="abc123" />);

        expect(screen.getByRole('status')).toHaveTextContent('Loading form...');
    });

    it('confirms a one-click discard link with the trimmed comment', () => {
        searchParamsState.query = 'approved=false';

        useApprovalFormMock.mockReturnValue(returnFromHook({}));

        render(<ApprovalForm id="abc123" />);

        expect(screen.getByText('Confirm to discard this request.')).toBeInTheDocument();

        fireEvent.change(screen.getByLabelText('Comment (optional)'), {target: {value: '  wrong vendor '}});
        fireEvent.click(screen.getByRole('button', {name: 'Confirm discard'}));

        expect(handleSubmitMock).toHaveBeenCalledWith({comment: 'wrong vendor'}, false);
    });

    it('submits the form values with the comment on approve and on discard', () => {
        const formValues = {amount: '5'};

        useApprovalFormMock.mockReturnValue(
            returnFromHook({
                form: {
                    getValues: () => formValues,
                    handleSubmit: (onValid: (values: Record<string, unknown>) => void) => () => onValid(formValues),
                },
                uiDefinition: {description: '', inputs: [{fieldName: 'amount', fieldType: 0}], title: 'Approve budget'},
            })
        );

        render(<ApprovalForm id="abc123" />);

        fireEvent.change(screen.getByLabelText('Comment (optional)'), {target: {value: 'looks fine'}});
        fireEvent.click(screen.getByRole('button', {name: 'Approve'}));
        fireEvent.click(screen.getByRole('button', {name: 'Discard'}));

        expect(handleSubmitMock).toHaveBeenNthCalledWith(1, {amount: '5', comment: 'looks fine'}, true);
        expect(handleSubmitMock).toHaveBeenNthCalledWith(2, {amount: '5', comment: 'looks fine'}, false);
    });
});
