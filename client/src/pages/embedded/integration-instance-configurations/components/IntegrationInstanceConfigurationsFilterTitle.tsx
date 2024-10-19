import {Badge} from '@/components/ui/badge';
import {Type} from '@/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations';
import {Integration, Tag} from '@/shared/middleware/embedded/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const IntegrationInstanceConfigurationsFilterTitle = ({
    environment,
    filterData,
    integrations,
    tags,
}: {
    environment: number;
    filterData: {id?: number | string; type: Type};
    integrations: Integration[] | undefined;
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Integration) {
        pageTitle = integrations?.find((integration) => integration.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm uppercase text-muted-foreground">Filter by environment:</span>

            <Badge variant="secondary">
                <span className="text-sm">{environment === 1 ? 'Test' : 'Production'}</span>
            </Badge>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'integration'}:
            </span>

            <Badge variant="secondary">
                <span className="text-sm">{pageTitle ?? 'All Integrations'}</span>
            </Badge>
        </div>
    );
};

export default IntegrationInstanceConfigurationsFilterTitle;
