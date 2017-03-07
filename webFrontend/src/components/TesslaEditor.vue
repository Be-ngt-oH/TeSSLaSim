<template>
  <div>
    <spec-editor
        :value="tesslaSpec" @input="onInput"
        placeholder="TeSSLa Specification" mode="tessla">
    </spec-editor>
  </div>
</template>

<script>
  import _ from 'lodash'
  import { mapState } from 'vuex'

  import SpecEditor from './SpecEditor'

  export default {
    name: 'tessla-editor',
    components: {'spec-editor': SpecEditor},
    computed: {
      ...mapState([
        'tesslaSpec'
      ])
    },
    methods: {
      onInput: _.debounce(function (value) {
        if (this.tesslaSpec.trimRight() !== value.trimRight()) {
          this.$store.commit('updateTesslaSpec', value)
          this.$store.dispatch('triggerRemoteSimulation')
        }
      }, 500)
    }
  }
</script>
