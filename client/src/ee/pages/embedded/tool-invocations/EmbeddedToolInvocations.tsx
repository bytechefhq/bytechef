import {ToolInvocations} from '@/pages/automation/tool-invocations/ToolInvocations';
import {PlatformType} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

export const EmbeddedToolInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    return (
        <ToolInvocations environmentId={currentEnvironmentId} key={currentEnvironmentId} type={PlatformType.Embedded} />
    );
};
