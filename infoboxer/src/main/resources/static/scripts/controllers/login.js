'use strict';

/**
 * @ngdoc function
 * @name infoboxer.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the infoboxer
 */
angular.module('infoboxer')
    .controller('LoginCtrl', ['$scope', '$location', '$translate', 'StatsService', 'AppStateService', '$window', '$routeParams', 'ConfigService',
        function ($scope, $location, $translate, StatsService, AppStateService, $window, $routeParams, ConfigService) {

            var self = this;
            this.virginUsername = true;

            //Load config from file
            //ConfigService.loadConfig();
            var userAgent = $window.navigator.userAgent;
            if(userAgent.indexOf("MSIE") > -1){
                self.ie = true;
            }



            this.username = "";

            self.cat1 = $routeParams.cat1;
            self.cat2 = $routeParams.cat2;
            self.pageName = $routeParams.pageName;


            AppStateService.loadFromLocalStorage();
            this.language = $translate.use();
            this.semantic = AppStateService.getSemantic();



            this.go = function (path) {
                $location.path(path);
            };

            this.goApp = function () {

                self.param1 = "";
                self.param2 = "";
                self.param3 = "";
                if (self.cat1 && self.cat1.length > 0) {
                    self.param1 = "/" + self.cat1;
                }
                if (self.cat2 && self.cat2.length > 0) {
                    self.param2 = "/" + self.cat2;
                }
                if(self.pageName && self.pageName.length > 0){
                    self.param3 = "/" +  self.pageName;
                }

                //Pass parameters of categories
                $location.path("/app" + self.param1 + self.param2 + self.param3);

            };

            this.startSession = function () {


                //Check if a username has been introduced
                if (this.username.length <= 0) {
                    this.virginUsername = false;
                    alert($translate.instant('PLEASE_ENTER_USERNAME'));
                }
                else {


                    if (StatsService.stats_activated) {
                        StatsService.newSession(self.username)


                            .then(function (sessionId) {
                                //Start editing
                                StatsService.addAction("EDITION INFORMATION", "EDITION START");
                                self.goApp();
                            })
                            .catch(function (data) {
                                self.goApp();
                                console.log("[STATS] Error contacting stats server.");
                            });
                    }
                    else {
                        //Stats not activated
                        console.log("[STATS] Stats not activated.");
                        self.goApp();
                    }
                }

            }

            this.getSimplified = function () {
                return AppStateService.getSimplified();
            }

            this.changeLanguage = function (key) {
                this.language = key;
                $translate.use(key);
            };

            this.setSimplified = function (value) {

                AppStateService.setSimplified(value);


            };


            this.getSemantic = function () {
                return AppStateService.getSemantic();
            }
            this.setSemantic = function (value) {
                AppStateService.setSemantic(value);
            }


        }]);