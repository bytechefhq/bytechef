import { SidebarContentLayout } from '../../components/Layouts/SidebarContentLayout'
import { IntegrationList } from './IntegrationList'
import { Button } from '../../components/Button/Button'

export const Integrations = () => {
  return (
    <SidebarContentLayout
      title={'Integrations'}
      subTitle={'All Integrations'}
      topRight={<Button title={'New Integration'} />}
    >
      <IntegrationList />
    </SidebarContentLayout>
  )
}
