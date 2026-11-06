import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {AiEvalRuleType} from '../../../types';
import AiEvalRules from '../AiEvalRules';

const evalRule: AiEvalRuleType = {
    createdDate: '1700000000000',
    delaySeconds: 30,
    enabled: true,
    filters: null,
    id: '1',
    lastModifiedDate: '1700000000000',
    model: 'gpt-4',
    name: 'Helpfulness',
    projectId: null,
    promptTemplate: 'Rate the response',
    samplingRate: 0.5,
    scoreConfigId: '1',
    version: 1,
    workspaceId: '1',
};

describe('AiEvalRules', () => {
    it('renders a row per eval rule', () => {
        render(<AiEvalRules evalRules={[evalRule]} isLoading={false} />);

        expect(screen.getByText('Helpfulness')).toBeInTheDocument();
        expect(screen.getByText('gpt-4')).toBeInTheDocument();
        expect(screen.getByText('50%')).toBeInTheDocument();
        expect(screen.getByText('30s')).toBeInTheDocument();
        expect(screen.getByText('Enabled')).toBeInTheDocument();
    });

    it('renders the empty state when there are no eval rules', () => {
        render(<AiEvalRules evalRules={[]} isLoading={false} />);

        expect(screen.getByText('No Eval Rules')).toBeInTheDocument();
    });
});
