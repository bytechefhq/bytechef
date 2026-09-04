import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowExecutionLogsContent from '../WorkflowExecutionLogsContent';

const {editorJobFileLogsQueryMock, jobFileLogsQueryMock, triggerExecutionFileLogsQueryMock} = vi.hoisted(() => ({
    editorJobFileLogsQueryMock: vi.fn(),
    jobFileLogsQueryMock: vi.fn(),
    triggerExecutionFileLogsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    LogLevel: {
        Debug: 'DEBUG',
        Error: 'ERROR',
        Info: 'INFO',
        Trace: 'TRACE',
        Warn: 'WARN',
    },
    useEditorJobFileLogsQuery: editorJobFileLogsQueryMock,
    useJobFileLogsQuery: jobFileLogsQueryMock,
    useTriggerExecutionFileLogsQuery: triggerExecutionFileLogsQueryMock,
}));

vi.mock('@/shared/components/JsonView', () => ({
    default: () => <div data-testid="json-view" />,
}));

const idleQuery = {data: undefined, error: undefined, isLoading: false};

const logPage = (message: string, componentName: string) => ({
    content: [
        {
            componentName,
            componentOperationName: 'newRequest',
            level: 'INFO',
            message,
            taskExecutionId: '77',
            timestamp: '2026-09-04T10:00:00Z',
            triggerExecutionId: '77',
        },
    ],
});

describe('WorkflowExecutionLogsContent', () => {
    beforeEach(() => {
        editorJobFileLogsQueryMock.mockReturnValue(idleQuery);
        jobFileLogsQueryMock.mockReturnValue(idleQuery);
        triggerExecutionFileLogsQueryMock.mockReturnValue(idleQuery);
    });

    it('reads the trigger execution logs when the trigger item is selected', () => {
        triggerExecutionFileLogsQueryMock.mockReturnValue({
            data: {triggerExecutionFileLogs: logPage('request received', 'webhook')},
            error: undefined,
            isLoading: false,
        });

        render(<WorkflowExecutionLogsContent jobId="1" triggerExecutionId="77" />);

        expect(screen.getByText('request received')).toBeInTheDocument();

        expect(triggerExecutionFileLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({triggerExecutionId: '77'}),
            {enabled: true}
        );
        expect(jobFileLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(editorJobFileLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
    });

    it('does not repeat the component name on trigger logs, which all come from one trigger', () => {
        triggerExecutionFileLogsQueryMock.mockReturnValue({
            data: {triggerExecutionFileLogs: logPage('request received', 'webhook')},
            error: undefined,
            isLoading: false,
        });

        render(<WorkflowExecutionLogsContent jobId="1" triggerExecutionId="77" />);

        expect(screen.queryByText('webhook')).not.toBeInTheDocument();
    });

    it('keeps reading the job logs when a task is selected', () => {
        jobFileLogsQueryMock.mockReturnValue({
            data: {jobFileLogs: logPage('task entry', 'httpClient')},
            error: undefined,
            isLoading: false,
        });

        render(<WorkflowExecutionLogsContent jobId="1" taskExecutionId="10" />);

        expect(screen.getByText('task entry')).toBeInTheDocument();

        expect(jobFileLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({filter: {taskExecutionId: '10'}, jobId: '1'}),
            {enabled: true}
        );
        expect(triggerExecutionFileLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
    });

    it('never asks for trigger logs in the editor, where a trigger is not executed', () => {
        editorJobFileLogsQueryMock.mockReturnValue({
            data: {editorJobFileLogs: logPage('editor entry', 'logger')},
            error: undefined,
            isLoading: false,
        });

        render(<WorkflowExecutionLogsContent isEditorEnvironment jobId="1" triggerExecutionId="77" />);

        expect(screen.getByText('editor entry')).toBeInTheDocument();

        expect(triggerExecutionFileLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(editorJobFileLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: true});
    });
});
