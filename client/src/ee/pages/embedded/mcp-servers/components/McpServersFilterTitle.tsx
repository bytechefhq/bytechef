import {Type} from '@/ee/pages/embedded/mcp-servers/McpServers';
import FilterTitle from '@/shared/components/filters/FilterTitle';
import {getMcpServersFilter} from '@/shared/components/mcp-server/mcpServersFilter';
import {Tag} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';

interface McpServersFilterTitleProps {
    componentDefinitions: ComponentDefinitionBasic[] | undefined;
    filterData: {id?: string; type: Type};
    tags: Tag[] | undefined;
    uniqueIntegrations: Array<{
        id: string;
        name: string;
    }>;
}

const McpServersFilterTitle = ({
    componentDefinitions,
    filterData,
    tags,
    uniqueIntegrations,
}: McpServersFilterTitleProps) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    const filterId = filterData.id;
    const filterType = filterData.type;

    if (filterId && filterType === Type.Component) {
        const matchedComponent = componentDefinitions?.find(
            (componentDefinition) => componentDefinition.name === filterId
        );

        pageTitle = matchedComponent?.title;
    } else if (filterId && filterType === Type.Integration) {
        const matchedIntegration = uniqueIntegrations.find((integration) => integration.id === filterId);

        pageTitle = matchedIntegration?.name;
    } else if (filterId && filterType === Type.Tag) {
        const matchedTag = tags?.find((tag) => tag.id === filterId);

        pageTitle = matchedTag?.name;
    }

    const filter = getMcpServersFilter({
        groupLabel: 'Integrations',
        groupSearchParamName: 'integrationId',
        pageTitle,
        searchParams,
    });

    return <FilterTitle filters={[filter]} />;
};

export default McpServersFilterTitle;
