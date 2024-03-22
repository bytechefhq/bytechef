import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {ControlTypeModel, WorkflowModel} from '@/middleware/platform/configuration';
import {ArrayPropertyType, ComponentDataType, CurrentComponentType, DataPillType, PropertyType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';

import Property from '../Property';
import DeletePropertyButton from './DeletePropertyButton';

const ArrayPropertyItem = ({
    arrayItem,
    arrayName,
    currentComponent,
    currentComponentData,
    dataPills,
    index,
    setArrayItems,
    updateWorkflowMutation,
}: {
    arrayItem: ArrayPropertyType;
    arrayName?: string;
    currentComponent?: CurrentComponentType;
    currentComponentData?: ComponentDataType;
    dataPills?: DataPillType[];
    index: number;
    setArrayItems: React.Dispatch<React.SetStateAction<Array<ArrayPropertyType | Array<ArrayPropertyType>>>>;
    updateWorkflowMutation?: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}) => (
    <div className="ml-2 flex w-full border-l pb-2" key={arrayItem.name || `${name}_0`}>
        <Property
            arrayIndex={index}
            arrayName={arrayName}
            currentComponent={currentComponent}
            currentComponentData={currentComponentData}
            customClassName="pl-2 w-full"
            dataPills={dataPills}
            mention={!!dataPills?.length}
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
                propertyName={arrayName}
                subPropertyIndex={index}
                updateWorkflowMutation={updateWorkflowMutation}
            />
        )}
    </div>
);

export default ArrayPropertyItem;
