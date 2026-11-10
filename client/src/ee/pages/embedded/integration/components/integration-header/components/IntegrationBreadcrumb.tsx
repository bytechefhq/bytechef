import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import IntegrationTitle from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationTitle';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
import {ReactNode} from 'react';

export interface IntegrationBreadcrumbProps {
    integration: Integration;
    integrations?: Integration[];
    /** The current-item switcher (e.g. IntegrationItemSelect) shown after the integration title, if any. */
    itemSelect?: ReactNode;
    onIntegrationValueChange?: (integrationId: number) => void;
}

const IntegrationBreadcrumb = ({
    integration,
    integrations,
    itemSelect,
    onIntegrationValueChange,
}: IntegrationBreadcrumbProps) => (
    <Breadcrumb className="min-w-0">
        <BreadcrumbList className="flex-nowrap">
            <ListPageBreadcrumbItem label="Integrations" to="/embedded/integrations" />

            <BreadcrumbItem>
                <IntegrationTitle
                    integration={integration}
                    integrations={integrations}
                    onIntegrationValueChange={onIntegrationValueChange}
                />
            </BreadcrumbItem>

            {itemSelect && (
                <>
                    <BreadcrumbSeparator />

                    <BreadcrumbItem>{itemSelect}</BreadcrumbItem>
                </>
            )}
        </BreadcrumbList>
    </Breadcrumb>
);

export default IntegrationBreadcrumb;
