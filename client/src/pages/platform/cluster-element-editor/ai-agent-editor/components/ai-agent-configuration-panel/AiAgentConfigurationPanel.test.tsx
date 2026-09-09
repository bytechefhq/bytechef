import {render} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useClusterRootDataPillsMock} = vi.hoisted(() => ({
    useClusterRootDataPillsMock: vi.fn(),
}));

vi.mock('@/pages/platform/cluster-element-editor/hooks/useClusterRootDataPills', () => ({
    default: useClusterRootDataPillsMock,
}));

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentModelSelectField',
    () => ({default: () => null})
);

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentPromptField',
    () => ({default: () => null})
);

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentStreamResponseField',
    () => ({default: () => null})
);

vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentTools',
    () => ({default: () => null})
);

import {AiAgentConfigurationPanel} from './AiAgentConfigurationPanel';

describe('AiAgentConfigurationPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('computes the data pills as soon as the panel renders', () => {
        render(<AiAgentConfigurationPanel />);

        expect(useClusterRootDataPillsMock).toHaveBeenCalled();
    });
});
