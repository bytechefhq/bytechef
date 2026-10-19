import {TooltipProvider} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {CopilotPanelContent} from '@/shared/components/copilot/CopilotPanelImpl';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {render} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    catalog: undefined as unknown,
    pickerProps: [] as Array<{selectedModel: string | null; selectedProvider: string | null}>,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAiDefaultModelQuery: () => ({
        data: {aiDefaultModel: {model: 'claude-sonnet', provider: 'ai.provider.anthropic'}},
        isPending: false,
    }),
    useAiProviderCatalogQuery: () => ({data: hoisted.catalog}),
}));

vi.mock('@/shared/components/ai/model-picker/ModelPicker', () => ({
    default: (props: {selectedModel: string | null; selectedProvider: string | null}) => {
        hoisted.pickerProps.push(props);

        return null;
    },
}));

vi.mock('@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider', () => ({
    CopilotRuntimeProvider: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('@/components/assistant-ui/thread', () => ({
    Thread: ({leadingComposerActions}: {leadingComposerActions?: ReactNode}) => <>{leadingComposerActions}</>,
}));

const WORKSPACE_ID = 1049;

const catalogOffering = (modelName: string) => ({
    aiProviderCatalog: [
        {
            enabled: true,
            key: 'ai.provider.openAi',
            models: [{name: modelName}],
            supportsModelById: false,
        },
    ],
});

const renderPanel = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <CopilotPanelContent />
            </TooltipProvider>
        </MemoryRouter>
    );

const lastPickerProps = () => hoisted.pickerProps.at(-1);

describe('CopilotPanelContent model picker', () => {
    beforeEach(() => {
        hoisted.catalog = undefined;
        hoisted.pickerProps = [];

        localStorage.clear();

        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
        useCopilotStore.setState({selectedLlmModel: null, selectedLlmProvider: null});

        writeLastUsedModel(WORKSPACE_ID, 'ai.provider.openAi', 'gpt-4o');
    });

    it('shows the remembered model when the catalog still offers it', () => {
        hoisted.catalog = catalogOffering('gpt-4o');

        renderPanel();

        expect(lastPickerProps()).toMatchObject({selectedModel: 'gpt-4o', selectedProvider: 'ai.provider.openAi'});
    });

    it('shows the default instead of a remembered model the catalog no longer offers, without forgetting it', () => {
        hoisted.catalog = catalogOffering('gpt-5');

        renderPanel();

        expect(lastPickerProps()).toMatchObject({selectedModel: null, selectedProvider: null});
        expect(readLastUsedModel(WORKSPACE_ID)).toEqual({model: 'gpt-4o', provider: 'ai.provider.openAi'});
    });

    it('shows the default while the catalog has not loaded', () => {
        renderPanel();

        expect(lastPickerProps()).toMatchObject({selectedModel: null, selectedProvider: null});
    });

    it('shows a model the user picked in this conversation as it is', () => {
        useCopilotStore.setState({selectedLlmModel: 'o3', selectedLlmProvider: 'ai.provider.openAi'});

        renderPanel();

        expect(lastPickerProps()).toMatchObject({selectedModel: 'o3', selectedProvider: 'ai.provider.openAi'});
    });
});
