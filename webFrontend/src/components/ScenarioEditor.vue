<template>
  <div>
    <spec-editor
        :value="scenarioSpec" @input="onInput"
        placeholder="Scenario Specification" mode="scenariodsl">
    </spec-editor>
  </div>
</template>

<script>
  import _ from 'lodash'
  import { mapState } from 'vuex'
  import CodeMirror from 'codemirror'
  import 'codemirror/addon/mode/simple'

  import SpecEditor from './SpecEditor'

  export default {
    name: 'scenario-editor',
    components: {'spec-editor': SpecEditor},
    computed: {
      ...mapState([
        'scenarioSpec'
      ])
    },
    methods: {
      onInput: _.debounce(function (value) {
        if (this.scenarioSpec.trimRight() !== value.trimRight()) {
          this.$store.commit('updateScenarioSpec', value)
          this.$store.dispatch('triggerRemoteSimulation')
        }
      }, 500)
    }
  }

  // This is far from perfect and not finished
  CodeMirror.defineSimpleMode('scenariodsl', {
    start: [
      {regex: /Signal|Events|define/, token: 'def'},
      {regex: /(<)(Boolean|Int|Float|Unit)(>)/, token: ['bracket', 'keyword', 'bracket']},
      {regex: /[-+\/*^=<>!]+|if/, token: 'operator'},
      {regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i, token: 'number'}
    ]
  })
</script>
