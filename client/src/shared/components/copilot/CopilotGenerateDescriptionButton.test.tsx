import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CopilotGenerateDescriptionButton from './CopilotGenerateDescriptionButton';

const {generateMock} = vi.hoisted(() => ({generateMock: vi.fn()}));

vi.mock('./useGenerateWorkflowDescription', () => ({
    useGenerateWorkflowDescription: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

describe('CopilotGenerateDescriptionButton', () => {
    beforeEach(() => {
        generateMock.mockReset();
    });

    it('generates and applies the value', async () => {
        const onApply = vi.fn();

        generateMock.mockResolvedValue({value: 'Syncs records nightly.'});

        render(<CopilotGenerateDescriptionButton environmentId={1} onApply={onApply} workflowId="wf1" />);

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await vi.waitFor(() => expect(onApply).toHaveBeenCalledWith('Syncs records nightly.'));
        expect(generateMock).toHaveBeenCalledWith({environmentId: 1, workflowId: 'wf1', workflowNodeName: undefined});
    });

    it('renders nothing when workflowId is undefined', () => {
        const {container} = render(
            <CopilotGenerateDescriptionButton environmentId={1} onApply={vi.fn()} workflowId={undefined} />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
