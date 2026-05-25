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

export {};
