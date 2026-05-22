// Augments the generated ComponentConnection interfaces (automation + platform) with the
// runtime-only subflow fields. They are injected client-side in projectDeploymentDialog-utils.ts
// to associate connections with their owning subflow chain (root -> leaf), enabling nested
// subflow grouping in the UI. Kept here so it survives OpenAPI client regeneration (which
// overwrites the model files).

declare module '@/shared/middleware/automation/configuration/models/ComponentConnection' {
    interface ComponentConnection {
        subflowWorkflowUuid?: string;
        subflowWorkflowUuidPath?: string[];
    }
}

declare module '@/shared/middleware/platform/configuration/models/ComponentConnection' {
    interface ComponentConnection {
        subflowWorkflowUuid?: string;
        subflowWorkflowUuidPath?: string[];
    }
}

declare module '@/shared/middleware/automation/configuration/models/WorkflowInput' {
    interface WorkflowInput {
        subflowWorkflowUuid?: string;
        subflowWorkflowUuidPath?: string[];
    }
}

declare module '@/shared/middleware/platform/configuration/models/WorkflowInput' {
    interface WorkflowInput {
        subflowWorkflowUuid?: string;
        subflowWorkflowUuidPath?: string[];
    }
}

export {};
