import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useState} from 'react';

export default function useMcpComponentListItem(componentName: string, componentVersion: number) {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName,
        componentVersion,
    });

    return {
        componentDefinition,
        setShowEditDialog,
        showEditDialog,
    };
}
