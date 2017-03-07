<template>
  <div class="ui segment">
    <div class="ui form">
      <select v-model="selected" name="visualStyle" class="dropdown">
        <option v-for="style in styles" :value="style.value">{{ style.text }}</option>
      </select>
    </div>

    <div class="meta">
      <a class="name">{{ stream.name }}</a>
      <span class="type">{{ prettyType }}</span>
    </div>

    <div class="stepButtons">
      <button
          @click="stepBackward"
          title="Jump to previous value of this stream"
          class="ui basic icon button">
        <i class="step backward icon"></i>
      </button>
      <button
          @click="stepForward"
          title="Jump to next value of this stream"
          class="ui basic icon button">
        <i class="step forward icon"></i>
      </button>
    </div>
  </div>
</template>

<script>
  import { mapState } from 'vuex'

  import $ from 'jquery'
  import _ from 'lodash'

  import 'semantic-ui-css/components/dropdown'

  import { Styles, Types } from '../constants'

  export default {
    name: 'stream-control',
    props: ['stream'],
    mounted: function () {
      // Initialize dropdowns and add CSS scoping attribute
      let $select = $(this.$el).find('select')
      let $dropdown = $select.dropdown()
      _.merge($dropdown[0].dataset, $select[0].dataset)
    },
    data () {
      return {
        styles: allowedStyles(this.stream)
      }
    },
    computed: {
      ...mapState([
        'streamSettings'
      ]),
      clock: {
        get: function () {
          return this.$store.state.clock
        },
        set: function (value) {
          this.$store.commit('updateClock', value)
        }
      },
      selected: {
        get: function () {
          return this.streamSettings[this.stream.name].style
        },
        set: function (value) {
          this.$store.commit('setStreamStyle', { stream: this.stream, style: value })
        }
      },
      prettyType () {
        let prefix = this.stream.type === Types.SIGNAL ? 'Signal' : 'Events'
        let suffix = this.stream.valueType !== Types.UNIT ? '<' + this.stream.valueType + '>' : ''
        return prefix + suffix
      }
    },
    methods: {
      stepBackward () {
        let lastBefore = this.stream.lastValueBefore(this.clock)
        if (lastBefore) {
          this.clock = lastBefore.t
        }
      },
      stepForward () {
        let firstAfter = this.stream.firstValueAfter(this.clock)
        if (firstAfter) {
          this.clock = firstAfter.t
        }
      }
    }
  }

  function allowedStyles ({type, valueType}) {
    const displayTexts = {
      [Styles.STEP]: 'step function',
      [Styles.SMOOTH]: 'smooth',
      [Styles.LIFELINE]: 'value lifeline',
      [Styles.SIMPLE]: 'simple',
      [Styles.DISCRETE]: 'discrete',
      [Styles.BUBBLE]: 'bubble'
    }

    let styles = []

    if (type === Types.SIGNAL) {
      if (valueType === Types.STRING) {
        styles = [Styles.LIFELINE]
      } else {
        styles = [Styles.STEP, Styles.SMOOTH, Styles.LIFELINE]
      }
    } else if (type === Types.EVENTS) {
      if (valueType === Types.STRING || valueType === Types.UNIT) {
        styles = [Styles.SIMPLE, Styles.BUBBLE]
      } else {
        styles = [Styles.SIMPLE, Styles.DISCRETE, Styles.BUBBLE]
      }
    }

    return styles.map((style) => ({
      value: style,
      text: displayTexts[style]
    }))
  }
</script>

<style lang="scss" scoped>
  .segment {
    padding: 0;

    min-height: 135px;

    .dropdown {
      width: 100%;
      min-width: 0;
    }
  }

  .meta {
    margin-top: .5rem;

    text-align: center;

    .name {
      display: block;
      text-decoration: underline;
    }
    .type {
      display: block;
      color: #AAA;
    }
  }

  .stepButtons {
    position: absolute;
    bottom: 0;
    width: 100%;

    display: flex;

    button {
      margin: 0;
      width: 100%;
    }
  }
</style>
