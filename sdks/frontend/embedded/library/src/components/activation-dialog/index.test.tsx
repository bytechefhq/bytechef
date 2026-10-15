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

        expect(document.getElementById('activation-dialog-portal')).not.toBeNull();

        await act(async () => result.current.openDialog());

        expect(renderMock).toHaveBeenCalled();
        expect(renderMock.mock.calls[renderMock.mock.calls.length - 1][0]).not.toBeNull();

        await act(async () => result.current.closeDialog());

        expect(renderMock.mock.calls[renderMock.mock.calls.length - 1][0]).toBeNull();
    });
});
