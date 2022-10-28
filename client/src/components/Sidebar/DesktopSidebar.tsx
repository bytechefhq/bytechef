import reactLogo from '../../assets/logo.svg'
import { SVGProps } from 'react'
import { Link } from 'react-router-dom'
import Avatar from '../Avatar/Avatar'

export function DesktopSidebar({
  user,
  navigation
}: {
  user: { name: string; email: string; imageUrl: string }
  navigation: {
    name: string
    href: string
    icon: (props: SVGProps<SVGSVGElement>) => JSX.Element
  }[]
}) {
  return (
    <div className="hidden lg:flex lg:flex-shrink-0">
      <div className="flex w-[64px] flex-col">
        <div className="flex min-h-0 flex-1 flex-col overflow-y-auto bg-gray-200 dark:bg-gray-800">
          <div className="flex-1">
            <div className="flex items-center justify-center py-4">
              <img className="h-8 w-auto" src={reactLogo} alt="ByteChef" />
            </div>
            <nav
              aria-label="Sidebar"
              className="flex flex-col items-center space-y-2 py-3"
            >
              {navigation.map((item) => (
                <Link
                  key={item.name}
                  to={item.href}
                  className="hover:text-primary flex items-center rounded-lg p-4 dark:text-gray-200"
                >
                  <item.icon className="h-7 w-7" aria-hidden="true" />
                  <span className="sr-only">{item.name}</span>
                </Link>
              ))}
            </nav>
          </div>
          <div className="flex flex-shrink-0 justify-center pb-5">
            <Link to={'/settings'}>
              <Avatar />
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
