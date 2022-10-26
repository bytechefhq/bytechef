import { PropsWithChildren, ReactNode } from 'react'

type Props = {
  title: string
  topRight?: ReactNode
}

export const MainLayout: React.FC<PropsWithChildren<Props>> = ({
  title,
  topRight,
  children
}) => {
  return (
    <>
      <div className="flex justify-center">
        <div className="flex w-full justify-between py-4 sm:px-6 xl:px-8">
          <h1 id="primary-heading" className="text-2xl dark:text-white">
            {title}
          </h1>

          {topRight}
        </div>
      </div>

      <main className="flex flex-1 overflow-hidden">
        {/* Primary column */}

        <section
          aria-labelledby="primary-heading"
          className="flex h-full min-w-0 flex-1 flex-col overflow-y-auto lg:order-last"
        >
          {/*<h1 id="primary-heading" className="sr-only">*/}
          {/*  Account*/}
          {/*</h1>*/}
          {/* Your content */}

          <div className="flex justify-center">
            <div className="w-full py-8 sm:px-6 xl:px-8">{children}</div>
          </div>
        </section>

        {/*Secondary column (hidden on smaller screens)*/}
        {/*<aside className="hidden lg:order-first lg:block lg:flex-shrink-0">*/}
        {/*  <div className="relative flex h-full w-96 flex-col overflow-y-auto border-l border-r border-gray-50 bg-white">*/}
        {/*    /!* Your content *!/*/}
        {/*  </div>*/}
        {/*</aside>*/}
      </main>
    </>
  )
}
