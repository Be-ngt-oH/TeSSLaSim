<template>
  <g>
    <path ref="path" :d="path" :stroke="color" class="signal"></path>
    <path v-if="showBaseline" :d="baseline" class="baseline"></path>
  </g>
</template>

<script>
  import * as d3 from 'd3'

  import SvgStreamBase, { YScale, animatePath } from './SvgStreamBase'
  import { Types } from '../constants'

  export default {
    name: 'step-signal',
    mixins: [SvgStreamBase, YScale],
    mounted () {
      animatePath(this.$refs.path)
    },
    computed: {
      path: function () {
        let [minT, maxT] = this.xScale.domain()
        let data = [{
          t: minT,
          v: this.stream.initialValue
        }].concat(this.stream.values).concat([{
          t: maxT,
          v: this.stream.initialValue
        }])

        return d3.line().curve(d3.curveStepAfter)
                 .x(d => this.xScale(d.t))
                 .y(d => this.yScale(d.v))(data)
      },
      showBaseline: function () {
        return this.stream.valueType !== Types.BOOL
      }
    }
  }
</script>

<style lang="scss" scoped>
  .signal {
    stroke-width: 2;
    fill: transparent;
    shape-rendering: crispEdges;
  }
  .baseline {
    stroke: #666;
  }
</style>
