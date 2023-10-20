import NewIntegrationButton from './NewIntegrationButton'
import './styles.css'

export const PageHeader = () => {
  const onClickHandler = () => {
    // some comment
    console.log('inside onClickHandler')
  }

  return (
    <div className="mb-6 flex justify-center py-4">
      <div className="flex w-full items-center justify-between">
        <h2 className="text-2xl tracking-tight text-gray-900 dark:text-gray-200">
          All integrations
        </h2>

        <div>
          {/* <button onClick={onClickHandler} className="button">
            New integration
          </button> */}

          <NewIntegrationButton />
        </div>
      </div>
    </div>
  )
}
