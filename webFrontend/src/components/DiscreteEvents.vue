<template>
  <g>
    <path ref="baseline" :d="baseline" class="baseline"></path>
    <g v-for="value in stream.values">
      <line :stroke="color"
            :x1="xScale(value.t)" :y1="yScale(0)"
            :x2="xScale(value.t)" :y2="yScale(value.v)"
            class="bar">
      </line>
      <circle r="3" fill="black" stroke="black"
              :cx="xScale(value.t)" :cy="yScale(value.v)">
      </circle>
      <title>{{value.v}}</title>
    </g>
  </g>
</template>

<script>
  import * as d3 from 'd3'

  import SvgStreamBase, { YScale } from './SvgStreamBase'

  export default {
    name: 'discrete-events',
    mixins: [SvgStreamBase, YScale],
    mounted () {
      d3.select(this.$el)
        .selectAll('g')
        .style('opacity', 0)
        .transition()
        .duration(750)
        .style('opacity', 1)
    }
  }
</script>

<style lang="scss" scoped>
  .baseline {
    stroke: #666;
  }
  .bar {
    stroke-width: 2;
  }
</style>
