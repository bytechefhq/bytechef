import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionTab from './ConnectionTab';

const mockSetShowConnectionNote = vi.fn();
const mockUseConnectionNoteStore = vi.fn();

vi.mock('../../../stores/useConnectionNoteStore', () => ({
    useConnectionNoteStore: (selector: unknown) => mockUseConnectionNoteStore(selector),
}));

vi.mock('zustand/react/shallow', () => ({
    useShallow: (selector: unknown) => selector,
}));

vi.mock(
    '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTabConnectionFieldset',
    () => ({
        default: ({
            componentConnection,
            workflowTestConfigurationConnection,
        }: {
            componentConnection: {key: string};
            workflowTestConfigurationConnection?: {workflowConnectionKey: string};
        }) => (
            <div
                data-matched-config={workflowTestConfigurationConnection?.workflowConnectionKey ?? 'none'}
                data-testid={`fieldset-${componentConnection.key}`}
            >
                ConnectionTabConnectionFieldset
            </div>
        ),
    })
);

describe('ConnectionTab', () => {
    const defaultComponentConnections = [
        {
            componentName: 'test-component',
            componentVersion: 1,
            key: 'conn_1',
            required: true,
            workflowNodeName: 'node-1',
        },
    ];

    const defaultProps = {
        componentConnections: defaultComponentConnections,
        workflowId: 'workflow-1',
        workflowNodeName: 'node-1',
    };

    beforeEach(() => {
        vi.clearAllMocks();

        mockUseConnectionNoteStore.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({
                setShowConnectionNote: mockSetShowConnectionNote,
                showConnectionNote: true,
            })
        );
    });

    it('renders with default p-4 class when no className prop is provided', () => {
        const {container} = render(<ConnectionTab {...defaultProps} />);

        const wrapper = container.firstElementChild as HTMLElement;

        expect(wrapper.className).toContain('p-4');
    });

    it('merges custom className with default classes via twMerge', () => {
        const {container} = render(<ConnectionTab {...defaultProps} className="p-0" />);

        const wrapper = container.firstElementChild as HTMLElement;

        expect(wrapper.className).toContain('p-0');
        expect(wrapper.className).not.toContain('p-4');
    });

    it('renders ConnectionTabConnectionFieldset for each componentConnection', () => {
        const multipleConnections = [
            {
                componentName: 'comp-a',
                componentVersion: 1,
                key: 'conn_1',
                required: true,
                workflowNodeName: 'node-1',
            },
            {
                componentName: 'comp-b',
                componentVersion: 1,
                key: 'conn_2',
                required: false,
                workflowNodeName: 'node-1',
            },
        ];

        render(<ConnectionTab {...defaultProps} componentConnections={multipleConnections} />);

        expect(screen.getByTestId('fieldset-conn_1')).toBeInTheDocument();
        expect(screen.getByTestId('fieldset-conn_2')).toBeInTheDocument();
    });

    it('matches test configuration connections by workflowConnectionKey', () => {
        const workflowTestConfigurationConnections = [
            {
                connectionId: 10,
                workflowConnectionKey: 'conn_1',
                workflowNodeName: 'node-1',
            },
        ];

        render(
            <ConnectionTab
                {...defaultProps}
                workflowTestConfigurationConnections={workflowTestConfigurationConnections}
            />
        );

        const fieldset = screen.getByTestId('fieldset-conn_1');

        expect(fieldset.getAttribute('data-matched-config')).toBe('conn_1');
    });

    it('shows connection note when showConnectionNote is true', () => {
        render(<ConnectionTab {...defaultProps} />);

        expect(screen.getByText('Note')).toBeInTheDocument();
        expect(screen.getByText('The selected connections are used for testing purposes only.')).toBeInTheDocument();
    });

    it('hides connection note when showConnectionNote is false', () => {
        mockUseConnectionNoteStore.mockImplementation((selector: (state: unknown) => unknown) =>
            selector({
                setShowConnectionNote: mockSetShowConnectionNote,
                showConnectionNote: false,
            })
        );

        render(<ConnectionTab {...defaultProps} />);

        expect(screen.queryByText('Note')).not.toBeInTheDocument();
    });

    it('calls setShowConnectionNote(false) when close button is clicked', () => {
        render(<ConnectionTab {...defaultProps} />);

        const closeButton = screen.getByTitle('Close the note');

        fireEvent.click(closeButton);

        expect(mockSetShowConnectionNote).toHaveBeenCalledWith(false);
    });
});
