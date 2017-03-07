# TeSSLaSimulator Developer Guide

The project is divided into four sub projects:

  * *scenarioParser* which contains code related to translation of scenario
    specifications to maps of streams

  * *simulatorCore* which deals with evaluation of tessla specifications on a
    map of input streams

  * *webService* a simple server implementation that provides a web interface

  * *webFrontend* a front-end for entering and visualizations of scenarios and
    TeSSLa specifications


Furthermore the directory *shared* contains some functions that are shared
between the back-end parts.

The build tool for the Scala sources is [SBT](http://www.scala-sbt.org/).
The frontend is built via [Webpack](https://webpack.github.io/), but
[npm](https://www.npmjs.com/) scripts for triggering the build process are
provided.

## Back-end

The back-end is completely written in Scala and organized as a
[multi-project build](http://www.scala-sbt.org/1.0/docs/Multi-Project.html).
When working in the SBT console you can invoke `projects` to see a list of the
registered project and `project {{projectName}}` to change the current project.
Outside of SBT (i.e. your shell) you can invoke project specific commands by
prefixing the command with the project name: `{{projectName}}/{{command}}`.

For example starting the web service can be done via following command:

    sbt 'webService/run'
    
The web service will be served at `localhost:8080`.

Note: If you are running that command for the first time it might take a while
since SBT has to download all dependencies first.
    
## Front-end

You can find its own README in the folder `/webFrontend`. But here are the most
important commands anyway.
Note: All these commands have to be run in `/webFrontend`

To download dependencies (only needed once):

    npm install

To run a dev server:

    npm run dev
    
The front end will be served at `localhost:9000`.

To build a bundle for deployment:

    npm run build
    
The minimized and optimized bundle can be found in `/dist`.
