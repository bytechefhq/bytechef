import {Project} from '@/shared/middleware/automation/configuration';

export interface GuideWorkflowI {
    projectId: number;
    projectWorkflowId: number;
}

const GUIDE_WORKFLOW_KEY = 'bytechef.automation-getting-started.guide-workflow';
const GUIDE_PROJECT_KEY = 'bytechef.automation-getting-started.guide-project';
const EDITOR_GUIDE_COLLAPSED_KEY = 'bytechef.automation-getting-started.editor-guide-collapsed';

export function getRememberedGuideWorkflow(): GuideWorkflowI | undefined {
    try {
        const storedValue = localStorage.getItem(GUIDE_WORKFLOW_KEY);

        if (!storedValue) {
            return undefined;
        }

        const parsedValue = JSON.parse(storedValue);

        if (typeof parsedValue?.projectId === 'number' && typeof parsedValue?.projectWorkflowId === 'number') {
            return {projectId: parsedValue.projectId, projectWorkflowId: parsedValue.projectWorkflowId};
        }

        return undefined;
    } catch {
        return undefined;
    }
}

export function getRememberedGuideProjectId(): number | undefined {
    try {
        const storedValue = localStorage.getItem(GUIDE_PROJECT_KEY);

        if (!storedValue) {
            return undefined;
        }

        const projectId = Number(storedValue);

        return Number.isInteger(projectId) ? projectId : undefined;
    } catch {
        return undefined;
    }
}

export function rememberGuideProject(projectId: number): void {
    try {
        localStorage.setItem(GUIDE_PROJECT_KEY, String(projectId));
    } catch {
        return;
    }
}

export function resolveGuideProjectId(
    rememberedProjectId: number | undefined,
    guideWorkflow: GuideWorkflowI | undefined,
    projects: Project[] | undefined
): number | undefined {
    if (guideWorkflow) {
        return guideWorkflow.projectId;
    }

    if (rememberedProjectId !== undefined && projects?.some((project) => project.id === rememberedProjectId)) {
        return rememberedProjectId;
    }

    return undefined;
}

export function rememberGuideWorkflow(guideWorkflow: GuideWorkflowI): void {
    try {
        localStorage.setItem(
            GUIDE_WORKFLOW_KEY,
            JSON.stringify({projectId: guideWorkflow.projectId, projectWorkflowId: guideWorkflow.projectWorkflowId})
        );
    } catch {
        return;
    }
}

export function resolveGuideWorkflow(
    remembered: GuideWorkflowI | undefined,
    projects: Project[] | undefined
): GuideWorkflowI | undefined {
    if (!projects) {
        return undefined;
    }

    if (remembered) {
        const rememberedProject = projects.find((project) => project.id === remembered.projectId);

        if (rememberedProject?.projectWorkflowIds?.includes(remembered.projectWorkflowId)) {
            return remembered;
        }
    }

    return undefined;
}

export function getGuideWorkflowHref(guideWorkflow: GuideWorkflowI | undefined): string {
    if (!guideWorkflow) {
        return '/automation/projects';
    }

    return `/automation/projects/${guideWorkflow.projectId}/project-workflows/${guideWorkflow.projectWorkflowId}`;
}

export function isEditorGuideCollapsed(): boolean {
    try {
        return localStorage.getItem(EDITOR_GUIDE_COLLAPSED_KEY) === 'true';
    } catch {
        return false;
    }
}

export function setEditorGuideCollapsed(collapsed: boolean): void {
    try {
        if (collapsed) {
            localStorage.setItem(EDITOR_GUIDE_COLLAPSED_KEY, 'true');
        } else {
            localStorage.removeItem(EDITOR_GUIDE_COLLAPSED_KEY);
        }
    } catch {
        return;
    }
}
