<template>
  <div class="editor" :class="{ fullScreen: isFullScreen }">
    <button @click="toggleFullScreen" class="fullScreen-toggle ui basic icon button"><i class="expand icon"></i></button>
    <!-- CodeMirror elements will be inserted here -->
  </div>
</template>

<script>
  import CodeMirror from 'codemirror'
  import 'codemirror/addon/display/placeholder'
  import 'codemirror/addon/hint/show-hint'
  import 'codemirror/addon/hint/anyword-hint'
  import 'codemirror/addon/scroll/simplescrollbars'

  import _ from 'lodash'

  export default {
    name: 'spec-editor',
    props: ['placeholder', 'mode', 'value'],
    mounted: function () {
      if (!_.isString(this.value)) {
        console.warn('No value prop passed. Make sure this is done, e.g. by using the v-model directive.')
      }

      this.editor = CodeMirror(this.$el, {
        mode: this.mode,
        value: _.isString(this.value) ? this.value : '',

        lineNumbers: true,
        tabSize: 2,
        // Force textarea on mobile (too), because contenteditable is still
        // super buggy on mobile browsers. Surprisingly codemirror defaults to
        // contenteditable on mobile browsers.
        inputStyle: 'textarea',
        placeholder: this.placeholder,
        scrollbarStyle: 'overlay',
        hintOptions: {
          hint: CodeMirror.hint.anyword
        },
        extraKeys: {
          'Ctrl-Space': 'autocomplete',
          'Esc': () => { if (this.isFullScreen) this.toggleFullScreen() },
          'Alt-Enter': this.toggleFullScreen
        }
      })
      // CodeMirror creates its own set of DOM elements. These don't have the
      // data attribute used by Vue for CSS scoping attached. We can fix this!
      // (This is for the top element only)
      _.merge(this.editor.display.wrapper.dataset, this.$el.dataset)

      this.editor.on('change', (cm, ev) => {
        this.$emit('input', this.editor.getValue())
      })
    },
    watch: {
      value (value) {
        // Update the value if it was changed externally in the parent component
        if (value !== this.editor.getValue()) {
          this.editor.setValue(value)
        }
      }
    },
    data: () => ({
      isFullScreen: false
    }),
    methods: {
      toggleFullScreen () {
        this.isFullScreen = !this.isFullScreen
        this.editor.focus()

        // We're manually changing the size of the editor by adding/removing a
        // class via Vue. Vue provides $nextTick which takes a callback that is
        // invoked after rendering finished. We can use this to trigger a
        // refresh on the Editor.
        this.$nextTick(() => {
          this.editor.refresh()
        })
      }
    }
  }
</script>

<style lang="scss">
  @import '~codemirror/lib/codemirror.css';
  @import '~codemirror/addon/hint/show-hint.css';
  @import '~codemirror/addon/scroll/simplescrollbars.css';

  .CodeMirror {
    .CodeMirror-placeholder {
      color: #c7c7cd;
    }
    .CodeMirror-overlayscroll-vertical, .CodeMirror-overlayscroll-horizontal {
      div {
        background: #bbb;
      }
    }
  }

  /* Hints are appended to the body, hence they are styled here */
  .CodeMirror-hints {
    z-index: 500;
  }
</style>

<style lang="scss" scoped>
  .editor {
    position: relative;
    height: 100%;
    width: 100%;
  }

  .fullScreen-toggle {
    position: absolute;
    z-index: 100;
    top: 4px;
    right: 6px;
    margin: 0;
  }
  .CodeMirror {
    z-index: 0;
    height: 100%;
  }

  .fullScreen {
    position: fixed !important;
    top: 0 !important;
    right: 0 !important;
    bottom: 0 !important;
    left: 0 !important;
    height: 100% !important;
    width: 100% !important;
    z-index: 500 !important;
  }
</style>
