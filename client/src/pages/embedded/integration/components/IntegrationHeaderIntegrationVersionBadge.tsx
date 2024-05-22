import {Badge} from '@/components/ui/badge';
import {IntegrationModel, IntegrationStatusModel} from '@/shared/middleware/embedded/configuration';

const IntegrationHeaderIntegrationVersionBadge = ({integration}: {integration: IntegrationModel}) => (
    <Badge
        className="flex space-x-1"
        variant={integration.status === IntegrationStatusModel.Published ? 'success' : 'secondary'}
    >
        <span>V{integration.integrationVersion}</span>

        <span>{integration.status === IntegrationStatusModel.Published ? `Published` : 'Draft'}</span>
    </Badge>
);

export default IntegrationHeaderIntegrationVersionBadge;
