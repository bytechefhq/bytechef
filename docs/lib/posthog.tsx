import {usePathname, useSearchParams} from "next/navigation";
import type {PostHog} from "posthog-js";
import {Suspense, useEffect, type ReactNode} from "react";

let postHogPromise: Promise<PostHog> | undefined

// posthog-js is a quarter of a megabyte; fetching it once the browser is idle keeps it out of every page's
// first-load JavaScript, which is what decides how soon the page can paint.
function loadPostHog(): Promise<PostHog> {
  postHogPromise ??= import("posthog-js").then(({default: posthog}) => {
    posthog.init('phc_oZ9XWAVf8xf6sI7IYBAbGldSbiud07ioZItD1XvhJ7S', {
      api_host: 'https://eu.i.posthog.com',
      person_profiles: 'identified_only', // or 'always' to create profiles for anonymous users as well
      capture_pageview: false,
      capture_pageleave: true
    })

    return posthog
  })

  return postHogPromise
}

function whenIdle(callback: () => void): () => void {
  if (typeof window.requestIdleCallback === "function") {
    const handle = window.requestIdleCallback(callback, {timeout: 4000})

    return () => window.cancelIdleCallback(handle)
  }

  const handle = window.setTimeout(callback, 1)

  return () => window.clearTimeout(handle)
}

function PostHogPageView() : null {
  const pathname = usePathname()
  const searchParams = useSearchParams()

  // Track pageviews
  useEffect(() => {
    if (!pathname) {
      return
    }

    let url = window.origin + pathname
    if (searchParams.toString()) {
      url = url + `?${searchParams.toString()}`
    }

    return whenIdle(() => {
      void loadPostHog().then((posthog) => posthog.capture('$pageview', { '$current_url': url }))
    })
  }, [pathname, searchParams])

  return null
}

export function PostHogProvider({ children }: { children: ReactNode }) {
  return (
    <>
      {/* Wrap this in Suspense to avoid the `useSearchParams` usage above from de-opting the whole app into
          client-side rendering. See: https://nextjs.org/docs/messages/deopted-into-client-rendering */}
      <Suspense fallback={null}>
        <PostHogPageView />
      </Suspense>
      {children}
    </>
  )
}
