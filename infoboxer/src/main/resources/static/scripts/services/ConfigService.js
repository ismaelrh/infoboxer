'use strict';

//Stores frontend config loaded from data/config.json
angular.module('infoboxer')

    .provider('ConfigService', function () {
        var config = {};
        this.config = function (opt) {
            angular.extend(config, opt);
        };
        this.$get = [function () {
            if (!config) {
                throw new Error('Config options must be configured');
            }
            return config;
        }];
    });
