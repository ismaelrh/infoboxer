'use strict';

angular.module('infoboxer')
    //This service contains common tools
    .service('ToolsService', [function () {



        /**
         Transforms an array of strings to a unique string, that contains
         the strings from the array separated by commas
         */
        this.arrayToCommaSeparated = function (array) {
            var classList = ""; //String with class list separated by comma

            var classNumber = array.length;
            if (classNumber > 0) {

                classList += array[0];

                for (var i = 1; i < classNumber; i++) {
                    classList += "," + array[i];
                }
            }
            return classList;
        };




        /**
         * Transforms a URI to another prefix.
         */
        this.transformUriToPrefix = function (oldUri, newPrefix) {

            var lastIndex = oldUri.lastIndexOf("/");
            var sub = oldUri.substring(lastIndex + 1, oldUri.length);
            if (sub.charAt(sub.length - 1) == ">") {
                sub = sub.substring(0, sub.length - 1);
            }

            return newPrefix + sub;

        };


        //Transforms URI to Lowercase separated by Hyphens
        this.URItoLowerHyphen = function (s) {

            return this.URItoCamelCase(s).toLowerCase().replace(/ /g, '_');
        };





    }]);
