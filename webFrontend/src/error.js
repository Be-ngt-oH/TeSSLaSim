export const Errors = {
  NETWORK_ERROR: {
    title: 'Network Error',
    message: 'There were difficulties reaching the server. ' +
      'Unfortunately this tool cannot work without a working connection.'
  },
  ARBITRARY_ERROR: {
    title: 'Catch me if you can',
    message: 'This should not have happened, apologies!'
  }
}

export const ErrorHandler = {
  logError (e) {
    console.error(e)
  },
  networkError () {
    this.displayError(
      Errors.NETWORK_ERROR
    )
  },
  arbitraryError () {
    this.displayError(
      Errors.ARBITRARY_ERROR
    )
  },
  /**
   * Function used to display an human-readable error message to the user.
   * It's meant to be overriden somewhere at the application's root.
   */
  displayError: (error) => console.log(error.title + ' - ' + error.message)
}
