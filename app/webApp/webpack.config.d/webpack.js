const path = require('path');

config.resolve.fallback = {
    "http": false
}
config.resolve.modules.push("kotlin");
if (config.devServer) {
    config.devServer.client = {
        overlay: false
    };
    config.devServer.hot = true;
    config.devServer.open = false;
    config.devServer.port = 3000;
    config.devServer.historyApiFallback = true;
    // Output the bundle at the path index.html expects, so webpack-dev-middleware serves the
    // processed bundle (with CSS inlined by style-loader) instead of the raw Kotlin .mjs file.
    config.output.filename = (chunkData) =>
        chunkData.chunk.name === 'main' ? 'kodex-app-webApp.mjs' : 'webApp-[name].js';
    config.devtool = 'eval-cheap-source-map';
} else {
    config.devtool = undefined;
}

// disable bundle size warning
config.performance = {
    assetFilter: function (assetFilename) {
        return !assetFilename.endsWith('.js');
    },
};
