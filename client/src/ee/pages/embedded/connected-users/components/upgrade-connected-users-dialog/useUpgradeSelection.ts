import {useState} from 'react';

interface SelectionI {
    parentId?: string;
    targetId?: string;
}

interface UseUpgradeSelectionResultI {
    parentId?: string;
    selectParent: (parentId: string) => void;
    selectTarget: (targetId: string) => void;
    targetId?: string;
}

export default function useUpgradeSelection(parentIds: string[]): UseUpgradeSelectionResultI {
    const [selection, setSelection] = useState<SelectionI>({});

    const parentId = selection.parentId || (parentIds.length === 1 ? parentIds[0] : undefined);

    const selectParent = (nextParentId: string) => setSelection({parentId: nextParentId});

    const selectTarget = (targetId: string) => setSelection({parentId, targetId});

    return {parentId, selectParent, selectTarget, targetId: selection.targetId};
}
