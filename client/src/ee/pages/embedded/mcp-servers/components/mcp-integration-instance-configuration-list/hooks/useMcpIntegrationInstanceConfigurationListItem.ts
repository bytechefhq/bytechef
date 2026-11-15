import {useState} from 'react';

const useMcpIntegrationInstanceConfigurationListItem = () => {
    const [showEditWorkflowsDialog, setShowEditWorkflowsDialog] = useState(false);

    return {
        setShowEditWorkflowsDialog,
        showEditWorkflowsDialog,
    };
};

export default useMcpIntegrationInstanceConfigurationListItem;
