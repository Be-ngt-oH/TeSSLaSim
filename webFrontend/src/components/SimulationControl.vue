<template>
  <div class="simulationControl">
    <div class="playButtons">
      <button
          @click="fastBackward"
          title="Jump to previous value of any stream"
          class="ui basic icon button">
        <i class="fast backward icon"></i>
      </button>
      <button v-if="paused" @click="play" class="ui basic icon button">
        <i class="play icon"></i>
      </button>
      <button v-if="!paused" @click="pause" class="ui basic icon button">
        <i class="pause icon"></i>
      </button>
      <button
          @click="fastForward"
          title="Jump to next value of any stream"
          class="ui basic icon button">
        <i class="fast forward icon"></i>
      </button>
    </div>

    <div class="streamControls">
      <!-- https://vuejs.org/v2/guide/list.html#key -->
      <stream-control
          v-for="stream in streams" :key="stream.id"
          :stream="stream"
          class="streamControl">
      </stream-control>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'

  import _ from 'lodash'

  import StreamControl from './StreamControl'

  // Time between clock increments in ms
  const playbackSpeed = 100

  export default {
    name: 'simulation-control',
    components: {'stream-control': StreamControl},
    computed: {
      ...mapGetters([
        'streams',
        'lastValue'
      ]),
      clock: {
        get: function () {
          return this.$store.state.clock
        },
        set: function (value) {
          this.$store.commit('updateClock', value)
        }
      },
      paused () {
        return this.playbackTimer === null
      }
    },
    data: () => ({
      playbackTimer: null
    }),
    methods: {
      fastBackward () {
        let lastBefore = _.maxBy(this.streams.map(s => s.lastValueBefore(this.clock)), 't')
        if (lastBefore) {
          this.clock = lastBefore.t
        }
      },
      fastForward () {
        let firstAfter = _.minBy(this.streams.map(s => s.firstValueAfter(this.clock)), 't')
        if (firstAfter) {
          this.clock = firstAfter.t
        }
      },
      play () {
        if (this.clock === null || this.lastValue === null) {
          return
        }
        this.playbackTimer = setInterval(() => {
          if (this.clock >= this.lastValue.t) {
            this.pause()
            return
          }
          this.clock += 1
        }, playbackSpeed)
      },
      pause () {
        clearInterval(this.playbackTimer)
        this.playbackTimer = null
      }
    }
  }
</script>

<style lang="scss" scoped>
  @import '../mixins';

  $playButtonsHeight: 36px;

  .playButtons {
    display: flex;
    height: $playButtonsHeight;

    button {
      margin: 0;
      width: 100%;
    }
  }

  .streamControls {
    $topMargin: 2rem;
    height: calc(100% - #{$playButtonsHeight} - #{$topMargin});

    margin: $topMargin 0 0 0;

    overflow: auto;

    @include smallScreen {
      white-space: nowrap;

      .streamControl {
        white-space: normal;
      }
    }
  }

  .streamControl {
    display: inline-block;
    margin: 0 0 1rem 0;
    width: 100%;
    height: 135px;

    @include smallScreen {
      width: 170px;
      margin: 0 1rem 0 0;
    }
  }
</style>
