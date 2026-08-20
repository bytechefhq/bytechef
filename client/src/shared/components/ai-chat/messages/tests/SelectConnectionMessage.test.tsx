import {act, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const capturedListeners: Array<() => void> = [];

let threadMessageCount = 0;

vi.mock('@assistant-ui/react', async () => {
    const actual = await vi.importActual<typeof import('@assistant-ui/react')>('@assistant-ui/react');

    return {
        ...actual,
        useAui: vi.fn(() => ({
            thread: {
                append: vi.fn(),
                getState: () => ({messages: new Array(threadMessageCount)}),
                subscribe: (listener: () => void) => {
                    capturedListeners.push(listener);

                    return () => {};
                },
            },
        })),
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1049}),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: () => ({data: {version: 1}}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: () => ({
        data: [
            {id: 1224, name: 'Gmail25'},
            {id: 1225, name: 'Gmail26'},
        ],
    }),
}));

vi.mock('@/shared/components/visibility/ResourceVisibilityBadge', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: () => null,
}));

import SelectConnectionMessage from '../SelectConnectionMessage';

const DATA = {
    componentLabel: 'Gmail',
    componentName: 'gmail',
    kind: 'select-connection' as const,
};

const simulateLaterMessages = (count: number) => {
    act(() => {
        threadMessageCount = count;

        capturedListeners.forEach((listener) => listener());
    });
};

describe('SelectConnectionMessage', () => {
    beforeEach(() => {
        capturedListeners.length = 0;
        threadMessageCount = 0;
    });

    it('renders an enabled connection dropdown', () => {
        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <SelectConnectionMessage {...({data: DATA} as any)} />
        );

        expect(screen.getByRole('combobox')).toBeEnabled();
    });

    // Regression: selectConnection is signaling-only and does not stop the agent's turn, so the agent emits
    // follow-up messages (a second picker, an askUserQuestion, narration) right after rendering this one. The
    // picker must stay interactive rather than disabling itself the moment a later message lands — otherwise it
    // becomes a dead, unclickable dropdown and the agent re-issues selectConnection.
    it('stays enabled after later messages land on the thread', () => {
        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <SelectConnectionMessage {...({data: DATA} as any)} />
        );

        expect(screen.getByRole('combobox')).toBeEnabled();

        simulateLaterMessages(3);

        expect(screen.getByRole('combobox')).toBeEnabled();
    });
});
