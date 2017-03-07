import _ from 'lodash'

import { Types } from './constants'

export function id (stream) {
  return stream.name + stream.type + stream.valueType
}

export default class Stream {
  constructor (stream) {
    _.assign(this, stream)
  }

  get id () {
    return id(this)
  }

  get hasValues () {
    return this.values.length > 0
  }

  get isNumeric () {
    return this.valueType === Types.INT || this.valueType === Types.FLOAT
  }

  get firstValue () {
    if (this.hasValues) {
      return this.values[0]
    }
    return null
  }

  get lastValue () {
    if (this.hasValues) {
      return this.values[this.values.length - 1]
    }
    return null
  }

  get minValue () {
    return this._reduceNumericValues(_.minBy)
  }

  get maxValue () {
    return this._reduceNumericValues(_.maxBy)
  }

  _reduceNumericValues (iteratee) {
    if (this.isNumeric) {
      if (this.hasValues) {
        return iteratee(this.values, 'v').v
      } else {
        return this.initialValue
      }
    }
    // All thats left are event streams that are empty or non-numeric
    return null
  }

  /**
   * Returns the (t, value)-tuple with the largest timestamp smaller than t
   */
  lastValueBefore (t) {
    let lastBefore = null
    for (let value of this.values) {
      if (value.t >= t) {
        break
      }
      lastBefore = value
    }
    return lastBefore
  }

  /**
   * Returns the (t, value)-tuple with the smallest timestamp higher than t
   */
  firstValueAfter (t) {
    for (let value of this.values) {
      if (value.t > t) {
        return value
      }
    }
    return null
  }

  /**
   * Returns the (t, value)-tuple (if any) with timestamp t
   */
  valueAt (t) {
    for (let value of this.values) {
      if (value.t === t) {
        return value
      }
      if (value.t > t) {
        return null
      }
    }
    return null
  }

  /**
   * For signals returns the value of the signal at timestamp t derived from
   * the encountered changes.
   * For events returns the event (if any) at timestamp t or null
   */
  computedValueAt (t) {
    let valueAtT = _.get(this.valueAt(t), 'v', null)
    if (this.type === Types.SIGNAL) {
      if (valueAtT !== null) {
        return valueAtT
      } else {
        let lastBefore = _.get(this.lastValueBefore(t), 'v', null)
        return lastBefore || this.initialValue
      }
    } else {
      return valueAtT
    }
  }
}
