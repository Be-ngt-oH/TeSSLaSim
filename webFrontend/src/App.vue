<template>
  <div id="app">
    <div id="controlColumn" class="column">
      <simulation-control class="ui raised segment"></simulation-control>
    </div>
    <div id="centerColumn" class="column">
      <div class="ui raised segment">
        <div class="isp-background"></div>

        <stream-visualization v-show="hasStreams" id="streamVisualization"></stream-visualization>
        <div class="example-buttons">
          <button v-for="n in 3" @click="loadExample(n-1)" class="ui basic example button">Example {{n}}</button>
        </div>
        <error-list v-show="hasErrors" id="errorList"></error-list>

        <div :class="{ active: pendingSimulation }" class="ui text loader">
          Waiting for result ...
        </div>
      </div>
    </div>
    <div id="editorColumn" class="column">
      <scenario-editor id="scenarioEditor" class="ui raised segment editor"></scenario-editor>
      <tessla-editor id="tesslaEditor" class="ui raised segment editor"></tessla-editor>
    </div>
    <error-modal ref="errorModal"></error-modal>
  </div>
</template>

<script>
  import { mapState, mapGetters } from 'vuex'

  import { ErrorHandler } from './error'

  import SimulationControl from './components/SimulationControl'
  import ScenarioEditor from './components/ScenarioEditor'
  import TesslaEditor from './components/TesslaEditor'
  import ErrorModal from './components/ErrorModal'
  import ErrorList from './components/ErrorList'
  import StreamVisualization from './components/StreamVisualization'

  import Example1 from './examples/example1'
  import Example2 from './examples/example2'
  import Example3 from './examples/example3'

  export default {
    name: 'app',
    components: {
      SimulationControl, ScenarioEditor, TesslaEditor, ErrorModal, ErrorList, StreamVisualization
    },
    mounted: function () {
      ErrorHandler.displayError = this.$refs.errorModal.display
      this.addSyncScrollHook()
    },
    computed: {
      ...mapState([
        'pendingSimulation',
        'errors'
      ]),
      ...mapGetters([
        'streams'
      ]),
      hasErrors () {
        return this.errors.length > 0
      },
      hasStreams () {
        return this.streams.length > 0
      }
    },
    methods: {
      loadExample (n) {
        let examples = [Example1, Example2, Example3]

        this.$store.commit('updateScenarioSpec', examples[n].scenario)
        this.$store.commit('updateTesslaSpec', examples[n].tessla)
        this.$store.dispatch('triggerRemoteSimulation')
      },
      addSyncScrollHook () {
        // Sync vertical scroll positions of stream-visualization and
        // simulation-control components.
        // This is done here in a straight-forward non-vue-esque fashion,
        // because it's very specific, easy to spot here and easy to change if
        // necessary.
        let [scrollA, scrollB] = this.$el.querySelectorAll(
          '#streamVisualization .scrollContainer, #controlColumn .streamControls'
        )

        let syncScroll = ev => {
          scrollA.scrollTop = ev.target.scrollTop
          scrollB.scrollTop = ev.target.scrollTop
        }

        scrollA.addEventListener('scroll', syncScroll)
        scrollB.addEventListener('scroll', syncScroll)
      }
    }
  }
</script>

<style lang="scss">
  @import '~semantic-ui-css/semantic.css';
  @import 'mixins';

  ::-webkit-scrollbar {
    height: 6px;
    width: 6px;
  }

  ::-webkit-scrollbar-thumb {
    background: #bbb;
    border-radius: 3px;
  }

  html, body {
    @include desktop {
      height: 100%;
      /* More or less arbitrary minimum height */
      min-height: 768px;
    }

    @include smallScreen {
      /* We use a vertical grid with fixed heights on phones/small tablets */
      height: auto;
    }
  }
</style>

<style lang="scss" scoped>
  @import 'mixins';

  /* --- Grid --- */

  #app {
    display: flex;
    flex-wrap: wrap;

    height: 100%;
    padding: 0.5rem 0.5rem 1rem 0.5rem;
  }

  #controlColumn, #editorColumn {
    flex-shrink: 0;
  }

  #centerColumn {
    flex-grow: 1;
  }


  @include desktop {
    .column {
      height: 100%;
    }

    #controlColumn {
      width: 200px;
    }

    #centerColumn {
      padding: 0 1rem 0 0.5rem;
    }

    #editorColumn {
      width: 25%;
    }
  }

  @include smallScreen {
    .column {
      width: 100%;
      padding-bottom: 1rem;
    }

    #controlColumn {
      height: 260px;
    }

    #centerColumn {
      height: 400px;
    }

    #editorColumn {
      height: 600px;
    }
  }

  /* --- Styling --- */

  .segment {
    height: 100%;
  }

  #centerColumn {
    .segment {
      position: relative;
    }

    .isp-background {
      position: absolute;

      width: 80%;
      height: 100%;

      top: calc(50% - 3rem);
      left: 50%;
      transform: translateX(-50%) translateY(-50%);

      background-image: url('./assets/isp-color.png');
      background-repeat: no-repeat;
      background-size: contain;
      background-position: center;

      opacity: .15;
    }

    #errorList, #streamVisualization {
      position: absolute;
      top: 0;
      left: 0;
      height: 100%;
      width: 100%;
    }

    #errorList {
      padding: inherit;
    }
    #streamVisualization {
      $segmentPadding: 1rem;
      $playButtonHeight: 36px;
      $playButtonMarginBot: 2rem;

      padding: 0;
      padding-top: calc(#{$segmentPadding} + #{$playButtonHeight} + #{$playButtonMarginBot / 2})
    }
    .example-buttons {
      position: absolute;
    }
  }

  #editorColumn {
    .editor {
      padding: 0;
      height: calc(50% - 0.5rem);
    }
  }
</style>
