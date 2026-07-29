import {QueryClient} from '@tanstack/react-query';
import {describe, expect, it} from 'vitest';

import invalidateSkillQueries from '../invalidateSkillQueries';

describe('invalidateSkillQueries', () => {
    it('invalidates the skill list and all skill detail queries', () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(['aiSkills'], {aiSkills: [{id: '1'}]});
        queryClient.setQueryData(['aiSkill', {id: '1'}], {aiSkill: {id: '1', name: 'hello-world-example'}});
        queryClient.setQueryData(['aiSkillFilePaths', {id: '1'}], {aiSkillFilePaths: ['SKILL.md']});
        queryClient.setQueryData(['aiSkillFileContent', {id: '1', path: 'SKILL.md'}], {
            aiSkillFileContent: '# Hello World',
        });

        invalidateSkillQueries(queryClient);

        expect(queryClient.getQueryState(['aiSkills'])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(['aiSkill', {id: '1'}])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(['aiSkillFilePaths', {id: '1'}])?.isInvalidated).toBe(true);
        expect(queryClient.getQueryState(['aiSkillFileContent', {id: '1', path: 'SKILL.md'}])?.isInvalidated).toBe(
            true
        );
    });

    it('does not invalidate unrelated queries', () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(['aiAgents'], {aiAgents: []});

        invalidateSkillQueries(queryClient);

        expect(queryClient.getQueryState(['aiAgents'])?.isInvalidated).toBe(false);
    });
});
