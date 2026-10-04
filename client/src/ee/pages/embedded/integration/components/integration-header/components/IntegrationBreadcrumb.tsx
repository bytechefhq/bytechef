import {Breadcrumb, BreadcrumbItem, BreadcrumbList, BreadcrumbSeparator} from '@/components/ui/breadcrumb';
import IntegrationTitle from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationTitle';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {ReactNode} from 'react';

export interface IntegrationBreadcrumbProps {
    integration: Integration;
    /** The current-item switcher (e.g. IntegrationItemSelect) shown after the integration title, if any. */
    itemSelect?: ReactNode;
}

const IntegrationBreadcrumb = ({integration, itemSelect}: IntegrationBreadcrumbProps) => (
    <Breadcrumb>
        <BreadcrumbList>
            <BreadcrumbItem>
                <IntegrationTitle integration={integration} />
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
