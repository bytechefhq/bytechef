import {Type} from '@/pages/automation/project-deployments/ProjectDeployments';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {PlatformType, useMcpServerTagsQuery} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

const McpServersLeftSidebarNav = () => {
    const [searchParams] = useSearchParams();

    const projectId = searchParams.get('projectId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: projectId ? projectId : tagId ? tagId : undefined,
        type: tagId ? Type.Tag : Type.Project,
    };

    const {data, isLoading: tagsIsLoading} = useMcpServerTagsQuery({type: PlatformType.Automation});

    if (!data || !data?.mcpServerTags) {
        return <></>;
    }

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        {!tagsIsLoading &&
                            (data.mcpServerTags.length ? (
                                data.mcpServerTags.map((item) => (
                                    <LeftSidebarNavItem
                                        icon={<TagIcon className="mr-1 size-4" />}
                                        item={{
                                            current: filterData?.id === item!.id && filterData.type === Type.Tag,
                                            id: item!.id,
                                            name: item!.name,
                                        }}
                                        key={item!.id}
                                        toLink={`?tagId=${item!.id}`}
                                    />
                                ))
                            ) : (
                                <span className="px-3 text-xs">No defined tags.</span>
                            ))}
                    </>
                }
                title="Tags"
            />
        </>
    );
};

export default McpServersLeftSidebarNav;
