import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

export default function getAvailableComponentVersions({
    componentDefinitionVersions,
    nodeVersion,
}: {
    componentDefinitionVersions?: Array<ComponentDefinitionBasic>;
    nodeVersion: string;
}): Array<number> {
    const versions = new Set((componentDefinitionVersions ?? []).map(({version}) => version));

    if (nodeVersion !== '' && !Number.isNaN(Number(nodeVersion))) {
        versions.add(Number(nodeVersion));
    }

    return Array.from(versions).sort((firstVersion, secondVersion) => firstVersion - secondVersion);
}
