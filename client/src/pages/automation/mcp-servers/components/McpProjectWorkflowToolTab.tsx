import McpWorkflowDialogToolForm from '@/pages/platform/mcp-servers/components/McpWorkflowDialogToolForm';
import {McpWorkflowToolFormsType} from '@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolForms';

import useMcpProjectWorkflowToolTab from './useMcpProjectWorkflowToolTab';

interface McpProjectWorkflowToolTabProps {
    projectId: number;
    toolForms: McpWorkflowToolFormsType;
    workflowId: string;
}

const McpProjectWorkflowToolTab = ({projectId, toolForms, workflowId}: McpProjectWorkflowToolTabProps) => {
    const {isLoading, properties} = useMcpProjectWorkflowToolTab({projectId, workflowId});

    return (
        <McpWorkflowDialogToolForm
            isLoading={isLoading}
            properties={properties}
            toolForms={toolForms}
            workflowId={workflowId}
        />
    );
};

export default McpProjectWorkflowToolTab;
