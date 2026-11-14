import ImportCodeWorkflowDialog from '@/pages/platform/code-workflow/components/ImportCodeWorkflowDialog';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, test, vi} from 'vitest';

const renderDialog = (overrides: Partial<Parameters<typeof ImportCodeWorkflowDialog>[0]> = {}) => {
    const onClose = vi.fn();
    const onImported = vi.fn();

    render(
        <ImportCodeWorkflowDialog
            deployUrl="/api/test/deploy"
            description="Upload a built code project."
            fileFieldName="projectFile"
            onClose={onClose}
            onImported={onImported}
            title="Import Code Project"
            {...overrides}
        />
    );

    return {onClose, onImported};
};

const selectFile = (name: string, content = 'export default {}') => {
    const file = new File([content], name);

    fireEvent.change(screen.getByLabelText(/code file/i), {target: {files: [file]}});

    return file;
};

describe('ImportCodeWorkflowDialog', () => {
    beforeEach(() => {
        applicationInfoStore.setState({workflow: {codeWorkflow: {javaEnabled: true}}});
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    test('posts the form fields and the selected file, then closes when the server returns no content', async () => {
        const fetchMock = vi.fn().mockResolvedValue({ok: true, status: 204});

        vi.stubGlobal('fetch', fetchMock);

        const {onClose, onImported} = renderDialog({formFields: {workspaceId: '7'}});

        selectFile('my-project.js');

        fireEvent.click(screen.getByRole('button', {name: 'Import'}));

        await waitFor(() => expect(onClose).toHaveBeenCalled());

        expect(fetchMock).toHaveBeenCalledWith('/api/test/deploy', expect.objectContaining({method: 'POST'}));

        const body = fetchMock.mock.calls[0][1].body as FormData;
        const uploadedFile = body.get('projectFile') as File;

        expect(body.get('workspaceId')).toBe('7');
        expect(uploadedFile.name).toBe('my-project.js');
        expect(await uploadedFile.text()).toBe('export default {}');
        expect(onImported).toHaveBeenCalled();
    });

    test('keeps the dialog open to show returned warnings', async () => {
        vi.stubGlobal(
            'fetch',
            vi.fn().mockResolvedValue({
                json: () => Promise.resolve({warnings: ['Workflow "x" has no publicly invocable trigger']}),
                ok: true,
                status: 200,
            })
        );

        const {onClose, onImported} = renderDialog();

        selectFile('my-project.py');

        fireEvent.click(screen.getByRole('button', {name: 'Import'}));

        expect(await screen.findByText(/no publicly invocable trigger/i)).toBeInTheDocument();
        expect(onImported).toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Done'}));

        expect(onClose).toHaveBeenCalled();
    });

    test('shows the error and stays open when the server rejects the file', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok: false, status: 400, statusText: 'Bad Request'}));

        const {onClose, onImported} = renderDialog();

        selectFile('my-project.js');

        fireEvent.click(screen.getByRole('button', {name: 'Import'}));

        expect(await screen.findByText('Import failed: Bad Request')).toBeInTheDocument();
        expect(onClose).not.toHaveBeenCalled();
        expect(onImported).not.toHaveBeenCalled();
    });

    test('accepts JAR files when Java is enabled', () => {
        renderDialog();

        expect(screen.getByLabelText(/code file/i)).toHaveAttribute('accept', '.jar,.js,.py');
        expect(screen.getByText('JAR, JS, PY files')).toBeInTheDocument();

        selectFile('my-project.jar');

        expect(screen.getByRole('button', {name: 'Import'})).toBeEnabled();
    });

    test('hides and rejects JAR files when Java is disabled', () => {
        applicationInfoStore.setState({workflow: {codeWorkflow: {javaEnabled: false}}});

        renderDialog();

        expect(screen.getByLabelText(/code file/i)).toHaveAttribute('accept', '.js,.py');
        expect(screen.getByText('JS, PY files')).toBeInTheDocument();

        selectFile('my-project.jar');

        expect(screen.getByText('Java code workflows are disabled on this instance.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Import'})).toBeDisabled();
    });
});
