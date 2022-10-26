import {
  Integration,
  IntegrationControllerApi
} from '../_generated/data-access/integration'
import { useQuery } from '@tanstack/react-query'

export enum ServerStateKeysEnum {
  Integrations = 'integrations'
}

export const useGetIntegrations = () =>
  useQuery<Integration[], Error>([ServerStateKeysEnum.Integrations], () =>
    new IntegrationControllerApi().getIntegrations()
  )
