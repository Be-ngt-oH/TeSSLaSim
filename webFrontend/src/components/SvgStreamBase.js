import * as d3 from 'd3'

import { Types } from '../constants'

export const PADDING = 15

export function animatePath (path) {
  let totalLength = path.getTotalLength()
  d3.select(path)
    .attr('stroke-dasharray', totalLength)
    .attr('stroke-dashoffset', totalLength)
    .transition().duration(1000).ease(d3.easeLinear)
    .attr('stroke-dashoffset', 0)
    .on('end', function () {
      d3.select(this)
        .attr('stroke-dasharray', null)
        .attr('stroke-dashoffset', null)
    })
}

export const YScale = {
  computed: {
    yScale: function () {
      let scale = d3.scaleLinear().range([this.height - PADDING, 0 + PADDING])
      if (this.stream.valueType === Types.BOOL) {
        return scale.domain([-1, 2])
      } else {
        return scale.domain(
          [Math.min(0, this.stream.minValue - 1), this.stream.maxValue + 1]
        )
      }
    },
    baseline: function () {
      return 'M0 ' + this.yScale(0) + ' H' + this.width
    }
  }
}

export default {
  props: [
    'stream',
    'height',
    'width',
    'xScale',
    'color'
  ]
}
