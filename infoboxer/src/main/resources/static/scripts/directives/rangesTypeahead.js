'use strict';

angular.module('infoboxer').
/**
 Author: Ismael Rodríguez Hernández, 2/11/15.
 Custom directive used to create typeahead with multiple datasets.
 Adapted to Infoboxer.
 */
directive('sugerencias', ['$compile', '$timeout', '$http', 'ToolsService', 'AppStateService', '$interval', 'ConfigService',
    function ($compile, $timeout, $http, ToolsService, AppStateService, $interval, ConfigService) {
        return {
            restrict: 'A', //only as an attribute
            transclude: true,
            scope: {
                ngModel: '=', //Model to change
                sugerencias: "=", //TODO: delete?
                rangesData: '=', //Information about ranges to display, and category list
                property: '=',
                value: '=',
                typeaheadCallback: "=" //What to do when something is selected
            },
            link: function (scope, elem, attrs) {

                if (AppStateService.getSimplified()) {
                    var template = '<div class="dropdown" ng-hide="!focused">' +
                        '<ul class="dropdown-menu tt-menu" style="display:block;" >' +
                        '<li ng-repeat-start="range in information" class="cabecera-sugerencia">' +
                        '<i>{{range.rangeLabel}}</i>' +
                        '</li>' +
                        '<li ng-show="range.items.length == 0 && (!range.downloading && !range.waitingType)">No instance matches the text</li>' +
                        '<li ng-show="range.downloading">Downloading...</li>' +
                        '<li ng-show="range.waitingType">Keep typing...</li>' +
                        '<li ng-repeat="item in filtered[$index] = (range.items |filter:{label:ngModel} | limitTo:5) track by $index"' +
                        ' style="cursor:pointer"  class="sugerencia" ng-class="{activa:$index == activeElement && $parent.$index == activeGroup}"' +
                        'ng-mouseenter="mouseenter($parent.$index,$index)" ng-mousedown="click(item)">' +
                        '<span>{{item.label}}</span>' +
                        '</li><div ng-repeat-end></div></ul></div>';
                }
                else {
                    var template = '<div class="dropdown" ng-hide="!focused">' +
                        '<ul class="dropdown-menu tt-menu" style="display:block;" >' +
                        '<li ng-repeat-start="range in information" class="cabecera-sugerencia">' +
                        '<i>{{range.rangeLabel}}</i>' +
                        '</li>' +
                        '<li ng-show="range.items.length == 0 && (!range.downloading && !range.waitingType)">No instance matches the text</li>' +
                        '<li ng-show="range.downloading">Downloading...</li>' +
                        '<li ng-show="range.waitingType">Keep typing...</li>' +
                        '<li ng-repeat="item in filtered[$index] = (range.items |filter:{label:ngModel} | limitTo:5) track by $index"' +
                        ' style="cursor:pointer"  class="sugerencia" ng-class="{activa:$index == activeElement && $parent.$index == activeGroup}"' +
                        'ng-mouseenter="mouseenter($parent.$index,$index)" ng-mousedown="click(item)">' +
                        '{{item.label}} - ({{item.count}} {{"TIMES" | translate}})' +
                        '</li><div ng-repeat-end></div></ul></div>';
                }


                var DOMAIN = ConfigService.endpoint.base;
                var URL = DOMAIN + ConfigService.endpoint.suggestions;


                var initialized = false;

                elem.bind('blur', function () {
                    $timeout(function () {
                        //scope.selected = true
                        scope.focused = false;
                        for(var i = 0; i < scope.information.length; i++){
                            if(scope.information[i].items.length > 0){
                                if(scope.information[i].items[0].label.toLowerCase() == scope.value.text.toLowerCase()){
                                    scope.click(scope.information[i].items[0]);
                                }
                            }
                        }

                    }, 100)
                });

                elem.bind('focus', function () {
                    $timeout(function () {
                        //scope.selected = false

                        scope.focused = true;

                    }, 100)

                    scope.updateData();
                    initialized = true;

                    //First time focus is done, data is downloaded.
                });

                //Key control
                elem.bind("keydown", function ($event) {
                    //Arrow up, only if not first element
                    if ($event.keyCode == 38 && !(scope.activeGroup == 0 && scope.activeElement == 0)) { // arrow up


                        if (scope.activeElement == 0) {
                            //Change group
                            scope.activeElement = scope.filtered[scope.activeGroup - 1].length - 1;
                            scope.activeGroup--;
                        }
                        else {
                            scope.activeElement--;
                        }
                        scope.$digest()
                        //Arrow down, only if not last element
                    } else if ($event.keyCode == 40 && !(scope.activeElement == scope.filtered[scope.filtered.length - 1].length - 1
                        && scope.activeGroup == scope.filtered.length - 1)) {

                        //Change group
                        if (scope.activeElement == scope.filtered[scope.activeGroup].length - 1) {
                            scope.activeElement = 0;
                            scope.activeGroup++;
                        }
                        else {
                            scope.activeElement++;
                        }
                        scope.$digest()

                        //Enter key, simulate click.
                    } else if ($event.keyCode == 13) {
                        scope.$apply(function () {
                            scope.click(scope.filtered[scope.activeGroup][scope.activeElement])
                        })
                    }

                })

                /**
                 Click action, updates ng-model with the clicked item
                 */
                scope.click = function (item) {

                    //Set value, type and uri
                    scope.value.text = item.label;
                    scope.value.type = item.type;
                    scope.value.uri = item._id;

                    //Update lastType
                    scope.lastType = scope.value.type;

                    scope.selected = item;
                    if (scope.typeaheadCallback) {
                        scope.typeaheadCallback(item)
                    }
                    elem[0].blur()
                }

                /**
                 On mouse enter, element is active.
                 */
                scope.mouseenter = function ($parentIndex, $childIndex) {
                    scope.activeGroup = $parentIndex;
                    scope.activeElement = $childIndex;
                }


                /*
                 Update items on group number "index", and sets dowloading to false.
                 */
                scope.updateList = function (index, items) {

                    for (var i = 0; i < items.length; i++) {
                        var item = items[i];
                        item.type = scope.information[index].rangeURI;
                        scope.information[index].items.push(item);
                    }

                    scope.information[index].downloading = false;

                }


                /**
                 Queries the remote endpoint and updates data.
                 Uses the ng-model text as a label.
                 */
                scope.updateData = function () {

                    for (var k = 0; k < scope.information.length; k++) {


                        var categoryListComma = ToolsService.arrayToCommaSeparated(scope.information[k].classList);

                        var config = undefined;
                        if (!scope.rangesData.semantic) {
                            config = {
                                cache: true, //Caching activated
                                params: {
                                    classList: categoryListComma,
                                    property: scope.property,
                                    rangeType: scope.information[k].rangeURI,
                                    label: scope.ngModel
                                }
                            };
                        }
                        else {
                            config = {
                                cache: true, //Caching activated
                                params: {
                                    rangeType: scope.information[k].rangeURI,
                                    label: scope.ngModel
                                }
                            };
                        }


                        console.dir(config);

                        //Set downloading to "true"
                        scope.information[k].waitingType = false;
                        scope.information[k].downloading = true;
                        scope.information[k].items = [];


                        //HTTP calls. Closure to avoid "i" being updated.
                        (function (i) {
                            $http.get(URL, config)
                                .then(function (data) {
                                    scope.updateList(i, data.data);
                                })
                                .catch(function (data) {
                                    scope.information[i].downloading = false;

                                });
                        })(k);
                    }

                };


                //Starts as unfocused
                scope.focused = false; //Starts as unfocused
                scope.information = []; //Information = empty

                //Last-type stores the last known type. Used for comparing it to scope.value.type
                //When text changes so we can determinate if change has been produced by typing (so we have to change type)
                //or Wikipedia load (Do not change type).
                scope.lastType = scope.value.type;

                //Fill "information" table with the received rangesData
                //UnknownRDFType goes the last
                var unknownRDFTypeObject = undefined;
                for (var j = 0; j < scope.rangesData.ranges.length; j++) {
                    var currentRange = scope.rangesData.ranges[j];
                    var currentLabel = scope.rangesData.labels[j];
                    if (currentRange.indexOf("unknownRDFType") > -1) {
                        unknownRDFTypeObject =
                        {
                            classList: scope.rangesData.classList,
                            rangeURI: currentRange,
                            rangeLabel: currentLabel,
                            downloading: false,
                            items: [] //Starts as empty

                        };
                    }
                    else {
                        scope.information.push(
                            {
                                classList: scope.rangesData.classList,
                                rangeURI: currentRange,
                                rangeLabel: currentLabel,
                                downloading: false,
                                items: [] //Starts as empty

                            }
                        );
                    }

                }
                ;

                //Push the last unknownRDFType
                if (unknownRDFTypeObject != undefined) {
                    scope.information.push(unknownRDFTypeObject);
                }

                //Loading interval, for not making one request on every type
                var timerInterval = 150; //100ms
                var currentTime = 300; //Decreasing counter, how many ms until charge
                var active = true;

                var loadingInterval = undefined;


                /** Watch changes on ngModel */
                scope.$watch('ngModel', function (input) {

                    if (scope.selected && scope.selected.label == input) {
                        return
                    }

                    /* Restart active group, element and selected on every
                     model change */
                    scope.activeGroup = 0
                    scope.activeElement = 0
                    scope.filtered = [];
                    scope.selected = false;

                    //When changing, by default is String
                    if (scope.value.changedByWiki) {
                        scope.value.changedByWiki = false;
                        //We preserve its type
                    }
                    else {
                        //Changed by hand -> String
                        scope.value.type = "<http://www.w3.org/2001/XMLSchema#String>";
                    }


                    /* Query data only if initialized (if it has been focused at least one time).
                     Used to avoid querying if it has not been used */
                    if (initialized) {

                        active = true;
                        currentTime = 300; //Reset time
                        for (var k = 0; k < scope.information.length; k++) {
                            scope.information[k].waitingType = true;
                            scope.information[k].items = [];

                        }

                        if (loadingInterval == undefined) { //If timer is destroyed, create another one
                            loadingInterval = $interval(function () {


                                if (currentTime <= 0 && active) {

                                    //When timer = 0, load data and destroy timer.
                                    active = false;
                                    scope.updateData();
                                    $interval.cancel(loadingInterval);
                                    loadingInterval = undefined;

                                }
                                else if (currentTime > 0 && active) {
                                    currentTime -= timerInterval;
                                }

                            }, timerInterval);
                        }

                        //scope.updateData();
                    }

                    // if we have an exact match and there is only one item in the list, automatically select it
                    /*if(input && scope.filtered.length == 1 && scope.filtered[0].name.toLowerCase() == input.toLowerCase()) {
                     scope.click(scope.filtered[0])
                     }*/
                });

                elem.after($compile(template)(scope))
            }
        }
    }]);
