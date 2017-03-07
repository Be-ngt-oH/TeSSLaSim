<template>
  <g :transform="'translate(0, ' + height / 2 + ')'">
    <defs>
      <clipPath :id="'circleClip' + stream.id">
        <circle :r="Math.max(radius - 2, 0)"></circle>
      </clipPath>
    </defs>

    <path ref="baseline" :d="baseline" :stroke="color"></path>

    <g v-for="value in stream.values"
       :transform="'translate(' + xScale(value.t) + ', 0)'"
       text-anchor="middle">
      <circle :stroke="color" :r="radius"></circle>
      <text v-if="stream.valueType !== Types.UNIT"
            :clip-path="'url(#circleClip' + stream.id + ')'"
            dy="0.3em">
        {{ value.v }}
      </text>
      <title v-if="stream.valueType !== Types.UNIT">
        {{ value.v }}
      </title>
    </g>
  </g>
</template>

<script>
  import * as d3 from 'd3'

  import SvgStreamBase from './SvgStreamBase'

  import { Types } from '../constants'

  export default {
    name: 'bubble-events',
    mixins: [SvgStreamBase],
    mounted () {
      this.updateRadius(true)
    },
    data: () => ({
      radius: 0,
      Types
    }),
    computed: {
      baseline () {
        let path = d3.path()
        path.moveTo(0, 0)
        for (let value of this.stream.values) {
          path.lineTo(this.xScale(value.t) - this.radius, 0)
          path.moveTo(this.xScale(value.t) + this.radius, 0)
        }
        path.lineTo(this.width, 0)

        return path.toString()
      }
    },
    methods: {
      updateRadius (animate = false) {
        let minT = this.xScale.domain()[0]
        let radius = this.xScale(minT + 0.5)

        if (!animate) {
          this.radius = radius
          return
        }

        let radiusInterpolator = d3.interpolateNumber(0, radius)
        let timer = d3.timer(elapsed => {
          let duration = 250
          let t = elapsed / duration
          if (elapsed >= duration) {
            timer.stop()
            t = 1
          }
          this.radius = radiusInterpolator(t)
        })
      }
    },
    watch: {
      xScale () {
        this.updateRadius()
      }
    }
  }
</script>

<style lang="scss" scoped>
  circle {
    stroke-width: 2;
    fill: transparent;
  }
  text {
    font-size: 1em;
    cursor: default;
    fill: #666;
    stroke: #666;
  }
</style>
