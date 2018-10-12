import { render, h } from 'preact';
import { trackProjectOpened, Switcher } from './switcher';
import { App } from './search';
import { isNSPage, isDocPage, initSrollIndicator, initToggleRaw, initDocTitle } from './cljdoc';

console.log(h('div', {className: "test"}, "conten"))

trackProjectOpened()
render(h(Switcher), document.querySelector('#cljdoc-switcher'))
render(h(App), document.querySelector('#cljdoc-search'))


if (isNSPage()) {
  initSrollIndicator()
  initToggleRaw()
}
if (isDocPage()) {
  console.log('isdocpage yes')
  initDocTitle()
}
