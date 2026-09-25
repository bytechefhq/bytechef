import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Properties from '../Properties';

const {issueParameterNamesState} = vi.hoisted(() => ({
    issueParameterNamesState: {issueParameterNames: new Set<string>()},
}));

vi.mock('../Property', () => ({
    default: ({property}: {property: PropertyAllType}) => <li>{property.label}</li>,
}));

vi.mock('../../../hooks/useNodeIssueParameterNames', () => ({
    default: () => issueParameterNamesState.issueParameterNames,
}));

vi.mock('../../../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: unknown) => unknown) =>
        selector({
            currentNode: {
                operationName: 'ask',
                parameters: {topK: '${firecrawl_5.data.json.result}'},
                workflowNodeName: 'anthropic_2',
            },
        }),
}));

const properties = [
    {label: 'Model', name: 'model', type: 'STRING'},
    {advancedOption: true, label: 'Top K', name: 'topK', type: 'INTEGER'},
] as Array<PropertyAllType>;

describe('Properties advanced section', () => {
    beforeEach(() => {
        issueParameterNamesState.issueParameterNames = new Set();
    });

    it('keeps the advanced properties collapsed when none of them has an issue', () => {
        issueParameterNamesState.issueParameterNames = new Set(['model']);

        render(<Properties properties={properties} />);

        expect(screen.getByText('Model')).toBeInTheDocument();
        expect(screen.queryByText('Top K')).not.toBeInTheDocument();
    });

    it('opens the advanced properties when one of them has an issue', () => {
        issueParameterNamesState.issueParameterNames = new Set(['topK']);

        render(<Properties properties={properties} />);

        expect(screen.getByText('Top K')).toBeInTheDocument();
    });
});
