import {buildSearchResultRoute} from '@/shared/command-bar/resourceCommandRoutes';
import {SearchAssetType} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

describe('buildSearchResultRoute', () => {
    it('routes a workflow to its project-workflow editor', () => {
        expect(buildSearchResultRoute({id: '77', projectId: '5', type: SearchAssetType.Workflow})).toBe(
            '/automation/projects/5/project-workflows/77'
        );
    });

    it('routes a project through its first project workflow', () => {
        expect(buildSearchResultRoute({id: '5', projectWorkflowId: '77', type: SearchAssetType.Project})).toBe(
            '/automation/projects/5/project-workflows/77'
        );
    });

    it('routes a project with no workflows to the projects list', () => {
        expect(buildSearchResultRoute({id: '5', type: SearchAssetType.Project})).toBe('/automation/projects');
    });

    it('routes a data table to the unhyphenated path', () => {
        expect(buildSearchResultRoute({id: '3', type: SearchAssetType.DataTable})).toBe('/automation/datatables/3');
    });

    it('routes an asset file to its detail page', () => {
        expect(buildSearchResultRoute({id: '9', type: SearchAssetType.AssetFile})).toBe('/automation/asset-files/9');
    });

    it('routes a type with no detail route to its list page', () => {
        expect(buildSearchResultRoute({id: '4', type: SearchAssetType.Connection})).toBe('/automation/connections');
        expect(buildSearchResultRoute({id: '4', type: SearchAssetType.Deployment})).toBe('/automation/deployments');
        expect(buildSearchResultRoute({id: '4', type: SearchAssetType.ApiCollection})).toBe(
            '/automation/api-platform/api-collections'
        );
    });

    it('routes a knowledge base document to its knowledge base page', () => {
        const route = buildSearchResultRoute({
            id: '6',
            knowledgeBaseId: '2',
            type: SearchAssetType.KnowledgeBaseDocument,
        });

        expect(route).toBe('/automation/knowledge-bases/2');
        expect(route).not.toContain('/documents/');
    });

    it('routes a knowledge base document with no knowledge base id to undefined', () => {
        expect(buildSearchResultRoute({id: '6', type: SearchAssetType.KnowledgeBaseDocument})).toBeUndefined();
    });
});
