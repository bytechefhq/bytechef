import { EllipsisVerticalIcon } from '@heroicons/react/20/solid'
import { useGetIntegrations } from '../../queries/integrations.queries'
import { PropsWithChildren } from 'react'

export const IntegrationList: React.FC<PropsWithChildren> = () => {
  const { isLoading, error, data: items } = useGetIntegrations()

  if (isLoading) return <div>Loading...</div>

  if (error) return <div>An error has occurred: + {error.message}</div>

  return (
    <div>
      <ul role="list" className="divide-y divide-gray-100 dark:divide-gray-800">
        {items.map((item) => (
          <li key={item.id}>
            <a
              href={''}
              className="block hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <div className="flex items-center py-4">
                <div className="flex min-w-0 flex-1 items-center">
                  <div className="min-w-0 flex-1 md:grid md:grid-cols-2 md:gap-4">
                    <div>
                      <p className="truncate text-sm font-medium text-black dark:text-sky-400">
                        {item.name}
                      </p>
                      <p className="mt-2 flex items-center text-sm text-gray-500 dark:text-gray-300">
                        <span className="truncate">{item.description}</span>
                      </p>
                    </div>
                    <div className="hidden md:block">
                      <div>
                        <p className="text-sm text-gray-900">
                          {/*Applied on <time dateTime={item.createdDate}>{item.createdDate}</time>*/}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                <div>
                  <EllipsisVerticalIcon
                    className="h-5 w-5 text-gray-400"
                    aria-hidden="true"
                  />
                </div>
              </div>
            </a>
          </li>
        ))}
      </ul>
    </div>
  )
}
