'use strict';

/**
 * Controller for /wiki, a page made for test speed on writing a Mediawiki infobox code.
 */
angular.module('infoboxer')

    .controller('WikiCtrl', ['$interval', '$routeParams', 'StatsService', function ($interval, $routeParams, StatsService) {

        var self = this;

        self.person = $routeParams.person;
        self.availableMinutes = $routeParams.availableMinutes;
        self.username = ""; //Username
        self.infobox = ""; //Infobox
        self.usedTime = 0;
        self.counting = false;
        self.interval = null;
        self.finished = false;

        self.originalInfobox =
            "{{ infobox \n" +
            "| title = \n" +
            "| label1 = Name\n" +
            "| data1 = INSERT_TEXT\n" +
            "| label2 = Occupation\n" +
            "| data2 = INSERT_TEXT\n" +
            "| label3 = Birth Place\n" +
            "| data3 = INSERT_TEXT\n" +
            "| label4 = Death Place\n" +
            "| data4 = INSERT_TEXT\n" +
            "| label5 = Nationality\n" +
            "| data5 = INSERT_TEXT\n" +
            "| label6 = Position\n" +
            "| data6 = INSERT_TEXT\n" +
            "}}";


        self.infobox = self.originalInfobox;


        //Starts or reset the counter and textarea
        self.start = function () {


            if (self.username.length <= 0) {
                alert("Please, enter a username");
            }
            else {
                self.infobox = self.originalInfobox;
                self.usedTime = 0;
                self.counting = true;


                //Start interval or reset it
                if (self.interval != null) {
                    $interval.cancel(self.interval);
                }

                self.interval = $interval(function () {
                    self.usedTime++;
                }, 1000, 0);

            }

        };

        self.submit = function () {
            //Send data to endpoint

            StatsService.saveWikimediaTime(self.username, self.infobox, self.usedTime)
                .then(function () {
                    self.finished = true;
                });

        };

        self.reset = function () {
            if (confirm("Are you sure you want to reset?\nAny previous work will be reset")) {
                self.start();
            }
        };


    }]);