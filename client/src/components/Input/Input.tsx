import { ExclamationCircleIcon } from '@heroicons/react/24/outline'
import cx from 'classnames'

type InputProps = {
  label: string
  name: string
  type?: string
  error?: string | undefined
} & React.DetailedHTMLProps<
  React.InputHTMLAttributes<HTMLInputElement>,
  HTMLInputElement
>

export const Input = ({
  label,
  name,
  type = 'text',
  error,
  ...props
}: InputProps) => {
  return (
    <>
      <div>
        <label
          htmlFor={name}
          className="block text-sm font-medium text-gray-700"
        >
          {label}
        </label>
        <div
          className={cx([
            'mt-1 ',
            error ? 'relative rounded-md shadow-sm' : null
          ])}
        >
          <input
            className={cx([
              'block w-full rounded-md sm:text-sm ',
              error
                ? 'border-red-300 pr-10 text-red-900 placeholder-red-300 focus:border-red-500 focus:outline-none focus:ring-red-500'
                : 'border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500'
            ])}
            id={name}
            name={name}
            type={type}
            {...props}
          />
          {error ? (
            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
              <ExclamationCircleIcon
                className="h-5 w-5 text-red-500"
                aria-hidden="true"
              />
            </div>
          ) : null}
        </div>
        {error ? (
          <p
            role="alert"
            className="mt-2 text-sm text-red-600"
            id={name + '-error'}
          >
            {error}
          </p>
        ) : null}
      </div>
    </>
  )
}
