import {Type} from '@/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations';
import {Integration, Tag} from '@/shared/middleware/embedded/configuration';
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
            <span className="text-sm uppercase text-muted-foreground">{`Filter by ${searchParams.get('tagId') ? 'tag' : 'integration'}:`}</span>

            <span className="text-base">{pageTitle ?? 'All Integrations'}</span>
        </div>
    );
};

export default IntegrationInstanceConfigurationsFilterTitle;
