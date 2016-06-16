'use strict';

//Stores useful information, like currently loaded Categories
angular.module('infoboxer')
    .service('AppStateService', ['localStorageService','$translate', function (localStorageService,$translate) {

        var self = this;

        var categoryList = []; //List of currently loaded categories
        var complexCategoryList = [];
        var savedValues = {};
        var simplified = true;
        var semantic = true;


        self.loadFromLocalStorage = function(){
            var language =  localStorageService.get("infoboxerLanguage");
            if(language){
                $translate.use(language);
            }
            else{
                $translate.use();
            }
            var sem =  localStorageService.get("infoboxerSemantic");
            if(sem !== null){

                semantic = sem;
            }
            var comp =  localStorageService.get("infoboxerComplex");
            if(comp !== null){
                simplified = !comp;
            }
            else{
                simplified = false;
            }

        };


        self.setSimplified = function (value) {
            simplified = value;
        };

        self.getSimplified = function () {
            return simplified;
        };

        self.getSemantic = function () {
            return semantic;
        }

        self.setSemantic = function (value) {
            semantic = value;
        }

        self.getCategoryList = function () {
            return categoryList;
        };

        self.getComplexCategoryList = function () {
            return complexCategoryList;
        };

        self.isCategoryContained = function (category) {
            var contains = false;
            var i = 0;
            while (!contains && i < categoryList.length) {
                contains = (categoryList[i] === category);
                i++;
            }
            return contains;
        };

        self.isComplexCategoryContained = function (name) {
            var contains = false;
            var i = 0;
            while (!contains && i < complexCategoryList.length) {
                contains = (complexCategoryList[i].name === name);
                i++;
            }
            return contains;
        };


        self.addToCategoryList = function (category) {
            if (!self.isCategoryContained(category)) {
                categoryList.push(category);
            }
        }

        self.addToComplexCategoryList = function (name, _id) {
            if (!self.isComplexCategoryContained(name)) {
                complexCategoryList.push({"name": name, "_id": _id});
            }

        };

        self.removeFromCategoryList = function (category) {
            if (self.isCategoryContained(category)) {

            }
        }

        self.clearCategoryList = function () {
            categoryList = [];
        };


        self.clearComplexCategoryList = function () {
            complexCategoryList = [];
        };

        self.clearValues = function () {
            savedValues = [];
        };

        //Saves values in order to display it in the new populated data
        self.saveValues = function (propertyHandlers) {
            //Save values
            for (var i = 0; i < propertyHandlers.length; i++) {
                var currentHandler = propertyHandlers[i];
                if (currentHandler.values.length > 1 || (currentHandler.values.length === 1 && currentHandler.values[0].text.length > 0)) {
                    if (savedValues[currentHandler.propertyURI] !== undefined) {
                        //Ya habia valores
                        for (var j = 0; j < currentHandler.values.length; j++) {
                            savedValues[currentHandler.propertyURI].push(currentHandler.values[j]);
                        }
                    }
                    else {
                        //Es nuevo
                        savedValues[currentHandler.propertyURI] = currentHandler.values;
                    }
                }
            }
            //console.dir(savedValues);
        };

        //Restores values
        self.restoreValues = function (propertyHandlers) {
            //For each stored property
            for (var propertyURI in savedValues) {
                if (savedValues.hasOwnProperty(propertyURI)) {
                    //We search the new propertyHandler
                    var found = false;
                    var j = 0;
                    while (!found && j < propertyHandlers.length) {

                        if (propertyHandlers[j].propertyURI === propertyURI) {
                            propertyHandlers[j].values = savedValues[propertyURI];
                            found = true;
                        }
                        j++;
                    }
                }
            }

        };


    }]);
