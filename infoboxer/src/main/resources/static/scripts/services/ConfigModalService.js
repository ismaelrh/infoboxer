'use strict';

angular.module('infoboxer')
    /* Service to show a "Config" modal */
    .service('ConfigModalService', ['$modal','$translate','AppStateService','$rootScope','localStorageService',
        function ($modal,$translate,AppStateService,$rootScope,localStorageService) {

            var modalDefaults = {
                backdrop: true,
                keyboard: true,
                modalFade: true,
                templateUrl: 'views/partials/configModal.html'
            };

            var modalOptions = {
                closeButtonText: 'Close',
                actionButtonText: 'OK',
                headerText: 'Proceed?',
                bodyText: 'Perform this action?'
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

                if (!tempModalDefaults.controller) {
                    tempModalDefaults.controller = function ($scope, $modalInstance) {
                        $scope.modalOptions = tempModalOptions;
                        $scope.modalOptions.ok = function (result) {
                            $modalInstance.close(result);
                        };
                        $scope.modalOptions.close = function () {
                            $modalInstance.dismiss('cancel');
                        };

                        //Get language from translate service
                        $scope.language = $translate.use();

                        //Changes language
                        $scope.saveLanguage = function () {
                            $translate.use($scope.language);
                            localStorageService.set("infoboxerLanguage", $scope.language);
                        };


                        $scope.complex = !AppStateService.getSimplified();

                        //Change mode
                        $scope.saveComplex = function () {


                            AppStateService.setSimplified(!$scope.complex);

                            localStorageService.set("infoboxerComplex", $scope.complex);
                            $rootScope.$broadcast('complex-changed');
                        }

                        $scope.semantic = AppStateService.getSemantic();

                        $scope.saveSemantic = function(){
                            AppStateService.setSemantic($scope.semantic);
                            localStorageService.set("infoboxerSemantic", $scope.semantic);
                            $rootScope.$broadcast('semantic-changed');
                        };

                        $scope.saveChanges = function(){
                            $scope.saveLanguage();
                            $scope.saveComplex();
                            $scope.saveSemantic();
                            $modalInstance.dismiss('cancel');
                        }

                    };
                }

                return $modal.open(tempModalDefaults).result;
            };

        }]);
