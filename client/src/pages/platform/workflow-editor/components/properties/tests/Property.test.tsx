import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Property from '../Property';

// Mutable hook return so each test can pick which node kind Property renders for: a plain task node has no
// clusterElementType, every cluster element (tools, subagent, model, ...) carries the key it is registered under.
const {hookState} = vi.hoisted(() => ({
    hookState: {
        clusterElementType: undefined as string | undefined,
    },
}));

// Render only the copilot slot so the assertions are about the gate, not about TipTap.
vi.mock(
    '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput',
    () => ({
        default: ({deletePropertyButton}: {deletePropertyButton?: ReactNode}) => <div>{deletePropertyButton}</div>,
    })
);

vi.mock(
    '@/pages/platform/workflow-editor/components/properties/components/property-copilot/PropertyCopilotButton',
    () => ({
        default: () => <button type="button">Ask copilot</button>,
    })
);

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/hooks/useProperty', () => ({
    default: () => ({
        calculatedPath: 'parameters.value',
        controlType: 'TEXT',
        currentNode: {
            clusterElementType: hookState.clusterElementType,
            name: 'activeCampaign_1',
        },
        editorRef: {current: null},
        handleMentionInputValueChange: vi.fn(),
        mentionInput: true,
        name: 'value',
        setIsFormulaMode: vi.fn(),
        type: 'STRING',
        workflow: {id: 'workflow-1'},
    }),
}));

const renderProperty = () => render(<Property property={{name: 'value', type: 'STRING'} as PropertyAllType} />);

describe('Property', () => {
    beforeEach(() => {
        hookState.clusterElementType = undefined;
    });

    it('renders the copilot button for a plain task node', () => {
        renderProperty();

        expect(screen.getByRole('button', {name: 'Ask copilot'})).toBeInTheDocument();
    });

    it.each(['tools', 'subagent', 'model'])('hides the copilot button for the %s cluster element', (type) => {
        hookState.clusterElementType = type;

        renderProperty();

        expect(screen.queryByRole('button', {name: 'Ask copilot'})).not.toBeInTheDocument();
    });
});
