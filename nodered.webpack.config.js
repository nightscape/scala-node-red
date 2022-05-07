var webpack = require('webpack');

module.exports = require('./scalajs.webpack.config');

Object.assign(module.exports,
    {
        resolve: {
            modules: [
                'node_modules',
                'src'
            ],
            fallback: {
                "assert": false,
                "constants": false,
                "fs": false,
                "tls": false,
                "net": false,
                "os": false,
                "path": false,
                "zlib": false,
                "http": false,
                "https": false,
                "stream": false,
                "crypto": false,
                "util": false,
            }
        },
    }
)
