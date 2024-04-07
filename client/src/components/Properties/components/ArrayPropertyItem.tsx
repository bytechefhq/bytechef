import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {ControlTypeModel, WorkflowModel} from '@/middleware/platform/configuration';
import {ArrayPropertyType, ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';

import Property from '../Property';
import DeletePropertyButton from './DeletePropertyButton';

interface ArrayPropertyItemProps {
    arrayItem: ArrayPropertyType;
    arrayName?: string;
    currentComponent?: CurrentComponentType;
    currentComponentData?: ComponentDataType;
    dataPills?: DataPillType[];
    index: number;
    path?: string;
    setArrayItems: React.Dispatch<React.SetStateAction<Array<ArrayPropertyType | Array<ArrayPropertyType>>>>;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const ArrayPropertyItem = ({
    arrayItem,
    arrayName,
    currentComponent,
    currentComponentData,
    dataPills,
    index,
    path,
    setArrayItems,
    updateWorkflowMutation,
}: ArrayPropertyItemProps) => (
    <div className="ml-2 flex w-full items-center border-l pb-2 last-of-type:pb-0" key={arrayItem.name}>
        <Property
            arrayIndex={index}
            arrayName={arrayName}
            currentComponent={currentComponent}
            currentComponentData={currentComponentData}
            customClassName="pl-2 w-full"
            dataPills={dataPills}
            path={`${path}.${arrayName}`}
            property={arrayItem as PropertyType & {controlType?: ControlTypeModel; defaultValue?: string}}
            updateWorkflowMutation={updateWorkflowMutation}
        />

        {arrayItem.custom && arrayName && arrayItem.name && currentComponentData && updateWorkflowMutation && (
            <DeletePropertyButton
                currentComponentData={currentComponentData}
                handleDeletePropertyClick={() =>
                    setArrayItems((subProperties) =>
                        subProperties.filter((_subProperty, subPropertyIndex) => subPropertyIndex !== index)
                    )
                }
                objectProperty={false}
                propertyName={arrayName}
                subPropertyIndex={index}
                updateWorkflowMutation={updateWorkflowMutation}
            />
        )}
    </div>
);

export default ArrayPropertyItem;
