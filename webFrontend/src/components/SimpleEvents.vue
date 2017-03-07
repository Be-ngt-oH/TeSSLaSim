<template>
  <g :transform="'translate(0, ' + height / 2 + ')'">
    <path ref="baseline" :d="'M0 0 H' + width" :stroke="color"></path>

    <g v-for="value in stream.values"
       :transform="'translate(' + xScale(value.t) + ', 0)'"
       text-anchor="middle">
      <text v-if="stream.valueType !== Types.UNIT"
            y="-15">
            {{ value.v }}
      </text>
      <path d="M-7 0 H7 M0 -7 V7" class="marks" transform="rotate(45)" :stroke="color"></path>
      <title v-if="stream.valueType !== Types.UNIT"> {{ value.v }}</title>
    </g>
  </g>
</template>

<script>
  import * as d3 from 'd3'

  import SvgStreamBase from './SvgStreamBase'

  import { Types } from '../constants'

  export default {
    name: 'simple-events',
    mixins: [SvgStreamBase],
    mounted () {
      this.animate()
    },
    data: () => ({
      Types
    }),
    methods: {
      animate () {
        d3.select(this.$el)
          .selectAll('text, .marks')
          .style('opacity', 0)
          .transition()
          .duration(750)
          .style('opacity', 1)
      }
    }
  }
</script>

<style lang="scss" scoped>
  text {
    font-size: 1em;
    cursor: default;
    fill: #666;
    stroke: #666;
  }
  .marks {
    stroke-width: 2;
  }
</style>
