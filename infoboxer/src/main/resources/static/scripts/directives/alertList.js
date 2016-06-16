'use strict';

angular.module('infoboxer')

    .directive('alertList', [function () {

        return {
            restrict: 'EA', //As Element or Attribute
            transclude: false,
            templateUrl: 'views/partials/alertList.html',
            scope: {
                list: "=" //list of alerts: {type,title,message},
                //where type = ["success","info","warning","danger"]
            },

            link: function (scope, element, attrs) {
                //It removes an alert from the alert list
                scope.remove = function (index) {
                    if (index > -1) {
                        scope.list.splice(index, 1);
                    }
                };

            }
        }
    }]);