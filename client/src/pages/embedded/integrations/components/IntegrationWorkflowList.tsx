import {IntegrationModel} from '@/middleware/embedded/configuration';

interface IntegrationWorkflowListProps {
    integration: IntegrationModel;
}

const IntegrationWorkflowList = ({integration}: IntegrationWorkflowListProps) => {
    return <>{integration?.componentName}</>;
};

export default IntegrationWorkflowList;
