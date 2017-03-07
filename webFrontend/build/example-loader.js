module.exports = function(source) {
  this.cacheable();

  var regexp = /^([\s\S]*?)\n*---\n*([\s\S]*?)\n*$/,
      match = regexp.exec(source)

  return "module.exports = " + JSON.stringify({
    scenario: match[1],
    tessla: match[2]
  })
}
