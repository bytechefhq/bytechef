import {useQuery} from '@tanstack/react-query';
import {Integration, IntegrationsApi} from 'data-access/integration';

export enum ServerStateKeysEnum {
	Integrations = 'integrations',
}

export const useGetIntegrations = () =>
	useQuery<Integration[], Error>([ServerStateKeysEnum.Integrations], () =>
		new IntegrationsApi().getIntegrations()
	);
