import { ErrorHandler } from './error'

const baseUrl = '//' + window.location.hostname + ':8080/'

async function post (endpoint, body) {
  try {
    var response = await fetch(baseUrl + endpoint, {
      method: 'POST',
      headers: new Headers({
        'Content-Type': 'application/json'
      }),
      body: JSON.stringify(body)
    })
  } catch (e) {
    // These are only network errors (e.g. timeout).
    // Semantic errors like 400 Bad Request are not caught here!
    // https://developer.mozilla.org/en-US/docs/Web/API/GlobalFetch/fetch
    ErrorHandler.networkError()
    // We re-throw the error and let it bubble to the top so the execution is stopped
    throw e
  }

  if (!response.ok) {
    ErrorHandler.arbitraryError()
    throw new Error(await response.text())
  }

  return response.json()
}

const Api = {
  /**
    * Returns a promise that resolves to true if the web service is reachable and false otherwise
    */
  async ping () {
    try {
      return (await fetch(baseUrl)).ok
    } catch (e) {
      return false
    }
  },

  /**
   * Sends the passed simulation data to the web service and returns a promise resolving to the simulation result.
   * The result is either a list of errors that were present in the simulation data or a map of stream objects.
   */
  simulate: (scenarioSpec, tesslaSpec, n) => post('simulate', { scenarioSpec, tesslaSpec })
}

export default Api
