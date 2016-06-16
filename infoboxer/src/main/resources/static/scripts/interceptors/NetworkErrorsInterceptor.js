'use strict';

//Stores useful information, like currently loaded Categories
angular.module('infoboxer')
    .factory('NetworkErrorsInterceptor', ['$q', '$rootScope', function ($q, $rootScope) {
        return {
            responseError: function (rejection) {

                //Called when the server returns a non-200 series status code.


                if (rejection.config.url.indexOf("/stats") > -1) {
                    var _message = "";
                    if (rejection.status === -1) {
                        _message = "Can't connect to Stats Server.";
                    }
                    else {
                        _message = "Problem while sending stats to Stats Server.";
                    }

                    $rootScope.$broadcast('networkWarning', {
                        code: rejection.status,
                        message: _message + " (Error code: " + rejection.status + ")",
                        type: "warning"
                    });
                    //If it comes from stats, it is not critical.
                    //Show a dialog.
                }
                else {
                    //Critical error
                    if (rejection.status === -1) {
                        _message = "Can't connect to WebService. Unreachable. Please restart Infoboxer and try later. ";
                    }
                    else {
                        _message = "Problem while retrieving data from WebService. Please restart Infoboxer and try later.";
                    }

                    $rootScope.$broadcast('networkWarning', {
                        code: rejection.status,
                        message: _message + " (Error code: " + rejection.status + ")",
                        type: "danger"
                    });
                }

                return $q.reject(rejection);
            },
        };

    }]);