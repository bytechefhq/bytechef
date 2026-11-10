import Badge from '@/components/Badge/Badge';
import {Integration, IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import BreadcrumbTitleSelect, {getPublishedVersion} from '@/shared/components/BreadcrumbTitleSelect';
import {PuzzleIcon} from 'lucide-react';
import {useMemo} from 'react';

interface IntegrationTitleProps {
    integration: Integration;
    integrations?: Integration[];
    onIntegrationValueChange?: (integrationId: number) => void;
}

const IntegrationTitle = ({integration, integrations, onIntegrationValueChange}: IntegrationTitleProps) => {
    const items = useMemo(
        () =>
            integrations?.map((workspaceIntegration) => ({
                disabled: !workspaceIntegration.integrationWorkflowIds?.length,
                id: String(workspaceIntegration.id),
                name: workspaceIntegration.name ?? workspaceIntegration.componentName,
                publishedVersion: getPublishedVersion(
                    workspaceIntegration.lastPublishedDate,
                    workspaceIntegration.lastIntegrationVersion,
                    workspaceIntegration.lastStatus === IntegrationStatus.Published
                ),
            })),
        [integrations]
    );

    return (
        <BreadcrumbTitleSelect
            ariaLabel="Integration select"
            badge={
                <Badge
                    className="flex shrink-0 space-x-1 border-transparent"
                    styleType={
                        integration.lastStatus === IntegrationStatus.Published ? 'success-outline' : 'secondary-filled'
                    }
                    weight="semibold"
                >
                    <span>V{integration.lastIntegrationVersion}</span>

                    <span>{integration.lastStatus}</span>
                </Badge>
            }
            currentId={integration.id !== undefined ? String(integration.id) : undefined}
            icon={PuzzleIcon}
            items={items}
            menuLabel="Integrations"
            name={integration.name ?? integration.componentName}
            onValueChange={onIntegrationValueChange && ((value) => onIntegrationValueChange(Number(value)))}
        />
    );
};

export default IntegrationTitle;
