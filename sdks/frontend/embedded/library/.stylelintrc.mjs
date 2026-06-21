/** @type {import('stylelint').Config} */

export default {
  extends: ['stylelint-config-standard'],
  rules: {
    // Regex for BEM classes: https://gist.github.com/Potherca/f2a65491e63338659c3a0d2b07eee382
    'selector-class-pattern': '^.[a-z]([a-z0-9-]+)?(__([a-z0-9]+-?)+)?(--([a-z0-9]+-?)+){0,2}$',
  },
}
