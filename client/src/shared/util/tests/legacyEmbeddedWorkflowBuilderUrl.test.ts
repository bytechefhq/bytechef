import {getLegacyEmbeddedWorkflowBuilderUrl} from '@/shared/util/legacyEmbeddedWorkflowBuilderUrl';
import {describe, expect, it} from 'vitest';

describe('getLegacyEmbeddedWorkflowBuilderUrl', () => {
    it('sends the path older SDKs embed to the standalone workflow builder, keeping the query string', () => {
        expect(
            getLegacyEmbeddedWorkflowBuilderUrl({pathname: '/embedded/workflow-builder/uuid-1', search: '?tab=1'})
        ).toBe('/workflow-builder.html?tab=1#/embedded/builder/uuid-1');
    });

    it('accepts a trailing slash', () => {
        expect(getLegacyEmbeddedWorkflowBuilderUrl({pathname: '/embedded/workflow-builder/uuid-1/', search: ''})).toBe(
            '/workflow-builder.html#/embedded/builder/uuid-1'
        );
    });

    it('leaves every other path alone', () => {
        expect(getLegacyEmbeddedWorkflowBuilderUrl({pathname: '/embedded/integrations', search: ''})).toBeUndefined();
        expect(
            getLegacyEmbeddedWorkflowBuilderUrl({pathname: '/embedded/workflow-builder', search: ''})
        ).toBeUndefined();
        expect(
            getLegacyEmbeddedWorkflowBuilderUrl({pathname: '/embedded/workflow-builder/uuid-1/extra', search: ''})
        ).toBeUndefined();
    });
});
