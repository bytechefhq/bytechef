import McpWorkflowDialogToolForm from '@/pages/platform/mcp-servers/components/McpWorkflowDialogToolForm';
import {McpWorkflowToolFormsType} from '@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolForms';
import {useToolEligibleAutomationProjectConfigurationWorkflowPropertiesQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

import {toToolEligibleAutomationWorkflowProperties} from './mcpAutomationWorkflowToolProperties';

interface McpAutomationWorkflowToolFieldsProps {
    automationProjectConfigurationId: string;
    toolForms: McpWorkflowToolFormsType;
    workflowUuid: string;
}

const McpAutomationWorkflowToolFields = ({
    automationProjectConfigurationId,
    toolForms,
    workflowUuid,
}: McpAutomationWorkflowToolFieldsProps) => {
    const {data: propertiesData, isLoading} = useToolEligibleAutomationProjectConfigurationWorkflowPropertiesQuery({
        automationProjectConfigurationId,
        workflowUuid,
    });

    const properties = useMemo(() => toToolEligibleAutomationWorkflowProperties(propertiesData), [propertiesData]);

    return (
        <McpWorkflowDialogToolForm
            isLoading={isLoading}
            properties={properties}
            toolForms={toolForms}
            workflowId={workflowUuid}
        />
    );
};

export default McpAutomationWorkflowToolFields;
