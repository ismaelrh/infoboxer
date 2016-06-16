'use strict';

angular.module('infoboxer')
    /* Service to show a "Information" modal */
    .service('UserFinishModalService', ['$modal', 'StatsService', '$location', '$translate', '$route', 'ConfigService',
        function ($modal, StatsService, $location, $translate, $route, ConfigService) {


            var self = this;
            var modalDefaults = {
                backdrop: true,
                keyboard: false,
                modalFade: true,
                templateUrl: 'views/partials/finishModal.html'
            };

            var modalOptions = {
                title: "¡Enhorabuena, has acabado!",
                questions: [
                    {"title": "Facilidad de uso", "value": 1},
                    {"title": "Utilidad", "value": 2},
                    {"title": "Otra pregunta", "value": 0}
                ]
            };


            this.showModal = function (customModalDefaults, customModalOptions) {
                if (!customModalDefaults) {
                    customModalDefaults = {};
                }
                customModalDefaults.backdrop = 'static';
                return this.show(customModalDefaults, customModalOptions);
            };

            this.show = function (customModalDefaults, customModalOptions) {
                //Create temp objects to work with since we're in a singleton service
                var tempModalDefaults = {};
                var tempModalOptions = {};


                //Map angular-ui modal custom defaults to modal defaults defined in service
                angular.extend(tempModalDefaults, modalDefaults, customModalDefaults);

                //Map modal.html $scope custom properties to defaults defined in service
                angular.extend(tempModalOptions, modalOptions, customModalOptions);


                tempModalOptions.infobox_export_screen.resultInfoboxList = [];
                //Generate infoboxes from server

                tempModalOptions.serverside = false;
                if (ConfigService.infoboxCode.serverside == true) {
                    tempModalOptions.serverside = true;
                }


                //If serverside = false (or default), infobox code is showed once.
                if (ConfigService.infoboxCode.serverside == false) {

                    for (var i = 0; i < tempModalOptions.infobox_export_screen.categoryList.length; i++) {

                        tempModalOptions.infobox_export_screen.resultInfoboxList[i] = tempModalOptions.infobox_export_screen.givenInfobox;

                    }

                    var currentCategory = tempModalOptions.infobox_export_screen.categoryList[0];

                    //Save infobox on server
                    if(StatsService.stats_activated){
                        StatsService.saveInfobox(tempModalOptions.infobox_export_screen.pageName,currentCategory._id,tempModalOptions.infobox_export_screen.givenInfobox);

                    }

                }
                //If serverside = true, infobox is sent to server for each category and shown once per category.
                else {
                    for (var i = 0; i < tempModalOptions.infobox_export_screen.categoryList.length; i++) {
                        var currentCategory = tempModalOptions.infobox_export_screen.categoryList[i];

                        tempModalOptions.infobox_export_screen.resultInfoboxList[i] = $translate.instant('LOADING') + "...";


                        var helper = function (e) {
                            StatsService.generateInfobox(tempModalOptions.infobox_export_screen.pageName,
                                currentCategory._id, tempModalOptions.infobox_export_screen.givenInfobox)
                                .then(function (result) {
                                    if (result == "ERROR") {
                                        tempModalOptions.infobox_export_screen.resultInfoboxList[e] = $translate.instant('ERROR_OCCURRED')
                                    }
                                    else {
                                        tempModalOptions.infobox_export_screen.resultInfoboxList[e] = result;
                                    }
                                });
                        };


                        helper(i);

                    }
                }
                ;


                if (!tempModalDefaults.controller) {
                    tempModalDefaults.controller = function ($scope, $modalInstance) {

                        //Used to save the survey to the server
                        var saveSurvey = function () {
                            var response1 = $scope.modalOptions.survey_points_screen.questions[0].value;
                            var response2 = $scope.modalOptions.survey_points_screen.questions[1].value;
                            var response3 = $scope.modalOptions.survey_points_screen.questions[2].value;

                            var freeText = $scope.modalOptions.survey_free_screen.text;
                            StatsService.saveSurvey(response1, response2, response3, freeText);


                        }


                        $scope.modalOptions = tempModalOptions;

                        $scope.modalOptions.mode = 0;
                        $scope.modalOptions.nextText = [$translate.instant('NEXT'), $translate.instant('NEXT'), $translate.instant('FINISH'), $translate.instant('FINISH')];
                        $scope.modalOptions.ok = function (result) {
                            $modalInstance.close(result);
                        };
                        $scope.modalOptions.close = function () {
                            $modalInstance.dismiss('cancel');
                        };
                        $scope.modalOptions.next = function () {
                            if ($scope.modalOptions.mode < 2) {
                                //Never
                                $scope.modalOptions.mode++;
                            }
                            else {


                                //Save survey on last step
                                saveSurvey();


                                //Close session, dismiss and goes to login

                                $modalInstance.dismiss('cancel');
                                if (StatsService.stats_activated) {
                                    StatsService.closeSession()
                                        .then(function () {
                                            return StatsService.newSession(StatsService.username)
                                                .then(function () {
                                                    StatsService.addAction("EDITION INFORMATION", "EDITION START");
                                                });
                                        })

                                }

                                $route.reload();


                            }

                        }
                    };
                }

                return $modal.open(tempModalDefaults).result;
            };

        }]);
