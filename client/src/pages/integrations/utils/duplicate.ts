import {IntegrationModel} from 'middleware/integration';

export default function duplicate(
    integrationItem: IntegrationModel,
    integrationNames: string[],
    duplicateMutation: {
        mutate: (object: IntegrationModel) => void;
    }
): void {
    let addendum = 1;

    while (
        integrationNames.includes(`${integrationItem?.name} (${addendum})`)
    ) {
        addendum++;
    }

    duplicateMutation.mutate({
        ...integrationItem,
        id: undefined,
        name: `${integrationItem?.name} (${addendum})`,
        version: undefined,
    } as IntegrationModel);
}
