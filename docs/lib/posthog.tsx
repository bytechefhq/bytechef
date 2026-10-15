import {usePathname, useSearchParams} from "next/navigation";
import type {PostHog} from "posthog-js";
import {Suspense, useEffect, type ReactNode} from "react";

let postHogPromise: Promise<PostHog> | undefined
let lastCapturedUrl: string | undefined

function loadPostHog(): Promise<PostHog> {
  postHogPromise ??= import("posthog-js").then(({default: posthog}) => {
    posthog.init('phc_oZ9XWAVf8xf6sI7IYBAbGldSbiud07ioZItD1XvhJ7S', {
      api_host: 'https://eu.i.posthog.com',
      person_profiles: 'identified_only', // or 'always' to create profiles for anonymous users as well
      capture_pageview: false,
      capture_pageleave: true
    })

    return posthog
  }).catch((error: unknown) => {
    postHogPromise = undefined

    throw error
  })

  return postHogPromise
}

function whenLoaded(callback: () => void): void {
  if (document.readyState === "complete") {
    callback()
  } else {
    window.addEventListener("load", callback, {once: true})
  }
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

    if (url === lastCapturedUrl) {
      return
    }

    lastCapturedUrl = url

    whenLoaded(() => {
      loadPostHog()
        .then((posthog) => posthog.capture('$pageview', { '$current_url': url }))
        .catch((error: unknown) => console.warn('PostHog failed to load', error))
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
