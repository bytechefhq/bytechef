import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {fireEvent, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, describe, expect, test, vi} from 'vitest';

import UploadCustomComponentDialog from './UploadCustomComponentDialog';

afterEach(() => {
    resetAll();
});

describe('UploadCustomComponentDialog', () => {
    test('posts the selected file to the session-authenticated internal deploy endpoint', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            statusText: 'No Content',
        });

        vi.stubGlobal('fetch', fetchMock);

        const user = userEvent.setup();

        render(<UploadCustomComponentDialog />);

        await user.click(screen.getByRole('button', {name: /import component/i}));

        const file = new File(['export default {}'], 'my-component.js', {type: 'text/javascript'});
        const input = document.getElementById('component-file-upload') as HTMLInputElement;

        fireEvent.change(input, {target: {files: [file]}});

        await user.click(screen.getByRole('button', {name: /^import$/i}));

        await waitFor(() =>
            expect(fetchMock).toHaveBeenCalledWith(
                '/api/platform/internal/custom-components/deploy',
                expect.objectContaining({method: 'POST'})
            )
        );

        const [, requestInit] = fetchMock.mock.calls[0];
        const body = requestInit.body as FormData;
        const uploadedFile = body.get('componentFile') as File;

        expect(uploadedFile).toBeInstanceOf(File);
        expect(uploadedFile.name).toBe('my-component.js');
        expect(await uploadedFile.text()).toBe('export default {}');
    });
    test('adds dropped files with an accepted extension and ignores the others', async () => {
        const user = userEvent.setup();

        render(<UploadCustomComponentDialog />);

        await user.click(screen.getByRole('button', {name: /import component/i}));

        const dropZone = screen.getByTestId('component-file-drop-zone');

        const dataTransfer = {
            dropEffect: 'none',
            files: [
                new File(['export default {}'], 'dropped.js', {type: 'text/javascript'}),
                new File(['x = 1'], 'dropped.py', {type: 'text/x-python'}),
                new File(['jar'], 'component.jar', {type: 'application/java-archive'}),
                new File(['notes'], 'notes.txt', {type: 'text/plain'}),
            ],
            types: ['Files'],
        };

        fireEvent.dragOver(dropZone, {dataTransfer});

        expect(dataTransfer.dropEffect).toBe('copy');

        fireEvent.drop(dropZone, {dataTransfer});

        expect(screen.getByText('Selected Files (3)')).toBeInTheDocument();
        expect(screen.getByText('dropped.js')).toBeInTheDocument();
        expect(screen.getByText('dropped.py')).toBeInTheDocument();
        expect(screen.getByText('component.jar')).toBeInTheDocument();
        expect(screen.queryByText('notes.txt')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: /^import$/i})).toBeEnabled();
    });

    test('ignores a dropped jar when Java custom components are disabled', async () => {
        const initialComponent = applicationInfoStore.getState().component;

        applicationInfoStore.setState({
            component: {
                ...initialComponent,
                customComponent: {...initialComponent.customComponent, javaEnabled: false},
            },
        });

        const user = userEvent.setup();

        render(<UploadCustomComponentDialog />);

        await user.click(screen.getByRole('button', {name: /import component/i}));

        fireEvent.drop(screen.getByTestId('component-file-drop-zone'), {
            dataTransfer: {
                files: [
                    new File(['export default {}'], 'dropped.js', {type: 'text/javascript'}),
                    new File(['jar'], 'component.jar', {type: 'application/java-archive'}),
                ],
                types: ['Files'],
            },
        });

        expect(screen.getByText('Selected Files (1)')).toBeInTheDocument();
        expect(screen.queryByText('component.jar')).not.toBeInTheDocument();

        applicationInfoStore.setState({component: initialComponent});
    });
});
