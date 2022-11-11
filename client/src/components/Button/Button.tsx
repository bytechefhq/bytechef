import { PropsWithChildren } from 'react'

type Props = {
  title: string
}

export const Button: React.FC<PropsWithChildren<Props>> = ({ title }) => {
  return (
    <button
      type="button"
      className="inline-flex items-center rounded-md border border-transparent bg-black px-2 py-1 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:bg-sky-500 dark:hover:bg-sky-400"
    >
      {title}
    </button>
  )
}
