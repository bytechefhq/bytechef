import GitConfigurationForm from '@/ee/pages/settings/platform/git-configuration/components/GitConfigurationForm';
import {useGetWorkspaceGitConfigurationQuery} from '@/ee/shared/queries/platform/gitConfiguration.queries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import React from 'react';

const GitConfiguration = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: gitConfiguration} = useGetWorkspaceGitConfigurationQuery(currentWorkspaceId!);

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="Git Configuration" />}
            leftSidebarOpen={false}
        >
            <div className="w-full p-4 3xl:mx-auto 3xl:w-4/5">
                <div className="max-w-xl divide-y divide-muted">
                    <GitConfigurationForm gitConfiguration={gitConfiguration} />
                </div>
            </div>
        </LayoutContainer>
    );
};

export default GitConfiguration;
