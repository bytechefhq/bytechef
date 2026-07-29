import DeployCodeWorkflowDialog from '@/ee/pages/embedded/automation-workflows/components/DeployCodeWorkflowDialog';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, test, vi} from 'vitest';

describe('DeployCodeWorkflowDialog', () => {
    test('posts the selected file and shows returned warnings', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            json: () => Promise.resolve({warnings: ['Workflow "x" has no publicly invocable trigger']}),
            ok: true,
        });

        vi.stubGlobal('fetch', fetchMock);

        const onClose = vi.fn();

        render(<DeployCodeWorkflowDialog onClose={onClose} />);

        const file = new File(['export default {}'], 'my-project.js', {type: 'text/javascript'});
        const input = screen.getByLabelText(/project file/i);

        fireEvent.change(input, {target: {files: [file]}});
        fireEvent.click(screen.getByRole('button', {name: /deploy/i}));

        await waitFor(() =>
            expect(fetchMock).toHaveBeenCalledWith(
                '/api/embedded/internal/automation/projects/deploy',
                expect.objectContaining({method: 'POST'})
            )
        );

        const [, requestInit] = fetchMock.mock.calls[0];
        const body = requestInit.body as FormData;
        const uploadedFile = body.get('projectFile') as File;

        expect(uploadedFile).toBeInstanceOf(File);
        expect(uploadedFile.name).toBe('my-project.js');
        expect(await uploadedFile.text()).toBe('export default {}');

        expect(await screen.findByText(/no publicly invocable trigger/i)).toBeInTheDocument();
    });
});
