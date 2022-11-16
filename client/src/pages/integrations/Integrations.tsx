import { SidebarContentLayout } from '../../components/Layouts/SidebarContentLayout'
import { IntegrationList } from './IntegrationList'
import React, { useState } from 'react'
import { PageHeader } from 'components/PageHeader/PageHeader'

type Props = {
  title: string
  subTitle: string
  onClick: React.MouseEventHandler<HTMLButtonElement>
}

export const Integrations = (): JSX.Element => {
  const [clickedButton, setClickedButton] = useState('')

  const buttonHandler = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault()

    const button: HTMLButtonElement = event.currentTarget
    setClickedButton(button.name)
  }

  return (
    <SidebarContentLayout title={'Integrations'}>
      <PageHeader />
      {/* <IntegrationList /> */}
    </SidebarContentLayout>
  )
}
