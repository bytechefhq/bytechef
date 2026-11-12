import {useEffect} from 'react';

const warnedComponentNames = new Set<string>();

export default function useConnectionDialogAllowedDeprecation(
    componentName: string,
    connectionDialogAllowed: boolean | undefined
) {
    useEffect(() => {
        if (connectionDialogAllowed === undefined || warnedComponentNames.has(componentName)) {
            return;
        }

        warnedComponentNames.add(componentName);

        console.warn(
            `ByteChef ${componentName}: the connectionDialogAllowed prop is deprecated and will be removed in the next ` +
                'major release. Set the connectionDialogAllowed claim in the embed JWT instead. Until then, the prop ' +
                'can only remove the permission the token grants.'
        );
    }, [componentName, connectionDialogAllowed]);
}
