import {
    McpAutomationProjectConfigurationsByServerIdQuery,
    useMcpAutomationProjectConfigurationsByServerIdQuery,
} from '@/shared/middleware/graphql';

export type McpAutomationProjectConfigurationItemType =
    McpAutomationProjectConfigurationsByServerIdQuery['mcpAutomationProjectConfigurationsByServerId'][number];

export type McpAutomationProjectConfigurationWorkflowItemType =
    McpAutomationProjectConfigurationItemType['workflows'][number];

const useMcpAutomationProjectConfigurationList = (mcpServerId: string) => {
    const {data, isLoading} = useMcpAutomationProjectConfigurationsByServerIdQuery({mcpServerId});

    const mcpAutomationProjectConfigurations = data?.mcpAutomationProjectConfigurationsByServerId ?? [];

    return {isLoading, mcpAutomationProjectConfigurations};
};

export default useMcpAutomationProjectConfigurationList;
