import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CopilotGenerateDescriptionButton from './CopilotGenerateDescriptionButton';

const {generateMock, toastErrorMock} = vi.hoisted(() => ({generateMock: vi.fn(), toastErrorMock: vi.fn()}));

vi.mock('sonner', () => ({
    toast: {error: toastErrorMock},
}));

vi.mock('./useGenerateWorkflowDescription', () => ({
    useGenerateWorkflowDescription: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({ai: {copilot: {enabled: true}}}),
}));

describe('CopilotGenerateDescriptionButton', () => {
    beforeEach(() => {
        generateMock.mockReset();
        toastErrorMock.mockReset();

        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('generates and applies the value', async () => {
        const onApply = vi.fn();

        generateMock.mockResolvedValue({value: 'Syncs records nightly.'});

        render(<CopilotGenerateDescriptionButton environmentId={1} onApply={onApply} workflowId="wf1" />);

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await vi.waitFor(() => expect(onApply).toHaveBeenCalledWith('Syncs records nightly.'));
        expect(generateMock).toHaveBeenCalledWith({environmentId: 1, workflowId: 'wf1', workflowNodeName: undefined});
    });

    it('shows a toast when the request never reaches the server', async () => {
        generateMock.mockRejectedValue(new TypeError('Failed to fetch'));

        render(<CopilotGenerateDescriptionButton environmentId={1} onApply={vi.fn()} workflowId="wf1" />);

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await vi.waitFor(() =>
            expect(toastErrorMock).toHaveBeenCalledWith('Failed to generate the description: Failed to fetch')
        );
    });

    it('leaves server errors to the global fetch-interceptor toast', async () => {
        generateMock.mockRejectedValue(new Error('Internal Server Error'));

        render(<CopilotGenerateDescriptionButton environmentId={1} onApply={vi.fn()} workflowId="wf1" />);

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await vi.waitFor(() => expect(generateMock).toHaveBeenCalled());

        expect(toastErrorMock).not.toHaveBeenCalled();
    });

    it.each([[''], ['   ']])('does not apply a blank value %j and shows a toast instead', async (value) => {
        const onApply = vi.fn();

        generateMock.mockResolvedValue({value});

        render(<CopilotGenerateDescriptionButton environmentId={1} onApply={onApply} workflowId="wf1" />);

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await vi.waitFor(() => expect(toastErrorMock).toHaveBeenCalledWith('Copilot returned an empty description.'));
        expect(onApply).not.toHaveBeenCalled();
    });

    it('shows a toast when applying the generated value throws', async () => {
        const onApply = vi.fn(() => {
            throw new Error('Form is unmounted');
        });

        generateMock.mockResolvedValue({value: 'Syncs records nightly.'});

        render(<CopilotGenerateDescriptionButton environmentId={1} onApply={onApply} workflowId="wf1" />);

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await vi.waitFor(() =>
            expect(toastErrorMock).toHaveBeenCalledWith('Failed to apply the generated description: Form is unmounted')
        );
    });

    it('renders nothing when workflowId is undefined', () => {
        const {container} = render(
            <CopilotGenerateDescriptionButton environmentId={1} onApply={vi.fn()} workflowId={undefined} />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
