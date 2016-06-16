'use strict';

angular.module('infoboxer')


    .filter('typeToCamelCase', ['ToolsService', function (ToolsService) {
        return function (text) {
            return ToolsService.typeToCamelCase(text);
        };
    }])


    //  URItoCamelCase Filter
    .filter('URIcamelCase', ['ToolsService', function (ToolsService) {
        return function (text) {

            return ToolsService.URItoCamelCase(text);

        };

    }])

    .filter('URItoLowerHyphen', ['ToolsService', function (ToolsService) {

        return function (text) {
            return ToolsService.URItoLowerHyphen(text);
        }
    }])


    .filter('lengthCutter', [function () {
        var limit = 24;
        return function (texto) {
            if (texto.length > limit) {
                return texto.substr(0, limit - 3) + "...";
            }
            return texto;
        };
    }])

    .filter('twoDecimals', [function () {
        return function (number) {
            return (number * 100).toFixed(2);
        };
    }
    ])

    .filter('fourDecimals', [function () {
        return function (number) {
            return (number * 100).toFixed(4);
        };
    }
    ])

    .filter('fiveDecimals', [function () {
        return function (number) {
            return (number * 100).toFixed(10);
        };
    }
    ])




    .filter('URItoURL', [function () {
        return function (URI) {

            return URI.replace("<", "").replace(">", "");
        };
    }
    ])

    .filter('removeSpaces', [function () {
        return function (string) {

            return string.replace(new RegExp(" ", 'g'), "");
        };
    }
    ])

    .filter('textToResourceURI', ['$translate','ConfigService', function ($translate,ConfigService) {
        return function (text) {

            var currentLanguage = $translate.use();
            var prefix = ConfigService.resourcePrefix[currentLanguage];
            if(!prefix){
                prefix = ConfigService.resourcePrefix["default"];
            }
            return "<" + prefix + text.replace(/ /g,'_') + ">";


        };
    }
    ])


    .filter('textToResource', ['$translate','ConfigService', function ($translate,ConfigService) {
        return function (text) {

            var currentLanguage = $translate.use();
            var prefix = ConfigService.resourcePrefix[currentLanguage];
            if(!prefix){
                prefix = ConfigService.resourcePrefix["default"];
            }
            return prefix + text.replace(/ /g,'_');


        };
    }
    ])

    .filter('secondsToDateTime', [function () {
        return function (seconds) {
            return new Date(1970, 0, 1).setSeconds(seconds);
        };
    }]);

