import Vue from 'vue'
import Vuex from 'vuex'

import _ from 'lodash'

import Api from './api'
import Stream, { id as streamId } from './stream'
import { Types, Styles } from './constants'

Vue.use(Vuex)

function defaultSettings (stream) {
  function preselectStyle ({type, valueType}) {
    if (type === Types.SIGNAL) {
      switch (valueType) {
        case Types.BOOL:
        case Types.INT:
        case Types.FLOAT:
          return Styles.STEP
        case Types.STRING:
          return Styles.LIFELINE
      }
    } else if (type === Types.EVENTS) {
      switch (valueType) {
        case Types.BOOL:
        case Types.UNIT:
        case Types.STRING:
          return Styles.SIMPLE
        case Types.INT:
        case Types.FLOAT:
          return Styles.DISCRETE
      }
    }
  }
  return {
    style: preselectStyle(stream)
  }
}

const store = new Vuex.Store({
  state: {
    scenarioSpec: '',
    tesslaSpec: '',
    _streams: {},
    errors: [],
    pendingSimulation: null,
    clock: null,
    streamSettings: {}
  },
  getters: {
    streams (state) {
      // This getter returns the current streams as instances of the stream
      // class. It would be much nicer to store instances directly, but
      // apparently doing that is not compatible with time travel debugging.
      // (The reason for this is that it works by storing and restoring JSON
      // which is not lossless when dealing with inherited properties)
      return Object.values(state._streams).map(s => new Stream(s))
    },
    firstValue (state, getters) {
      let first = _.minBy(getters.streams.map(s => s.firstValue), 't')
      return !first ? null : first
    },
    lastValue (state, getters) {
      let last = _.maxBy(getters.streams.map(s => s.lastValue), 't')
      return !last ? null : last
    },
    currentComputedValues (state, getters) {
      return getters.streams.map(s => s.computedValueAt(state.clock))
    }
  },
  mutations: {
    updateScenarioSpec (state, scenarioSpec) {
      state.scenarioSpec = scenarioSpec
    },
    updateTesslaSpec (state, tesslaSpec) {
      state.tesslaSpec = tesslaSpec
    },
    updateStreams (state, plainStreams) {
      // Initialize streamSettings for new and delete for removed streams
      let previousStreams = Object.values(state._streams)
      let currentStreams = Object.values(plainStreams)

      let newStreams = _.differenceBy(currentStreams, previousStreams, streamId)
      let removedStreams = _.differenceBy(previousStreams, currentStreams, streamId)

      // It's important to use Vue.set and Vue.delete to allow reactivity
      // See https://vuejs.org/v2/guide/reactivity.html
      for (let stream of removedStreams) {
        Vue.delete(state.streamSettings, stream.name)
      }
      for (let stream of newStreams) {
        Vue.set(state.streamSettings, stream.name, defaultSettings(stream))
      }

      // The backend doesn't sort the streams (which is ok, because display
      // ordering is a concern of the frontend). We'll sort the streams based on
      // the order in which they are defined.
      let specsConcat = state.scenarioSpec + state.tesslaSpec
      let namePositions = {}
      for (let stream of Object.values(plainStreams)) {
        // RegExps for matching stream declaration in scenario and tessla
        // specifications.
        // TODO: This can be tricked by putting string literals in specs.
        let regexpSce = (stream.type === Types.SIGNAL ? 'Signal' : 'Events') +
            '<\\s*' + stream.valueType + '\\s*>\\s*' + stream.name
        let regexpTes = 'define\\s*' + stream.name + '\\s*\\:='

        namePositions[stream.name] = Math.max(
          specsConcat.search(regexpSce),
          specsConcat.search(regexpTes)
        )
      }
      // Keys of JavaScript objects in ES6 are ordered, so the following works.
      // http://stackoverflow.com/a/31102605/2483911
      let orderedStreams = {}
      Object.values(plainStreams).sort(
        (a, b) => namePositions[a.name] > namePositions[b.name]
      ).forEach(s => {
        orderedStreams[s.name] = s
      })

      // Finally update the streams
      state._streams = orderedStreams
    },
    updateErrors (state, errors) {
      state.errors = errors
    },
    updatePendingSimulation (state, pending) {
      state.pendingSimulation = pending
    },
    clearPendingSimulation (state) {
      state.pendingSimulation = null
    },
    updateClock (state, clock) {
      state.clock = clock
    },
    setStreamStyle (state, { stream, style }) {
      Vue.set(state.streamSettings[stream.name], 'style', style)
    }
  },
  actions: {
    async triggerRemoteSimulation ({ commit, state, getters }, n) {
      // This looks a bit unusual, because we want to be able to deal with
      // out-of-order responses. We do this by always storing the latest
      // simulation promise (which might overwrite previous values) and
      // re-checking later on if this value has changed in the meantime.
      let pending = Api.simulate(state.scenarioSpec, state.tesslaSpec)
      commit('updatePendingSimulation', pending)
      try {
        let result = await pending

        // We'll drop the result if the function was called again while waiting for the response.
        if (pending !== state.pendingSimulation) {
          return
        }

        if (result.streams) {
          commit('updateErrors', [])
          commit('updateStreams', result.streams)

          if (state.clock === null) {
            // Initialize clock with earliest value on any stream or 0 if there are no values
            let firstValue = getters.firstValue
            commit('updateClock', firstValue ? firstValue.t : 0)
          }
        } else {
          commit('updateErrors', result.errors)
        }
      } finally {
        commit('clearPendingSimulation')
      }
    }
  }
})

export default store
