declare module '@/shared/middleware/automation/configuration/models/WorkflowInput' {
    interface WorkflowInput {
        subflowInputName?: string;
        subflowWorkflowUuid?: string;
        subflowWorkflowUuidPath?: string[];
    }
}

declare module '@/shared/middleware/platform/configuration/models/WorkflowInput' {
    interface WorkflowInput {
        subflowInputName?: string;
        subflowWorkflowUuid?: string;
        subflowWorkflowUuidPath?: string[];
    }
}

export {};
