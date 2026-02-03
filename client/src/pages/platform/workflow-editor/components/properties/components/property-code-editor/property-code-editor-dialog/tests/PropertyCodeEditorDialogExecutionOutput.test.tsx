import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogExecutionOutput from '../PropertyCodeEditorDialogExecutionOutput';

const hoisted = vi.hoisted(() => {
    return {
        storeState: {
            scriptIsRunning: false,
            scriptTestExecution: undefined as
                | {
                      error?: {message: string};
                      output?: object | string;
                  }
                | undefined,
        },
    };
});

vi.mock('../stores/usePropertyCodeEditorDialogStore', () => ({
    usePropertyCodeEditorDialogStore: (selector: (state: unknown) => unknown) =>
        selector({
            scriptIsRunning: hoisted.storeState.scriptIsRunning,
            scriptTestExecution: hoisted.storeState.scriptTestExecution,
        }),
}));

vi.mock('react-json-view', () => ({
    default: ({src}: {src: object}) => <div data-testid="react-json-view">{JSON.stringify(src)}</div>,
}));

describe('PropertyCodeEditorDialogExecutionOutput', () => {
    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.scriptIsRunning = false;
        hoisted.storeState.scriptTestExecution = undefined;
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('when script is running', () => {
        it('should display running message', () => {
            hoisted.storeState.scriptIsRunning = true;

            render(<PropertyCodeEditorDialogExecutionOutput />);

            expect(screen.getByText('Script is running...')).toBeInTheDocument();
        });
    });

    describe('when script has not been executed', () => {
        it('should display not executed message', () => {
            hoisted.storeState.scriptIsRunning = false;
            hoisted.storeState.scriptTestExecution = undefined;

            render(<PropertyCodeEditorDialogExecutionOutput />);

            expect(screen.getByText('The script has not yet been executed.')).toBeInTheDocument();
        });
    });

    describe('when script has object output', () => {
        it('should render ReactJson component with output', async () => {
            hoisted.storeState.scriptIsRunning = false;
            hoisted.storeState.scriptTestExecution = {
                output: {key: 'value', nested: {data: 123}},
            };

            render(<PropertyCodeEditorDialogExecutionOutput />);

            // Wait for Suspense to resolve
            const jsonView = await screen.findByTestId('react-json-view');

            expect(jsonView).toBeInTheDocument();
            expect(jsonView).toHaveTextContent('key');
            expect(jsonView).toHaveTextContent('value');
        });
    });

    describe('when script has string output', () => {
        it('should render output as text', () => {
            hoisted.storeState.scriptIsRunning = false;
            hoisted.storeState.scriptTestExecution = {
                output: 'Hello, World!' as unknown as object,
            };

            render(<PropertyCodeEditorDialogExecutionOutput />);

            expect(screen.getByText('Hello, World!')).toBeInTheDocument();
        });
    });

    describe('when script has error', () => {
        it('should display error message', () => {
            hoisted.storeState.scriptIsRunning = false;
            hoisted.storeState.scriptTestExecution = {
                error: {message: 'Something went wrong'},
            };

            render(<PropertyCodeEditorDialogExecutionOutput />);

            expect(screen.getByText('Error')).toBeInTheDocument();
            expect(screen.getByText('Something went wrong')).toBeInTheDocument();
        });
    });

    describe('when script has no output and no error', () => {
        it('should display no defined output message', () => {
            hoisted.storeState.scriptIsRunning = false;
            hoisted.storeState.scriptTestExecution = {};

            render(<PropertyCodeEditorDialogExecutionOutput />);

            expect(screen.getByText('No defined output.')).toBeInTheDocument();
        });
    });
});
