import {
    getGuideWorkflowHref,
    getRememberedGuideProjectId,
    getRememberedGuideWorkflow,
    isEditorGuideCollapsed,
    rememberGuideProject,
    rememberGuideWorkflow,
    resolveGuideProjectId,
    resolveGuideWorkflow,
    setEditorGuideCollapsed,
} from '@/pages/automation/getting-started/guideWorkflowStorage';
import {Project} from '@/shared/middleware/automation/configuration';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('guideWorkflowStorage', () => {
    beforeEach(() => {
        localStorage.clear();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('remembered guide workflow', () => {
        it('round-trips the remembered workflow', () => {
            rememberGuideWorkflow({projectId: 2, projectWorkflowId: 3});

            expect(getRememberedGuideWorkflow()).toEqual({projectId: 2, projectWorkflowId: 3});
        });

        it('stores the workflow under one key for the whole tenant', () => {
            rememberGuideWorkflow({projectId: 2, projectWorkflowId: 3});

            expect(localStorage.getItem('bytechef.automation-getting-started.guide-workflow')).toBe(
                JSON.stringify({projectId: 2, projectWorkflowId: 3})
            );
        });

        it('returns undefined for malformed JSON', () => {
            localStorage.setItem('bytechef.automation-getting-started.guide-workflow', '{nope');

            expect(getRememberedGuideWorkflow()).toBeUndefined();
        });

        it('does not throw when storage is denied', () => {
            vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
                throw new Error('denied');
            });
            vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
                throw new Error('denied');
            });

            expect(() => rememberGuideWorkflow({projectId: 2, projectWorkflowId: 3})).not.toThrow();
            expect(getRememberedGuideWorkflow()).toBeUndefined();
        });
    });

    describe('resolveGuideWorkflow', () => {
        const remembered = {projectId: 2, projectWorkflowId: 3};

        it('returns the remembered workflow when it still exists', () => {
            const projects: Project[] = [{id: 2, projectWorkflowIds: [3, 4]} as Project, {id: 5} as Project];

            expect(resolveGuideWorkflow(remembered, projects)).toEqual(remembered);
        });

        it('does not guess a workflow the guide did not create, even for the only project', () => {
            const projects: Project[] = [{id: 2, projectWorkflowIds: [9]} as Project];

            expect(resolveGuideWorkflow(remembered, projects)).toBeUndefined();
        });

        it('returns undefined when the remembered workflow is gone and there are two projects', () => {
            const projects: Project[] = [
                {id: 2, projectWorkflowIds: [9]} as Project,
                {id: 5, projectWorkflowIds: [6]} as Project,
            ];

            expect(resolveGuideWorkflow(remembered, projects)).toBeUndefined();
        });

        it('returns undefined without a remembered workflow when the only project has two workflows', () => {
            const projects: Project[] = [{id: 2, projectWorkflowIds: [3, 4]} as Project];

            expect(resolveGuideWorkflow(undefined, projects)).toBeUndefined();
        });

        it('returns undefined while projects are not loaded', () => {
            expect(resolveGuideWorkflow(remembered, undefined)).toBeUndefined();
        });
    });

    describe('editor guide collapsed', () => {
        it('round-trips the collapsed flag and removes the key when expanded', () => {
            expect(isEditorGuideCollapsed()).toBe(false);

            setEditorGuideCollapsed(true);

            expect(isEditorGuideCollapsed()).toBe(true);

            setEditorGuideCollapsed(false);

            expect(isEditorGuideCollapsed()).toBe(false);
            expect(localStorage.getItem('bytechef.automation-getting-started.editor-guide-collapsed')).toBeNull();
        });

        it('does not throw when storage is denied', () => {
            vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
                throw new Error('denied');
            });
            vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
                throw new Error('denied');
            });
            vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
                throw new Error('denied');
            });

            expect(() => setEditorGuideCollapsed(true)).not.toThrow();
            expect(() => setEditorGuideCollapsed(false)).not.toThrow();
            expect(isEditorGuideCollapsed()).toBe(false);
        });
    });

    describe('getGuideWorkflowHref', () => {
        it('links to the workflow editor', () => {
            expect(getGuideWorkflowHref({projectId: 2, projectWorkflowId: 3})).toBe(
                '/automation/projects/2/project-workflows/3'
            );
        });

        it('links to the projects list without a workflow', () => {
            expect(getGuideWorkflowHref(undefined)).toBe('/automation/projects');
        });
    });

    describe('guide project', () => {
        it('remembers and reads the guide project', () => {
            expect(getRememberedGuideProjectId()).toBeUndefined();

            rememberGuideProject(4);

            expect(getRememberedGuideProjectId()).toBe(4);
        });

        it('ignores a stored value that is not a project id', () => {
            localStorage.setItem('bytechef.automation-getting-started.guide-project', 'abc');

            expect(getRememberedGuideProjectId()).toBeUndefined();
        });

        it('resolves the remembered guide project only while it exists', () => {
            const projects = [{id: 4}, {id: 7}] as Project[];

            expect(resolveGuideProjectId(4, undefined, projects)).toBe(4);
            expect(resolveGuideProjectId(6, undefined, projects)).toBeUndefined();
            expect(resolveGuideProjectId(undefined, undefined, projects)).toBeUndefined();
        });

        it("prefers the guide workflow's project", () => {
            expect(
                resolveGuideProjectId(4, {projectId: 7, projectWorkflowId: 3}, [{id: 4}, {id: 7}] as Project[])
            ).toBe(7);
        });
    });
});
