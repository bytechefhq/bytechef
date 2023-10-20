import Button from 'components/Button/Button';
import {ProjectExecutionModel} from 'middleware/project';

const ReadOnlyWorkflow = ({execution}: {execution: ProjectExecutionModel}) => {
    const {instance, project, workflow} = execution;

    return (
        <div className="w-96 bg-white p-4">
            <h3>
                {workflow?.label
                    ? `${project?.name} / ${instance?.name} / ${workflow?.label}`
                    : 'No data to show'}
            </h3>

            <Button label="Edit" className="ml-4" />
        </div>
    );
};

export default ReadOnlyWorkflow;
