import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Environment} from '@/shared/middleware/graphql';

const EnvironmentLeftSidebarNav = ({
    connectedUserId,
    environment,
}: {
    connectedUserId?: number;
    environment?: Environment;
}) => {
    return (
        <LeftSidebarNav
            body={
                <>
                    {[
                        {label: 'All Environments'},
                        {label: 'Development', value: 1},
                        {label: 'Staging', value: 2},
                        {label: 'Production', value: 3},
                    ]?.map((item) => (
                        <LeftSidebarNavItem
                            item={{
                                current: environment === item.value,
                                id: item.value,
                                name: item.label,
                            }}
                            key={item.value ?? ''}
                            toLink={`?connectedUserId=${connectedUserId ?? ''}&environment=${item.value ?? ''}`}
                        />
                    ))}
                </>
            }
            title="Environments"
        />
    );
};

export default EnvironmentLeftSidebarNav;
