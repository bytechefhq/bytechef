export const MCP_AUTOMATION_TOOL_DESCRIPTION_KEY = 'toolDescription';
export const MCP_AUTOMATION_TOOL_NAME_KEY = 'toolName';

export const getMcpAutomationToolText = (
    parameters: Record<string, unknown> | null | undefined,
    key: typeof MCP_AUTOMATION_TOOL_DESCRIPTION_KEY | typeof MCP_AUTOMATION_TOOL_NAME_KEY
): string => {
    const value = parameters?.[key];

    return typeof value === 'string' ? value : '';
};

export const buildMcpAutomationToolParameters = (
    parameters: Record<string, unknown> | null | undefined,
    key: typeof MCP_AUTOMATION_TOOL_DESCRIPTION_KEY | typeof MCP_AUTOMATION_TOOL_NAME_KEY,
    value: string
): Record<string, unknown> => {
    const updatedParameters = {...parameters};

    if (value === '') {
        delete updatedParameters[key];
    } else {
        updatedParameters[key] = value;
    }

    return updatedParameters;
};
