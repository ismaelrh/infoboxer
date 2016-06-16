'use strict';

angular.module('infoboxer')
    .service('DataObtainingService', ['$http', 'ToolsService', '$translate', 'ConfigService',
        function ($http, ToolsService, $translate, ConfigService) {

            /**
             * PROVIDES BASIC BACKEND COMUNICATION.
             * This service provides functions for doing requests to the back-end.
             * Every function returns a promise that passes the result as a parameter
             * to the callback function.
             */


            var URL = ConfigService.endpoint.base;
            var URLbase = URL + ConfigService.endpoint.data;
            var URLpopular = URL + ConfigService.endpoint.suggestions;
            var URLFromWikipedia = URL + ConfigService.endpoint.fromWikipedia;


            var inverseCompareRanges = function (a, b) {
                if (a.count < b.count) {
                    return 1;
                }
                if (a.count > b.count) {
                    return -1;
                }

                return 0;
            };

            /**
             * Number of instances of one or more classes.
             * Returns a promise that passes an integer to the callback function.
             */
            this.getInstanceCount = function (classURIArray) {

                var classList = ToolsService.arrayToCommaSeparated(classURIArray);

                //GET params and Cache
                var config =
                {
                    cache: true,
                    params: {classList: classList}
                };
                //Returns "count" value -> an integer.
                return $http.get(URLbase + '/instanceCount', config).then(
                    function (response) {


                        return response.data.count;
                    }
                );
            };

            /**
             Number of instances of one or more classes that manifest a property.
             Returns a promise that passes an integer to the callback function.
             */
            this.getNumberInstancesOfProperty = function (classURIArray, propertyURI) {

                var classList = ToolsService.arrayToCommaSeparated(classURIArray);

                //GET params and Cache
                var config =
                {
                    cache: true,
                    params: {
                        classList: classList,
                        property: propertyURI
                    }
                };
                //Returns "count" value -> an integer.
                return $http.get(URLbase + '/instancesWithPropertyCount', config).then(
                    function (response) {
                        return response.data.count;
                    }
                );

            };
            /*
             * list of properties and counts for a class
             */
            this.getPropertiesList = function (classList, semantic) {

                var classListCommaSeparated = ToolsService.arrayToCommaSeparated(classList);


                //GET params and Cache
                if (semantic) {
                    var config =
                    {
                        cache: true,
                        params: {
                            classList: classListCommaSeparated,
                            language: $translate.use(),
                            semantic: true
                        }
                    };
                }


                else {
                    var config =
                    {
                        cache: true,
                        params: {
                            classList: classListCommaSeparated,
                            language: $translate.use()
                        }
                    };

                }

                return $http.get(URLbase + "/propertyList", config).then(
                    function (response) {

                        var result = response.data;
                        var hasThumbnail = false;
                        for (var j = 0; j < result.length; j++) {
                            if (result[j]._id.indexOf(ConfigService.thumbnail.property) > -1) {
                                hasThumbnail = true;
                            }
                        }


                        if (!hasThumbnail && ConfigService.thumbnail.activated) {
                            result.push({
                                _id: ConfigService.thumbnail.property,
                                count: 0,
                                label: "Thumbnail",
                                semantic: true
                            });
                        }

                        return response.data;
                    }
                );
            };


            /**
             Number of uses of a property in a certain set of classes.
             Returns a promise that passes an integer to the callback function.
             */
            this.getNumberUsesOfProperty = function (classURIArray, propertyURI) {
                var classList = ToolsService.arrayToCommaSeparated(classURIArray);

                //GET params and Cache
                var config =
                {
                    cache: true,
                    params: {
                        classList: classList,
                        property: propertyURI
                    }
                };

                return $http.get(URLbase + '/getPropUses', config).then(
                    function (response) {
                        return response.data.count;
                    }
                );
            };






            /**
             Returns a JSON List with the ranges and its uses on the indicated  object that
             represent the number of uses of the range "range" on values
             */
            this.getRangesAndUses = function (classURIArray) {
                var classList = ToolsService.arrayToCommaSeparated(classURIArray);

                //GET params and Cache
                var config =
                {
                    cache: true,
                    params: {
                        classList: classList,
                        language: $translate.use()
                    }
                };

                return $http.get(URLbase + '/rangesAndUses', config)
                    .then(
                        function (response) {

                            var result = response.data;
                            //We iterate over the properties and sort their ranges
                            for (var i = 0; i < result.length; i++) {
                                result[i].value.sort(inverseCompareRanges);

                            }
                            return result;
                        }
                    );
            };

            this.getDataForInstance = function (wikipediaInstance) {

                var config =
                {
                    cache: true,
                    params: {
                        wikipediaUrl: wikipediaInstance
                    }

                };

                return $http.get(URLFromWikipedia, config)
                    .then(
                        function (response) {
                            return response.data;
                        }
                    );
            }


        }]);
