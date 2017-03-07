<template>
  <div>
    <div ref="scrollContainer" class="scrollContainer">
      <svg ref="canvas" :width="width" :height="height">
        <g ref="grid" class="grid"></g>
        <g ref="streams" :class="stream.name" v-for="(stream, index) in streams">
          <component :is="streamComponents[streamSettings[stream.name].style]"
                     :stream="stream"
                     :height="streamHeight"
                     :width="width"
                     :xScale="xScale"
                     :color="getColor(index)">
          </component>
        </g>
        <g class="clockIndicator">
          <line :x1="xScale(clock)" y1="0" :x2="xScale(clock)" y2="100%" class="current"></line>
          <line ref="cursor" y1="0" y2="100%" class="cursor"></line>
          <text v-for="(current, i) in currentComputedValues"
                v-if="current !== null"
                :x="xScale(clock) + 10"
                :y="i * streamWithAxisHeight"
                class="currentValue"
                dy="2em">
            {{ current }}
          </text>
        </g>
        <g ref="axis" v-for="stream in streams" class="axis"></g>
      </svg>
    </div>
    <div class="controls">
      <button @click="zoom(-1)" class="ui basic icon button">
        <i class="zoom out icon"></i>
      </button>
      <button @click="zoom(1)" class="ui basic icon button">
        <i class="zoom icon"></i>
      </button>
    </div>
  </div>
</template>

<script>
  import * as d3 from 'd3'

  import { mapState, mapGetters } from 'vuex'

  import { Styles } from '../constants'

  import StepSignal from './StepSignal'
  import SmoothSignal from './SmoothSignal'
  import LifelineStream from './LifelineStream'
  import SimpleEvents from './SimpleEvents'
  import BubbleEvents from './BubbleEvents'
  import DiscreteEvents from './DiscreteEvents'

  const DOMAIN_PADDING = 10
  const T_WIDTH = 20
  const AXIS_HEIGHT = 14
  const STREAM_HEIGHT = 135

  export default {
    name: 'stream-visualization',
    components: {
      StepSignal, SmoothSignal, LifelineStream, SimpleEvents, BubbleEvents, DiscreteEvents
    },
    data: () => ({
      zoomFactor: 1,
      streamComponents: {
        [Styles.STEP]: StepSignal,
        [Styles.SMOOTH]: SmoothSignal,
        [Styles.LIFELINE]: LifelineStream,
        [Styles.SIMPLE]: SimpleEvents,
        [Styles.BUBBLE]: BubbleEvents,
        [Styles.DISCRETE]: DiscreteEvents
      }
    }),
    mounted () {
      this.render()
      this.addClockIndicatorHooks()
    },
    updated () {
      this.render()
    },
    beforeDestroy () {
      this.removeClockIndicatorHooks()
    },
    computed: {
      ...mapState([
        'streamSettings'
      ]),
      ...mapGetters([
        'streams',
        'currentComputedValues'
      ]),
      clock: {
        get: function () {
          return this.$store.state.clock
        },
        set: function (value) {
          this.$store.commit('updateClock', value)
        }
      },
      tWidth () {
        return T_WIDTH * this.zoomFactor
      },
      streamHeight () {
        return STREAM_HEIGHT
      },
      streamWithAxisHeight () {
        return this.streamHeight + AXIS_HEIGHT
      },
      minDomainWidth () {
        return Math.floor(Math.max(window.screen.width, window.screen.height) / this.tWidth)
      },
      xScale () {
        return d3.scaleLinear()
                 .domain([this.minT - DOMAIN_PADDING, this.maxT + DOMAIN_PADDING])
                 .range([0, this.width])
      },
      minT () {
        let firstValue = this.$store.getters.firstValue
        return firstValue ? firstValue.t : 0
      },
      maxT () {
        let lastValue = this.$store.getters.lastValue
        if (lastValue && lastValue.t - this.minT >= this.minDomainWidth) {
          return lastValue.t
        } else {
          return this.minT + this.minDomainWidth
        }
      },
      domainWidth () {
        return this.maxT - this.minT + 2 * DOMAIN_PADDING
      },
      width () {
        return this.domainWidth * this.tWidth
      },
      height () {
        return this.streams.length * this.streamWithAxisHeight
      }
    },
    watch: {
      clock (t) {
        if (this.$refs.scrollContainer) {
          this.$refs.scrollContainer.scrollLeft =
            this.xScale(t) - this.$refs.scrollContainer.clientWidth / 3
        }
      }
    },
    methods: {
      render () {
        // Draw the grid
        d3.select(this.$refs.grid)
          .attr('transform', 'translate(-0.5, 0)')
          .call(
            d3.axisBottom(this.xScale)
              .ticks(this.domainWidth)
              .tickSize(this.height)
              .tickFormat(() => null)
          )
        // Generate one time-axis per stream and move it to the right place
        d3.selectAll(this.$refs.axis)
          .each((data, i, group) => {
            d3.select(group[i])
              .attr('transform', 'translate(-0.5, ' + (i * this.streamWithAxisHeight) + ')')
          })
          .call(selection => {
            // Add a rect to serve as a stylable background
            selection.selectAll('rect').data([null]).enter()
                     .append('rect')
                     .attr('width', '100%').attr('height', AXIS_HEIGHT)
          })
          .call(
            d3.axisBottom(this.xScale)
              .ticks((this.domainWidth / 10) * this.zoomFactor)
              .tickSize(0)
              .tickPadding(2)
          )
        // The actual streams are rendered by the StreamSvg component.
        // We'll only move them to the right position here.
        let streamRefs = this.$refs.streams || []
        d3.selectAll(streamRefs)
          .each((data, i, group) => {
            d3.select(group[i])
              .attr('transform', 'translate(0, ' + (AXIS_HEIGHT + (i * this.streamWithAxisHeight)) + ')')
          })
      },
      getColor (i) {
        let colors = ['steelblue', '#2ecc40', '#85144b', '#3d9970', '#001f3f']
        return colors[i % colors.length]
      },
      zoom (x) {
        let zoomFactors = [0.1, 0.25, 0.5, 1, 1.5, 2, 3, 4.5]
        let current = zoomFactors.indexOf(this.zoomFactor)
        let next = current + x
        if (next < 0 || next >= zoomFactors.length) {
          return
        }
        this.zoomFactor = zoomFactors[next]
        // Adjust scroll position accordingly
        this.$refs.scrollContainer.scrollLeft -= this.tWidth * this.zoomFactor
      },
      addClockIndicatorHooks () {
        this.$refs.canvas.addEventListener('click', this.onCanvasClick)
        this.$refs.canvas.addEventListener('mousemove', this.onCanvasMouseMove)
        this.$refs.canvas.addEventListener('mouseenter', this.showCursor)
        this.$refs.canvas.addEventListener('mouseleave', this.hideCursor)
      },
      removeClockIndicatorHooks () {
        this.$refs.canvas.removeEventListener('click', this.onCanvasClick)
        this.$refs.canvas.removeEventListener('mousemove', this.onCanvasMouseMove)
        this.$refs.canvas.removeEventListener('mouseenter', this.showCursor)
        this.$refs.canvas.removeEventListener('mouseleave', this.hideCursor)
      },
      onCanvasClick (ev) {
        let t = Math.round(this.xScale.invert(ev.offsetX))
        this.clock = t
      },
      onCanvasMouseMove (ev) {
        let t = Math.round(this.xScale.invert(ev.offsetX))
        d3.select(this.$refs.cursor)
          .attr('x1', this.xScale(t))
          .attr('x2', this.xScale(t))
      },
      hideCursor () {
        this.$refs.cursor.style.opacity = 0
      },
      showCursor () {
        this.$refs.cursor.style.opacity = null
      }
    }
  }
</script>

<style lang="scss">
  /* Styling of elements that do not hve the data attribute used for scoping */
  .axis {
    font-family: inherit;
    font-size: inherit;

    rect {
      fill: white;
    }
    .tick {
      fill-opacity: .5;
    }
    .domain {
      stroke-opacity: 0;
    }
  }
  .grid {
    .domain {
      fill: steelblue;
      opacity: 0.1;
      stroke-opacity: 0;
    }
    .tick line {
      stroke: white;
    }
  }
</style>

<style lang="scss" scoped>
  .scrollContainer {
    overflow: auto;
    height: 100%;
    width: 100%;
  }
  .controls {
    position: absolute;
    top: 1rem;
    right: 1rem;
    button {
      margin: 0;
    }
  }
  .clockIndicator {
    .current, .cursor {
      stroke: red;
    }
    .cursor {
      opacity: .25;
    }
    .currentValue {
      fill: #666
    }
  }
</style>
