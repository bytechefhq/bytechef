vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {render} from '@/shared/util/test-utils';
import {act, waitFor} from '@testing-library/react';
import {useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInput from '../PropertyMentionsInput';
import {TOOL_PROPERTY_FORMULA_MODE_PLACEHOLDER, TOOL_PROPERTY_PLACEHOLDER} from '../mentionsInputPlaceholder';

/**
 * The placeholder differs per formula mode, and the Placeholder extension resolves it per decoration from a
 * ref. Handing the editor an already-resolved string instead froze the hint on whatever the mode was when the
 * editor was built.
 */

let toggleFormulaMode: (() => void) | undefined;

const Wrapper = ({toolProperty}: {toolProperty?: boolean}) => {
    const [isFormulaMode, setIsFormulaMode] = useState(true);

    toggleFormulaMode = () => setIsFormulaMode((current) => !current);

    return (
        <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
            <PropertyMentionsInput
                controlType="TEXT"
                isFormulaMode={isFormulaMode}
                label="Editor"
                leadingIcon="📄"
                path="parameters.uri"
                placeholder="https://example.com/index.html"
                setIsFormulaMode={setIsFormulaMode}
                toolProperty={toolProperty}
                type="STRING"
                value=""
            />
        </WorkflowEditorProvider>
    );
};

const microtaskTick = async (times = 1) => {
    for (let index = 0; index < times; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }
};

describe('editor identity across a formula mode toggle', () => {
    beforeEach(() => {
        toggleFormulaMode = undefined;

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-toggle-identity', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {connectionId: undefined, workflowNodeName: 'test_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('swaps the placeholder when formula mode is left', async () => {
        const {container} = render(<Wrapper toolProperty />);

        await microtaskTick(4);

        await waitFor(() =>
            expect(container.querySelector('[data-placeholder]')?.getAttribute('data-placeholder')).toBe(
                TOOL_PROPERTY_FORMULA_MODE_PLACEHOLDER
            )
        );

        act(() => toggleFormulaMode!());

        await waitFor(() =>
            expect(container.querySelector('[data-placeholder]')?.getAttribute('data-placeholder')).toBe(
                TOOL_PROPERTY_PLACEHOLDER
            )
        );
    });
});
