import {describe, it, expect, vi} from 'vitest';
import {renderHook, act} from '@testing-library/react';
import useActivationDialog from './index';
import {createRoot} from 'react-dom/client';

vi.mock('react-dom/client', () => ({
    createRoot: vi.fn(() => ({render: vi.fn(), unmount: vi.fn()})),
}));

describe('useActivationDialog', () => {
    it('renders the dialog into its own portal on open and clears it on close', async () => {
        const renderMock = vi.fn();

        vi.mocked(createRoot).mockReturnValue({render: renderMock, unmount: vi.fn()});

        const {result} = renderHook(() =>
            useActivationDialog({baseUrl: 'https://api.example.com', jwtToken: 'ey', workflowTemplateId: 'wf-1'})
        );

        expect(document.querySelectorAll('[data-bytechef-activation-dialog]')).toHaveLength(1);

        await act(async () => result.current.openDialog());

        expect(renderMock).toHaveBeenCalled();
        expect(renderMock.mock.calls[renderMock.mock.calls.length - 1][0]).not.toBeNull();

        await act(async () => result.current.closeDialog());

        expect(renderMock.mock.calls[renderMock.mock.calls.length - 1][0]).toBeNull();
    });

    it('gives each hook instance its own container and removes it on unmount', async () => {
        const renderMocks = [vi.fn(), vi.fn()];
        const containers: unknown[] = [];

        vi.mocked(createRoot).mockImplementation((container) => {
            containers.push(container);

            return {render: renderMocks[containers.length - 1], unmount: vi.fn()};
        });

        const first = renderHook(() => useActivationDialog({jwtToken: 'ey', workflowTemplateId: 'wf-1'}));
        const second = renderHook(() => useActivationDialog({jwtToken: 'ey', workflowTemplateId: 'wf-2'}));

        expect(document.querySelectorAll('[data-bytechef-activation-dialog]')).toHaveLength(2);

        await act(async () => first.result.current.openDialog());
        await act(async () => second.result.current.openDialog());

        expect(containers).toHaveLength(2);
        expect(containers[0]).not.toBe(containers[1]);
        expect(renderMocks[0].mock.calls[0][0]).not.toBeNull();
        expect(renderMocks[1].mock.calls[0][0]).not.toBeNull();

        first.unmount();
        second.unmount();

        await act(async () => {
            await Promise.resolve();
        });

        expect(document.querySelectorAll('[data-bytechef-activation-dialog]')).toHaveLength(0);
    });
});
