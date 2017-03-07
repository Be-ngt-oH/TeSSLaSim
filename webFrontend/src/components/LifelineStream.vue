<template>
  <g :transform="'translate(0, ' + height / 2 + ')'">
    <defs>
      <clipPath v-for="(adjChanges, i) in changePairs" :id="'clip' + stream.id + i">
        <rect :x="xScale(adjChanges[0].t + 0.3)"
              :y="-pathHeight"
              :width="xScale(adjChanges[1].t - 0.3) - xScale(adjChanges[0].t + 0.3)"
              :height="pathHeight * 2">
        </rect>
      </clipPath>
    </defs>

    <g :stroke="color">
      <path ref="bottomPath" :d="path"></path>
      <path ref="topPath" :d="path" transform="scale(1, -1)"></path>
    </g>

    <g :fill="color">
      <text v-for="(adjChanges, i) in changePairs"
            :x="xScale(adjChanges[0].t + 0.3)"
            :clip-path="'url(#clip' + stream.id + i + ')'"
            dy="0.3em">
        {{ adjChanges[0].v }}
        <title>{{ adjChanges[0].v }}</title>
      </text>
    </g>

    <!-- <g :fill="color">
         <text v-for="(adjChanges, i) in changePairs"
         :x="xScale(midpoint(adjChanges[0].t, adjChanges[1].t))"
         :clip-path="'url(#clip' + stream.id + i + ')'"
         text-anchor="middle"
         dy="0.3em">
         {{ adjChanges[0].v }}
         <title>{{ adjChanges[0].v }}</title>
         </text>
         </g> -->
  </g>
</template>

<script>
  import * as d3 from 'd3'

  import SvgStreamBase, { animatePath } from './SvgStreamBase'

  export default {
    name: 'lifeline-stream',
    mixins: [SvgStreamBase],
    mounted () {
      this.animate()
    },
    computed: {
      path () {
        let path = d3.path()
        path.moveTo(0, this.pathHeight)
        for (let change of this.stream.values) {
          path.lineTo(this.xScale(change.t - 0.3), this.pathHeight)
          path.lineTo(this.xScale(change.t), 0)
          path.lineTo(this.xScale(change.t + 0.3), this.pathHeight)
        }
        path.lineTo(this.width, this.pathHeight)

        return path.toString()
      },
      pathHeight () {
        return this.height / 6
      },
      changePairs () {
        let [minT, maxT] = this.xScale.domain()
        let values = [{
          t: minT,
          v: this.stream.initialValue
        }].concat(this.stream.values).concat([{
          t: maxT,
          v: this.stream.initialValue
        }])

        return d3.pairs(values)
      }
    },
    methods: {
      // midpoint (t1, t2) {
      //   let abs = Math.abs(t1 - t2) / 2
      //   return t1 < t2 ? t1 + abs : t2 + abs
      // },
      animate () {
        animatePath(this.$refs.topPath)
        animatePath(this.$refs.bottomPath)
        d3.select(this.$el)
          .selectAll('text')
          .style('opacity', 0)
          .transition()
          .duration(750)
          .style('opacity', 1)
      }
    }
  }
</script>

<style lang="scss" scoped>
  path {
    stroke-width: 2px;
    fill: transparent;
  }
  text {
    font-size: 1em;
    cursor: default;
  }
</style>
