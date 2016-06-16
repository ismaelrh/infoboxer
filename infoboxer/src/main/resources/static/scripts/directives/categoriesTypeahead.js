'use strict';

angular.module('infoboxer').
/**
 Author: Ismael Rodríguez Hernández, 2/11/15.
 Custom directive used to create typeahead with multiple datasets.
 Adapted to Infoboxer.
 */
directive('sugclases', ['$compile', '$timeout', '$translate', function ($compile, $timeout, $translate) {
    return {
        restrict: 'A',
        transclude: true,
            scope: {
            ngModel: '=',
            sugclases: '=',
            typeaheadCallback: "=",
            clase: '=',
            introducidas: '=',
        },

        link: function (scope, elem, attrs) {
            var template =
                '<div class="dropdown" ng-hide="!focused">' +
                '<ul class="dropdown-menu tt-menu" style="display:block;">' +
                '<li ng-repeat="item in filitered = (sugclases | filter:{name:ngModel} | notIntroducedCategory:introducidas |limitTo:10) track by $index"' +
                'ng-mousedown="click(item)" style="cursor:pointer" ng-class="{active:$index==active}" ng-mouseenter="mouseenter($index)">' +
                '<a class="sugerencia">{{item.name}}</a>' +
                '</li>' +
                '<li ng-if="(sugclases | filter:{name:ngModel}).length>10"><a class="mostrar-mas">' + $translate.instant('TYPE_TO_SHOW_MORE') + '</a></li>' +
                '</ul>' +
                '</div>';

            elem.bind('blur', function () {
                $timeout(function () {
                    scope.selected = true
                    scope.focused = false;
                }, 100)
            })


            elem.bind('focus', function () {
                $timeout(function () {
                    //scope.selected = false

                    scope.focused = true;

                }, 100)


            });


            elem.bind("keydown", function ($event) {
                if ($event.keyCode == 38 && scope.active > 0) { // arrow up
                    scope.active--
                    scope.$digest()
                } else if ($event.keyCode == 40 && scope.active < scope.filitered.length - 1) { // arrow down
                    scope.active++
                    scope.$digest()
                } else if ($event.keyCode == 13) { // enter
                    scope.$apply(function () {
                        scope.click(scope.filitered[scope.active])
                    })
                }
            })

            scope.click = function (item) {


                scope.selected = item
                scope.clase.name = item.name;
                scope.clase._id = item._id;
                if (scope.typeaheadCallback) {
                    scope.typeaheadCallback(item)
                }
                elem[0].blur()
            }

            scope.mouseenter = function ($index) {
                scope.active = $index
            }


            scope.quitarTildes = function (cadena) {
                cadena = cadena.toLowerCase();
                cadena = cadena.replace("á", "a");
                cadena = cadena.replace("à", "a");
                cadena = cadena.replace("é", "e");
                cadena = cadena.replace("è", "e");
                cadena = cadena.replace("í", "i");
                cadena = cadena.replace("ì", "i");
                cadena = cadena.replace("ó", "o");
                cadena = cadena.replace("ò", "o");
                cadena = cadena.replace("ú", "u");
                cadena = cadena.replace("ù", "u");
                cadena = cadena.replace("ü", "u");
                return cadena;
            };

            //Starts as unfocused
            scope.focused = false; //Starts as unfocused

            scope.$watch('ngModel', function (input) {


                if (scope.selected && scope.selected.name == input) {
                    return
                }
                else {
                    //scope.clase._id = undefined;
                }


                scope.active = 0
                scope.selected = false

                // if we have an exact match and there is only one item in the list, automatically select it
                if (input && scope.filitered && scope.filitered.length == 1 && scope.filitered[0].name.toLowerCase() == input.toLowerCase()) {
                    scope.click(scope.filitered[0])
                }
            })

            elem.after($compile(template)(scope))
        }
    }
}]);