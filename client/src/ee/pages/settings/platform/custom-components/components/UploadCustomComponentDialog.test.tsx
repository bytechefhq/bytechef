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
});
