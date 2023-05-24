import {ProjectInstanceModel} from 'middleware/automation/project';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {UseFormGetValues} from 'react-hook-form';

import InstanceDialogWorkflowListItem from './InstanceDialogWorkflowListItem';

const InstanceDialogWorkflowList = (props: {
    getValues: UseFormGetValues<ProjectInstanceModel>;
}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(
        props.getValues().projectId!
    );

    return (
        <div>
            {workflows &&
                workflows?.map((workflowModel) => (
                    <InstanceDialogWorkflowListItem
                        key={workflowModel.id!}
                        workflowModels={workflowModel}
                    />
                ))}
        </div>
    );
};

export default InstanceDialogWorkflowList;
