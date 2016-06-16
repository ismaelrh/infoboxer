'use strict';

angular.module('infoboxer')

    /* Contains functions to transform a set of PropertyHandler objects to
     an output format, like RDF or HTML */
    .service('OutputDataService', ['$modal', 'URItoURLFilter', 'URIcamelCaseFilter', 'textToResourceURIFilter', 'AppStateService', 'URItoLowerHyphenFilter', 'ToolsService', 'ConfigService',
        function ($modal, URItoURLFilter, URIcamelCaseFilter,  textToResourceURIFilter, AppStateService, URItoLowerHyphenFilter, ToolsService, ConfigService) {


            var self = this;

            /**
             * Calls the proper "generateInfobox" function based on infoboxCode.format config ("mediawiki" or "wikipedia")
             * Default is mediawiki format.
             */
            self.generateInfobox = function (pageName, propertyHandlers) {


                if (ConfigService.infoboxCode.format === "mediawiki") {
                    return self.generateWikimediaInfobox(pageName, propertyHandlers);
                }
                else {
                    return self.generateWikipediaInfobox(pageName, propertyHandlers);
                }

            };


            /* Returns a string with the infobox code for Wikipedia */
            self.generateWikipediaInfobox = function (pageName, propertyHandlers) {

                var currentProperty; //To check more than one value for one property
                var result = "";

                var cats = AppStateService.getCategoryList();

                result += "{{Infobox ";//Infobox template, currently first category
                result += "\n| title = " + pageName; //Infobox title = page name

                //For each property
                for (var i = 0; i < propertyHandlers.length; i++) {
                    var currentHandler = propertyHandlers[i];

                    //For each value
                    for (var j = 0; j < currentHandler.values.length; j++) {


                        var currentValue = currentHandler.values[j];

                        //Not empty
                        if (currentValue.text.length > 0) {
                            var text = currentValue.text;

                            //Has type and it is not XMLSchema -> wrap in "[[" and "]]" to make a link.
                            if (currentValue.type && currentValue.type.indexOf("XMLSchema") == -1 && currentValue.type.indexOf("langString") == -1) {

                                text = "[[ " + text + " ]]";

                            }
                            else if (currentValue.text.indexOf("http://") !== -1) {
                                //URL
                                text = "[ " + text + " ]";
                            }
                            else if (currentValue.type && currentValue.type.indexOf("Year") !== -1) {
                                //Years are linked too
                                text = "[[ " + text + " ]]";
                            }

                            if (currentProperty !== currentHandler.propertyURI) {
                                //New property
                                currentProperty = currentHandler.propertyURI;
                                result += "\n| " + currentHandler.propertyLabel + " = " + text;

                            }
                            else {
                                //Same property, we are creating a list
                                result += "<br/>" + text;
                            }

                        }

                    }
                }

                result += "\n}}"; //Close infobox
                return result;

            }


            /* Returns a string with the infobox code for MediaWiki (Customized) */
            self.generateWikimediaInfobox = function (pageName, propertyHandlers) {

                var currentProperty; //To check more than one value for one property
                var result = "";

                var labelNumber = 1;
                var cats = AppStateService.getCategoryList();

                result += "{{ infobox  ";//Infobox template, currently first category
                result += "\n| title = " + pageName; //Infobox title = page name

                //For each property
                for (var i = 0; i < propertyHandlers.length; i++) {
                    var currentHandler = propertyHandlers[i];

                    //For each value
                    for (var j = 0; j < currentHandler.values.length; j++) {


                        var currentValue = currentHandler.values[j];

                        //Not empty
                        if (currentValue.text.length > 0) {
                            var text = currentValue.text;

                            //Has type and it is not XMLSchema or langString-> wrap in "[[" and "]]" to make a link.
                            if (currentValue.type && currentValue.type.indexOf("XMLSchema") == -1 && currentValue.type.indexOf("langString") == -1) {

                                var wikipediaUri = ToolsService.transformUriToPrefix(currentValue.uri, "http://es.wikipedia.org/wiki/");
                                text = "[" + wikipediaUri + " " + text + "]";

                            }
                            else if (currentValue.text.indexOf("http://") !== -1 && currentHandler.propertyLabel != "Thumbnail") {
                                //URL
                                text = "[ " + text + " ]";
                            }
                            else if (currentValue.type && currentValue.type.indexOf("Year") !== -1) {
                                //Years are linked too
                                text = "[[ " + text + " ]]";
                            }

                            if (currentProperty !== currentHandler.propertyURI) {
                                //New property
                                currentProperty = currentHandler.propertyURI;
                                if (currentHandler.propertyLabel != "Thumbnail") {
                                    result += "\n| " + "label" + labelNumber + " = " + currentHandler.propertyLabel;
                                    result += "\n| " + "data" + labelNumber + " = " + text;
                                }
                                else {
                                    result += "\n| " + "image = {{sized-external-image|180px|" + text + "}}";
                                }

                            }
                            else {
                                //Same property, we are creating a list
                                result += "<br/>" + text;
                            }

                            labelNumber++;
                        }

                    }
                }

                result += "\n}}"; //Close infobox
                return result;

            };

            /*Returns a string with RDF triples for the current data*/
            self.generateRDF = function (pageName, propertyHandlers) {

                var triples = "";

                var cats = AppStateService.getCategoryList();
                //First, we print types.
                for (var k = 0; k < cats.length; k++) {
                    triples += textToResourceURIFilter(pageName) + "\t" + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" + "\t" + cats[k] + " .\n";
                }


                //For each propertyHandler
                for (var i = 0; i < propertyHandlers.length; i++) {
                    var currentHandler = propertyHandlers[i];

                    //For each value
                    for (var j = 0; j < currentHandler.values.length; j++) {

                        var currentValue = currentHandler.values[j];
                        //Only output if is input is not empty
                        if (currentValue.text.length > 0) {

                            var currentTriple = textToResourceURIFilter(pageName) + "\t" + currentHandler.propertyURI + "\t";

                            //If type contains XMLSchema -> value^^type
                            if (currentValue.type && currentValue.type.indexOf("XMLSchema#") !== -1) {

                                var text = currentValue.text;
                                console.log(currentValue.type);
                                if (currentValue.type.indexOf("XMLSchema#String") !== -1 || currentValue.type.indexOf("XMLSchema#string") !== -1  ) {
                                    //Quotes if it is a string
                                    text = "\"" + text + "\"";
                                }
                                currentTriple += text + "^^" + currentValue.type;
                            }
                            else {
                                currentTriple += currentValue.uri;
                            }

                            currentTriple += " .\n";
                            triples += currentTriple;
                        }
                    }
                }


                return triples;
            };


            /**Returns HTML code representing the Infobox for the current data*/
            self.generateHTML = function (pageName, propertyHandlers) {
                var codeInfoBox = "<table id='infoboxerInfobox' class='infobox vcard' cellspacing='3' style='border-spacing:3px;width:22em;'>" +
                    "<tbody>" +
                    "<tr>" +
                    "<th colspan='2' class='fn' style='text-align:center;font-size:125%;font-weight:bold;background:transparent;text-align:center;;'>" + pageName + "</th>" +
                    "</tr>";

                var thumbNailCode = "";
                var textCode = "";

                //For each propertyHandler
                for (var i = 0; i < propertyHandlers.length; i++) {
                    var currentHandler = propertyHandlers[i];

                    //For each value
                    for (var j = 0; j < currentHandler.values.length; j++) {

                        var currentValue = currentHandler.values[j];
                        if (currentValue.text.length > 0 && currentHandler.propertyURI.indexOf('thumbnail') === -1) {
                            //Text property value

                            var currentPropURI = URItoURLFilter(currentHandler.propertyURI);
                            var currentPropLabel = currentHandler.propertyLabel;
                            var currentValueURI = URItoURLFilter(currentValue.uri);
                            textCode +=
                                //Property title
                                "<tr>" +
                                "<th scope='row' style='text-align:left;background:transparent; padding-top:0.225em;line-height:1.1em; padding-right:0.5em;;'>" +
                                "<a href='" + currentPropURI + "' title='" + currentPropLabel + "' target='_blank'>" + currentPropLabel + "</a>" +
                                "</th>";

                            //Property value
                            textCode += "<td style='vertical-align:middle;line-height:1.3em;;'>";
                            if (currentValue.type.indexOf('XMLSchema') !== -1 || currentValue.type.indexOf("langString") !== -1 || !currentValue.type) {
                                textCode += "<span>" + currentValue.text + "</span>";
                            }
                            else {
                                textCode += "<a href='" + currentValueURI + "' target='_blank' title='" + currentValue.text + "'>" + currentValue.text + "</a>";
                            }
                            textCode += "</td></tr>";


                        }
                        else if (currentValue.text.length > 0 && currentHandler.propertyURI.indexOf('thumbnail') !== -1) {
                            //Thumbnail value
                            thumbNailCode += "<tr>" +
                                "<td colspan='2' style='text-align:center;padding-bottom:0.5em;;'>" +
                                "<a href='" + currentValue.text + "' class='image'>" +
                                "<img alt='" + currentValue.text + "' src='" + currentValue.text + "' width='108' height='108' data-file-width='108' data-file-height='108' />" +
                                "</a></td></tr>";

                        }

                    }

                }
                codeInfoBox += thumbNailCode + textCode + "</tbody></table>";
                return codeInfoBox;
            };

        }]);
