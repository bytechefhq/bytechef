import {useGetWorkspaceGitConfigurationQuery} from '@/ee/queries/gitConfiguration.queries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import GitConfigurationForm from '@/pages/platform/settings/git-configuration/components/GitConfigurationForm';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import React from 'react';

const GitConfiguration = () => {
    const {currentWorkspaceId} = useWorkspaceStore();

    const {data: gitConfiguration} = useGetWorkspaceGitConfigurationQuery(currentWorkspaceId!);

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="Git Configuration" />}
            leftSidebarOpen={false}
        >
            <div className="w-full p-4 2xl:mx-auto 2xl:w-4/5">
                <div className="max-w-xl divide-y divide-muted">
                    <GitConfigurationForm gitConfiguration={gitConfiguration} />
                </div>
            </div>
        </LayoutContainer>
    );
};

export default GitConfiguration;
