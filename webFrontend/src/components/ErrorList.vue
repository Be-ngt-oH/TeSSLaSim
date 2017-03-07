<template>
  <div class="ui relaxed list">
    <div v-for="error in errors" class="item">
      <div class="ui warning message">
        <a @click="jumpTo(error)" class="header">
          {{ getLabel(error.type) }}
        </a>
        <p>
          {{ error.message }}
        </p>
      </div>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'
  import { mapState } from 'vuex'

  const ErrorTypes = {
    SCENARIO: 'Scenario',
    TESSLA: 'TeSSLa'
  }

  export default {
    name: 'error-list',
    computed: {
      ...mapState([
        'errors'
      ])
    },
    methods: {
      getLabel (type) {
        if (type === ErrorTypes.SCENARIO) {
          return 'Scenario Specification'
        } else {
          return 'TeSSLa Specification'
        }
      },
      jumpTo (error) {
        let editorComponent
        if (error.type === ErrorTypes.SCENARIO) {
          editorComponent = window.scenarioEditor
        } else {
          editorComponent = window.tesslaEditor
        }

        if (!editorComponent) {
          // Editor not found, so we do nothing
          return
        }

        let cmInstance = editorComponent.querySelector('.CodeMirror').CodeMirror
        cmInstance.focus()
        if (_.has(error, 'details.from')) {
          cmInstance.setCursor({
            line: error.details.from.line - 1,
            ch: error.details.from.column - 1
          })
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .list {
    height: 100%;
    overflow: auto;
    margin: 0;
  }

  .warning.message {
    background-color: rgba(255, 240, 223, .6);
  }
</style>
