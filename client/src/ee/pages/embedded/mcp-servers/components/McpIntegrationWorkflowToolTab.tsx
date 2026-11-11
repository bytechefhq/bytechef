import McpWorkflowDialogToolForm from '@/pages/platform/mcp-servers/components/McpWorkflowDialogToolForm';
import {McpWorkflowToolFormsType} from '@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolForms';

import useMcpIntegrationWorkflowToolTab from './hooks/useMcpIntegrationWorkflowToolTab';

interface McpIntegrationWorkflowToolTabProps {
    integrationInstanceConfigurationId: string;
    toolForms: McpWorkflowToolFormsType;
    workflowId: string;
}

const McpIntegrationWorkflowToolTab = ({
    integrationInstanceConfigurationId,
    toolForms,
    workflowId,
}: McpIntegrationWorkflowToolTabProps) => {
    const {isLoading, properties} = useMcpIntegrationWorkflowToolTab({integrationInstanceConfigurationId, workflowId});

    return (
        <McpWorkflowDialogToolForm
            isLoading={isLoading}
            properties={properties}
            toolForms={toolForms}
            workflowId={workflowId}
        />
    );
};

export default McpIntegrationWorkflowToolTab;
