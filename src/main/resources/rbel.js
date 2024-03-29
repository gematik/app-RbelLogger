/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

document.addEventListener('DOMContentLoaded', function () {
  // Modals
  var rootEl = document.documentElement;
  var $modals = getAll('.modal');
  var $modalButtons = getAll('.modal-button');
  var $modalCloses = getAll(
      '.modal-background, .modal-close, .message-header .delete, .modal-card-foot .button');

  if ($modalButtons.length > 0) {
    $modalButtons.forEach(function ($el) {
      $el.addEventListener('click', function (e) {
        var target = $el.dataset.target;
        var $target = document.getElementById(target);
        rootEl.classList.add('is-clipped');
        $target.classList.add('is-active');
        e.preventDefault();
        return false;
      });
    });
  }

  if ($modalCloses.length > 0) {
    $modalCloses.forEach(function ($el) {
      $el.addEventListener('click', function () {
        closeModals();
      });
    });
  }

  document.addEventListener('keydown', function (event) {
    var e = event || window.event;
    if (e.keyCode === 27) {
      closeModals();
    }
  });

  function closeModals() {
    rootEl.classList.remove('is-clipped');
    $modals.forEach(function ($el) {
      $el.classList.remove('is-active');
    });
  }

  // Functions

  function getAll(selector) {
    return Array.prototype.slice.call(document.querySelectorAll(selector), 0);
  }

  function htmlToElement(html) {
    const template = document.createElement('template');
    html = html.trim(); // Never return a text node of whitespace as the result
    template.innerHTML = html;
    return template.content.firstChild;
  }
});

function toggleCollapsableIcon(target) {
  const classList = target.classList;
  if (classList.contains("fa-toggle-on")) {
    classList.remove("fa-toggle-on");
    classList.add("fa-toggle-off");
  } else {
    classList.add("fa-toggle-on");
    classList.remove("fa-toggle-off");
  }

}

document.addEventListener('DOMContentLoaded', function () {
  let msgCards = document.getElementsByClassName('msg-card');
  for (let i = 0; i < msgCards.length; i++) {
    msgCards[i].children[0].children[0].children[0].children[1].addEventListener('click', e => {
      e.currentTarget
          .parentElement.parentElement.parentElement.parentElement
          .childNodes[1].classList.toggle('is-hidden');
      toggleCollapsableIcon(e.currentTarget);
      e.preventDefault();
      return false;
    });
  }

  document.getElementById("collapse-all").addEventListener('click', e => {
    for (let i = 0; i < msgCards.length; i++) {
      const classList = msgCards[i].childNodes[1].classList;
      if (!classList.contains('is-hidden')) {
        classList.add('is-hidden');
      }
      const classList2 = msgCards[i].children[0].children[0].children[0].children[1].classList;
      if (classList2.contains("fa-toggle-on")) {
        classList2.remove("fa-toggle-on");
        classList2.add("fa-toggle-off");
      }
    }
    e.preventDefault();
    return false;
  });

  document.getElementById("expand-all").addEventListener('click', e => {
    for (let i = 0; i < msgCards.length; i++) {
      const classList = msgCards[i].childNodes[1].classList;
      if (classList.contains('is-hidden')) {
        classList.remove('is-hidden');
      }
      const classList2 = msgCards[i].children[0].children[0].children[0].children[1].classList;
      if (classList2.contains("fa-toggle-off")) {
        classList2.remove("fa-toggle-off");
        classList2.add("fa-toggle-on");
      }
    }
    e.preventDefault();
    return false;
  });
});
