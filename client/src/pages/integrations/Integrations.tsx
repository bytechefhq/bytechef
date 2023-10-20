import { MainLayout } from '../../components/Layouts/MainLayout'
import { IntegrationList } from './IntegrationList'
import { Button } from '../../components/Button'

export const Integrations = () => {
  return (
    <MainLayout
      title={'Integrations'}
      topRight={<Button title={'New Integration'} />}
    >
      <IntegrationList />
    </MainLayout>
  )
}
