import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import ConnectedUserProjectWorkflowList from '@/ee/pages/embedded/automation-workflows/components/connected-user-project-workflow-list/ConnectedUserProjectWorkflowList';
import {ConnectedUserProject} from '@/shared/middleware/graphql';

import ConnectedUserProjectListItem from './ConnectedUserProjectListItem';

const ConnectedUserProjectList = ({connectedUserProjects}: {connectedUserProjects: ConnectedUserProject[]}) => {
    return (
        <>
            {connectedUserProjects.map((connectedUserProject) => {
                return (
                    <Collapsible className="group" key={connectedUserProject.id}>
                        <ConnectedUserProjectListItem
                            connectedUserProject={connectedUserProject}
                            key={connectedUserProject.id}
                        />

                        {!!connectedUserProject.connectedUserProjectWorkflows?.length && (
                            <CollapsibleContent>
                                <ConnectedUserProjectWorkflowList
                                    connectedUserProjectWorkflows={connectedUserProject.connectedUserProjectWorkflows}
                                />
                            </CollapsibleContent>
                        )}
                    </Collapsible>
                );
            })}
        </>
    );
};

export default ConnectedUserProjectList;
