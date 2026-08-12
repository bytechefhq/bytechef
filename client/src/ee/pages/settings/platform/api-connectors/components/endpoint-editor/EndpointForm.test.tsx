import {render, resetAll, screen, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {Suspense} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import EndpointForm from './EndpointForm';

const hoisted = vi.hoisted(() => ({
    onCloseMock: vi.fn(),
    onSaveMock: vi.fn(),
}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({onChange, value}: {onChange: (value: string | undefined) => void; value: string}) => (
        <textarea data-testid="monaco-editor-mock" onChange={(event) => onChange(event.target.value)} value={value} />
    ),
}));

const renderEndpointForm = () =>
    render(
        <Suspense fallback={null}>
            <EndpointForm onClose={hoisted.onCloseMock} onSave={hoisted.onSaveMock} open />
        </Suspense>
    );

// Radix stacks two modal layers (endpoint dialog + nested dialog) and jsdom reports
// `pointer-events: none` on the inner layer; disable the userEvent guard as other dialog
// tests in this repo do (see WorkflowInputsEditDialog.test.tsx) — events still dispatch
// through the real React tree, which is what the regression is about.
const setupUser = () => userEvent.setup({pointerEventsCheck: 0});

type UserEventType = ReturnType<typeof setupUser>;

const fillRequiredEndpointFields = async (user: UserEventType) => {
    await user.type(screen.getByLabelText('Path'), '/users');
    await user.type(screen.getByLabelText('Operation ID'), 'listUsers');
};

const clickSectionAddButton = async (user: UserEventType, sectionLabel: string) => {
    const sectionHeader = screen.getByText(sectionLabel).parentElement;

    if (!sectionHeader) {
        throw new Error(`Section header for "${sectionLabel}" not found`);
    }

    await user.click(within(sectionHeader).getByRole('button', {name: 'Add'}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('EndpointForm nested dialog submits', () => {
    it('saves only the parameter and keeps the endpoint dialog open when the parameter dialog is submitted', async () => {
        const user = setupUser();

        renderEndpointForm();

        await fillRequiredEndpointFields(user);

        await clickSectionAddButton(user, 'Parameters');

        const parameterDialog = await screen.findByRole('dialog', {name: 'Add Parameter'});

        await user.type(within(parameterDialog).getByLabelText('Name'), 'userId');

        await user.click(within(parameterDialog).getByRole('button', {name: 'Add'}));

        await waitFor(() => {
            expect(screen.queryByRole('dialog', {name: 'Add Parameter'})).not.toBeInTheDocument();
        });

        expect(hoisted.onSaveMock).not.toHaveBeenCalled();
        expect(hoisted.onCloseMock).not.toHaveBeenCalled();

        expect(screen.getByText('userId')).toBeInTheDocument();
    });

    it('saves only the request body and keeps the endpoint dialog open when the request body dialog is submitted', async () => {
        const user = setupUser();

        renderEndpointForm();

        await fillRequiredEndpointFields(user);

        await clickSectionAddButton(user, 'Request Body');

        const requestBodyDialog = await screen.findByRole('dialog', {name: 'Add Request Body'});

        await user.click(within(requestBodyDialog).getByRole('button', {name: 'Add'}));

        await waitFor(() => {
            expect(screen.queryByRole('dialog', {name: 'Add Request Body'})).not.toBeInTheDocument();
        });

        expect(hoisted.onSaveMock).not.toHaveBeenCalled();
        expect(hoisted.onCloseMock).not.toHaveBeenCalled();

        expect(screen.getByText('application/json')).toBeInTheDocument();
    });

    it('saves only the response and keeps the endpoint dialog open when the response dialog is submitted', async () => {
        const user = setupUser();

        renderEndpointForm();

        await fillRequiredEndpointFields(user);

        await clickSectionAddButton(user, 'Responses');

        const responseDialog = await screen.findByRole('dialog', {name: 'Add Response'});

        await user.type(within(responseDialog).getByLabelText('Description'), 'Created');

        await user.click(within(responseDialog).getByRole('button', {name: 'Add'}));

        await waitFor(() => {
            expect(screen.queryByRole('dialog', {name: 'Add Response'})).not.toBeInTheDocument();
        });

        expect(hoisted.onSaveMock).not.toHaveBeenCalled();
        expect(hoisted.onCloseMock).not.toHaveBeenCalled();

        expect(screen.getByText('Created')).toBeInTheDocument();
    });

    it('submits the endpoint with its parameters when the endpoint dialog itself is submitted', async () => {
        const user = setupUser();

        renderEndpointForm();

        await fillRequiredEndpointFields(user);

        await clickSectionAddButton(user, 'Parameters');

        const parameterDialog = await screen.findByRole('dialog', {name: 'Add Parameter'});

        await user.type(within(parameterDialog).getByLabelText('Name'), 'userId');

        await user.click(within(parameterDialog).getByRole('button', {name: 'Add'}));

        await waitFor(() => {
            expect(screen.queryByRole('dialog', {name: 'Add Parameter'})).not.toBeInTheDocument();
        });

        const endpointDialog = screen.getByRole('dialog', {name: 'Add Endpoint'});

        const endpointSubmitButton = within(endpointDialog)
            .getAllByRole('button', {name: 'Add'})
            .find((button) => button.getAttribute('type') === 'submit');

        if (!endpointSubmitButton) {
            throw new Error('Endpoint dialog submit button not found');
        }

        await user.click(endpointSubmitButton);

        await waitFor(() => {
            expect(hoisted.onSaveMock).toHaveBeenCalledTimes(1);
        });

        expect(hoisted.onSaveMock).toHaveBeenCalledWith(
            expect.objectContaining({
                operationId: 'listUsers',
                parameters: [expect.objectContaining({in: 'query', name: 'userId'})],
                path: '/users',
            })
        );

        expect(hoisted.onCloseMock).toHaveBeenCalledTimes(1);
    });
});
