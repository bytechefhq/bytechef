import {fireEvent, render, screen, within} from '@testing-library/react';
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
    default: ({
        collapsed,
        shouldCollapse,
        src,
    }: {
        collapsed?: boolean | number;
        shouldCollapse?: (field: {src: object}) => boolean;
        src: object;
    }) => (
        <div
            data-collapsed={String(collapsed)}
            data-nested-collapsed={String(!!shouldCollapse?.({src: {}}))}
            data-root-collapsed={String(!!shouldCollapse?.({src}))}
            data-testid="json-view"
        />
    ),
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

const logEntry = (level: string, message: string) => ({
    componentName: 'aiAgent',
    componentOperationName: 'streamChat',
    level,
    message,
    taskExecutionId: '10',
    timestamp: '2026-10-09T12:00:00Z',
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

    it('hides the entries of a level that is switched off', () => {
        jobFileLogsQueryMock.mockReturnValue({
            data: {jobFileLogs: {content: [logEntry('INFO', 'info message'), logEntry('DEBUG', 'debug message')]}},
            error: undefined,
            isLoading: false,
        });

        render(<WorkflowExecutionLogsContent jobId="1" taskExecutionId="10" />);

        fireEvent.click(screen.getByRole('button', {name: 'DEBUG (1)'}));

        expect(screen.queryByText('debug message')).not.toBeInTheDocument();
        expect(screen.getByText('info message')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'DEBUG (1)'}));

        expect(screen.getByText('debug message')).toBeInTheDocument();
    });

    it('expands and collapses every JSON entry at once', () => {
        jobFileLogsQueryMock.mockReturnValue({
            data: {jobFileLogs: {content: [logEntry('INFO', '{"toolName":"TodoWrite"}')]}},
            error: undefined,
            isLoading: false,
        });

        render(<WorkflowExecutionLogsContent jobId="1" taskExecutionId="10" />);

        const jsonView = () => screen.getByTestId('json-view');

        expect(jsonView()).toHaveAttribute('data-collapsed', 'false');
        expect(jsonView()).toHaveAttribute('data-root-collapsed', 'true');
        expect(jsonView()).toHaveAttribute('data-nested-collapsed', 'false');

        fireEvent.click(screen.getByRole('button', {name: 'Expand all'}));

        expect(jsonView()).toHaveAttribute('data-collapsed', 'false');
        expect(jsonView()).toHaveAttribute('data-root-collapsed', 'false');

        fireEvent.click(screen.getByRole('button', {name: 'Collapse all'}));

        expect(jsonView()).toHaveAttribute('data-root-collapsed', 'true');
        expect(jsonView()).toHaveAttribute('data-nested-collapsed', 'false');
    });

    it('shows the expand toggle as a single button that switches between expand and collapse', () => {
        jobFileLogsQueryMock.mockReturnValue({
            data: {jobFileLogs: {content: [logEntry('INFO', '{"toolName":"TodoWrite"}')]}},
            error: undefined,
            isLoading: false,
        });

        render(<WorkflowExecutionLogsContent jobId="1" taskExecutionId="10" />);

        fireEvent.click(screen.getByRole('button', {name: 'Expand all'}));

        expect(screen.queryByRole('button', {name: 'Expand all'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Collapse all'})).toBeInTheDocument();
    });

    it('renders its toolbar into the container the tabs row provides', () => {
        jobFileLogsQueryMock.mockReturnValue({
            data: {jobFileLogs: {content: [logEntry('DEBUG', 'debug message')]}},
            error: undefined,
            isLoading: false,
        });

        const toolbarContainer = document.createElement('div');

        document.body.appendChild(toolbarContainer);

        render(<WorkflowExecutionLogsContent jobId="1" taskExecutionId="10" toolbarContainer={toolbarContainer} />);

        expect(within(toolbarContainer).getByRole('button', {name: 'DEBUG (1)'})).toBeInTheDocument();
        expect(within(toolbarContainer).getByRole('button', {name: 'Expand all'})).toBeInTheDocument();

        toolbarContainer.remove();
    });
});
