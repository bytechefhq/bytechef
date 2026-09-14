import { NextRequest, NextResponse } from 'next/server';
import { isMarkdownPreferred, rewritePath } from 'fumadocs-core/negotiation';
import { applyNegotiatedVary } from '@/lib/agent/content-negotiation';

const { rewrite: rewriteLLM } = rewritePath('/*path', '/llms.mdx/*path');

export default function proxy(request: NextRequest) {
  if (isMarkdownPreferred(request)) {
    const result = rewriteLLM(request.nextUrl.pathname);

    if (result) {
      const response = NextResponse.rewrite(new URL(result, request.nextUrl));

      applyNegotiatedVary(response.headers);

      return response;
    }
  }

  // Both representations live at the same URL, so caches must key on Accept. Next replaces Vary
  // on HTML page responses with its own list, so this is guaranteed only on the markdown variant.
  const response = NextResponse.next();

  applyNegotiatedVary(response.headers);

  return response;
}
