import Badge from '@/components/Badge/Badge';
import {Type} from '@/ee/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations';
import {Integration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

const IntegrationInstanceConfigurationsFilterTitle = ({
    filterData,
    integrations,
    tags,
}: {
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
            <span className="text-sm font-semibold uppercase text-muted-foreground">Filter by </span>

            <span className="text-sm uppercase text-muted-foreground">
                {searchParams.get('tagId') ? 'tag' : 'integration'}:
            </span>

            <Badge styleType="secondary-filled" weight="semibold">
                <span className="text-sm">{pageTitle ?? 'All Integrations'}</span>
            </Badge>
        </div>
    );
};

export default IntegrationInstanceConfigurationsFilterTitle;
