'use strict';

angular.module('infoboxer')
    /**
     * This service contains HTTP funcions to comunicate with the Stats Server.
     * TODO: Add return code control -> Not allow to add a registry if no session has been started.
     * Add error control.
     */
    .service('StatsService', ['$http', '$q', 'ConfigService', function ($http, $q, ConfigService) {

        var self = this;

        var c = ConfigService;

        this.username = "anonymous";
        this.sessionId = undefined;

        this.stats_activated = ConfigService.stats.activated;

        var URL = c.endpoint.base;

        var URL_newAction = URL + c.endpoint.stats.newAction;
        var URL_newSession = URL + c.endpoint.stats.newSession;
        var URL_closeSession = URL + c.endpoint.stats.closeSession;
        var URL_saveInfobox = URL + c.endpoint.stats.saveInfobox;
        var URL_generateInfobox = URL + c.endpoint.generateInfobox;
        var URL_saveRdf = URL + c.endpoint.stats.saveRdf;
        var URL_saveSurvey = URL + c.endpoint.stats.saveSurvey;
        var URL_wikimediaTime = URL + c.endpoint.stats.wikimediaTime;


        /**
         Creates a new session with the given username.
         Returns a promise that passes an integer to the callback function
         with the sessionId.
         */
        this.newSession = function (username) {
            username = username.replace("#", "%23");
            self.username = username;



            //Returns session Id.


            return this.addAction("SYSTEM INFORMATION", "DUMMY REGISTER","DUMMY",-1)
                .then(function(response){
                        return $http.post(URL_newSession + "?username=" + username + "&timestamp=" + new Date().getTime())

                }

                )
                .then(
                    function (response) {

                        if (response.data.status == "OK") {
                            //All right
                            self.sessionId = response.data.sessionId;
                            console.log("[STATS] Session started. Username: " + username + " - sessionId: " + self.sessionId);
                            return self.sessionId;
                        }
                        else {
                            //Error obtaining ID...
                            //We set a random ID
                            self.sessionId = Math.floor((Math.random() * 10000) + 1);
                            console.log("[STATS] Error creating session. Random session ID: " + self.sessionId);
                            //alert("Error al contactar con el servidor de estadísticas. Contacta con un responsable.");
                        }

                    }
                )
                .catch(function (data) {
                    console.error("Error while registering new session.");
                });


        };


        /**
         * Restores local data from session -> Last session is forgiven.
         * Adds a remote register of {"SYSTEM INFORMATION", "SESSION CLOSED",""}
         */
        this.closeSession = function () {

            //Create new action

            return $http.post(URL_closeSession + "?sessionId=" + self.sessionId + "&timestamp=" + new Date().getTime()).then(
                function (response) {

                    if (response.data.status == "OK") {
                        console.log("[STATS] Session closed.");
                    }
                    else {
                        console.log("[STATS] Error closing session (ID: " + self.sessionId + ")");
                    }
                    //Forget data about the last session
                    //self.username = "anonymous";
                    self.sessionId = undefined;

                }
            ).catch(function (data) {
                console.log("[STATS] Error closing session (ID: " + self.sessionId + ")");
                //Forget data about the last session
                //self.username = "anonymous";
                self.sessionId = undefined;

            });
        };


        /**
         * Saves the edited content as RDF
         */
        this.saveRdf = function (pageName, categories, rdfCode) {

            pageName = pageName.replace("#", "%23");
            categories = categories.replace("#", "%23");
            rdfCode = rdfCode.replace("#", "%23");

            if (self.stats_activated) {

                var payload =
                {

                    sessionId: self.sessionId,
                    timestamp: new Date().getTime(),
                    categories: categories,
                    pageName: pageName,
                    rdfCode: rdfCode
                };

                return $http({
                    method: 'POST',
                    data: payload,
                    url: URL_saveRdf,
                    headers: {'Content-Type': 'application/json'}

                })
                    .then(function (response) {

                        //Todo: check error code
                        if (response.data.status == "OK") {
                            console.log("[STATS] Saved RDF!");
                        }
                        else {
                            console.log("[STATS] Error saving RDF.");
                        }
                    });

            }
            else {
                return $q.when("stats not activated");
            }

        };


        /**
         * Given a infobox code of the filled properties, a page name
         * and a category (only one), sends it to the server to save it.
         */
        this.saveInfobox = function(pageName,categories,infoboxCode){

            pageName = pageName.replace("#", "%23");
            categories = categories.replace("#", "%23");
            var payload =
            {

                sessionId: self.sessionId,
                timestamp: new Date().getTime(),
                categories: categories,
                pageName: pageName,
                infoboxCode: infoboxCode
            };

            return $http({
                method: 'POST',
                data: payload,
                url: URL_saveInfobox,
                headers: {'Content-Type': 'application/json'}

            })
                .then(function success(response) {

                    if(response.data.status=="ERROR"){
                        console.log("[STATS] Error saving infobox: " + response.data.message);
                    }
                    else{
                        console.log("[STATS] Infobox saved!");
                    }

                }, function error(response){

                    console.log("[STATS] Error saving infobox");

                });


        };

        /**
         * Given a infobox code of the filled properties, a page name
         * and a category (only one), it request to the server the
         * correct infobox that follows a template.
         */
        this.generateInfobox = function (pageName, category, givenInfobox) {

            pageName = pageName.replace("#", "%23");
            category = category.replace("#", "%23");
            //rdfCode = rdfCode.replace("#","%23");


            var config = {};
            if (self.stats_activated) {

                var payload =
                {

                    sessionId: self.sessionId,
                    timestamp: new Date().getTime(),
                    category: category,
                    pageName: pageName,
                    givenInfobox: givenInfobox
                };
            }
            else {


                var payload =
                {

                    category: category,
                    pageName: pageName,
                    givenInfobox: givenInfobox
                };

            }


            return $http({
                method: 'POST',
                data: payload,
                url: URL_generateInfobox,
                headers: {'Content-Type': 'application/json'}

            })
                .then(function (response) {

                    return response.data.resultInfobox;
                });


        };


        /**
         * Saves a survey with 3 numeric responses and a free text.
         */
        this.saveSurvey = function (response1, response2, response3, freeText) {

            freeText = freeText.replace("#", "%23");

            if (self.stats_activated) {

                return $http.post(URL_saveSurvey + "?sessionId=" + self.sessionId + "&timestamp=" + new Date().getTime()
                        + "&response1=" + response1 + "&response2=" + response2 + "&response3=" + response3 + "&freeText=" + freeText)
                    .then(function (response) {

                        //Todo: check error code
                        if (response.data.status == "OK") {
                            console.log("[STATS] Survey saved!");
                        }
                        else {
                            console.log("[STATS] Error saving survey.");
                        }
                    });

            }
            else {
                return undefined;
            }

        }


        /**
         * Adds a new action.
         */
        this.addAction = function (subject, action, value,givenSessionId) {

            if(givenSessionId==undefined){
                givenSessionId = self.sessionId;
            }

            if (!value) {
                value = "";
            }
            subject = subject.replace("#", "%23");
            action = action.replace("#", "%23");
            value = value.replace("#", "%23");

            if (self.stats_activated) {

                return $http.post(URL_newAction + "?sessionId=" + givenSessionId + "&timestamp=" + new Date().getTime()
                    + "&subject=" + subject + "&action=" + action + "&value=" + value).then(
                    function (response) {

                        //Todo: check error code
                        if (response.data.status == "OK") {
                            console.log("[STATS] Added action {" + subject + "," + action + "," + value + "}.");
                        }
                        else {
                            console.log("[STATS] Error adding action {" + subject + "," + action + "," + value + "}.");
                        }


                    }
                );
            }
            else {
                //Stats are not activated, no call is made
                return $q.when("stats not activated");
            }

        }


        this.saveWikimediaTime = function (username, infobox, time) {

            username = username.replace("#", "%23");
            infobox = infobox.replace("#", "%23");


            var payload =
            {

                username: username,
                infobox: infobox,
                time: time
            };

            return $http({
                method: 'POST',
                data: payload,
                url: URL_wikimediaTime,
                headers: {'Content-Type': 'application/json'}

            })
                .then(function (response) {

                    //Todo: check error code
                    if (response.data.status == "OK") {
                        console.log("[STATS] Saved Wikimedia Time and data!");
                    }
                    else {
                        console.log("[STATS] Error saving Wikimedia Time and data.");
                    }
                });


        };


    }]);
