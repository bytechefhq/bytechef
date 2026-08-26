import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgentModelSummary from './AgentModelSummary';

const {aiProviderCatalogQuery, getRootComponentClusterElementDefinitions} = vi.hoisted(() => ({
    aiProviderCatalogQuery: vi.fn(),
    getRootComponentClusterElementDefinitions: vi.fn(),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="provider icon" src={src} />,
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    useGetRootComponentClusterElementDefinitions: getRootComponentClusterElementDefinitions,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiProviderCatalogQuery: aiProviderCatalogQuery,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 123})),
}));

const PROVIDERS_RESULT = {
    data: [{componentName: 'openAi', componentVersion: 1, icon: '<svg />', name: 'model', title: 'OpenAI Model'}],
};

const CATALOG_RESULT = {
    data: {aiProviderCatalog: [{key: 'openAi', models: [{label: 'GPT-4o', name: 'gpt-4o'}]}]},
};

beforeEach(() => {
    getRootComponentClusterElementDefinitions.mockReset().mockReturnValue(PROVIDERS_RESULT);
    aiProviderCatalogQuery.mockReset().mockReturnValue(CATALOG_RESULT);
});

describe('AgentModelSummary', () => {
    // "OpenAI Model" is the cluster element's title; the trailing noun is redundant under a heading that
    // already reads Model, and the catalog's models.dev name beats the raw id where it exists.
    it('reads as the provider title and the catalog name for the model', () => {
        render(<AgentModelSummary model="gpt-4o" onClick={vi.fn()} provider="openAi" />);

        expect(screen.getByRole('button')).toHaveTextContent('OpenAI · GPT-4o');
    });

    it('falls back to the raw model id when the catalog does not name it', () => {
        render(<AgentModelSummary model="gpt-4o-nano" onClick={vi.fn()} provider="openAi" />);

        expect(screen.getByRole('button')).toHaveTextContent('OpenAI · gpt-4o-nano');
    });

    it('invites a first pick when no model is configured', () => {
        render(<AgentModelSummary model="" onClick={vi.fn()} provider="" />);

        expect(screen.getByRole('button', {name: 'Select model'})).toBeInTheDocument();
    });
});
