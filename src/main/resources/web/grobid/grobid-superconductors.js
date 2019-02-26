/**
 *  Javascript functions for the front end.
 *
 *  Author: Patrice Lopez
 */

var grobid = (function ($) {

        // for components view
        var responseJson = null;

        // for associating several quantities to a measurement
        var superconMap = new Array();
        var measurementMap = new Array();
        var abbreviationsMap = new Array();

        function defineBaseURL(ext) {
            var baseUrl = null;
            if ($(location).attr('href').indexOf("index.html") != -1)
                baseUrl = $(location).attr('href').replace("index.html", ext);
            else
                baseUrl = $(location).attr('href') + ext;
            return baseUrl;
        }

        function setBaseUrl(ext) {
            var baseUrl = defineBaseURL('service' + '/' + ext);
            $('#gbdForm').attr('action', baseUrl);
        }

        $(document).ready(function () {

            $("#subTitle").html("About");
            $("#divAbout").show();
            $("#divRestI").hide();
            $("#divDoc").hide();
            $('#consolidateBlock').show();

            createInputTextArea('text');
            setBaseUrl('processSuperconductorsText');
            $('#example0').bind('click', function (event) {
                event.preventDefault();
                $('#inputTextArea').val(examples[0]);
            });
            setBaseUrl('processQuantityText');
            $('#example1').bind('click', function (event) {
                event.preventDefault();
                $('#inputTextArea').val(examples[1]);
            });
            $('#example2').bind('click', function (event) {
                event.preventDefault();
                $('#inputTextArea').val(examples[2]);
            });
            $('#example3').bind('click', function (event) {
                event.preventDefault();
                $('#inputTextArea').val(examples[3]);
            });
            $("#selectedService").val('processSuperconductorsText');

            $('#selectedService').change(function () {
                processChange();
                return true;
            });

            $('#submitRequest').bind('click', submitQuery);

            $("#about").click(function () {
                $("#about").attr('class', 'section-active');
                $("#rest").attr('class', 'section-not-active');
                $("#doc").attr('class', 'section-not-active');
                $("#demo").attr('class', 'section-not-active');

                $("#subTitle").html("About");
                $("#subTitle").show();

                $("#divAbout").show();
                $("#divRestI").hide();
                $("#divDoc").hide();
                $("#divDemo").hide();
                return false;
            });
            $("#rest").click(function () {
                $("#rest").attr('class', 'section-active');
                $("#doc").attr('class', 'section-not-active');
                $("#about").attr('class', 'section-not-active');
                $("#demo").attr('class', 'section-not-active');

                $("#subTitle").hide();
                //$("#subTitle").show();
                processChange();

                $("#divRestI").show();
                $("#divAbout").hide();
                $("#divDoc").hide();
                $("#divDemo").hide();
                return false;
            });
            $("#doc").click(function () {
                $("#doc").attr('class', 'section-active');
                $("#rest").attr('class', 'section-not-active');
                $("#about").attr('class', 'section-not-active');
                $("#demo").attr('class', 'section-not-active');

                $("#subTitle").html("Doc");
                $("#subTitle").show();

                $("#divDoc").show();
                $("#divAbout").hide();
                $("#divRestI").hide();
                $("#divDemo").hide();
                return false;
            });
            $("#demo").click(function () {
                $("#demo").attr('class', 'section-active');
                $("#rest").attr('class', 'section-not-active');
                $("#about").attr('class', 'section-not-active');
                $("#doc").attr('class', 'section-not-active');

                $("#subTitle").html("Demo");
                $("#subTitle").show();

                $("#divDemo").show();
                $("#divDoc").hide();
                $("#divAbout").hide();
                $("#divRestI").hide();
                return false;
            });
        });

        function ShowRequest(formData, jqForm, options) {
            var queryString = $.param(formData);
            $('#requestResult').html('<font color="grey">Requesting server...</font>');
            return true;
        }

        function AjaxError(jqXHR, textStatus, errorThrown) {
            $('#requestResult').html("<font color='red'>Error encountered while requesting the server.<br/>" + jqXHR.responseText + "</font>");
            responseJson = null;
        }

        function AjaxError2(message) {
            if (!message)
                message = "";
            message += " - The PDF document cannot be annotated. Please check the server logs.";
            $('#infoResult').html("<font color='red'>Error encountered while requesting the server.<br/>" + message + "</font>");
            responseJson = null;
            return true;
        }

        function htmll(s) {
            return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }

        function submitQuery() {
            var selected = $('#selectedService option:selected').attr('value');
            var urlLocal = $('#gbdForm').attr('action');

            superconMap = new Array();

            $('#infoResult').html('<font color="grey">Requesting server...</font>');
            $('#requestResult').html('');

            if (selected == 'processSuperconductorsText') {
                var formData = new FormData();
                formData.append("text", $('#inputTextArea').val());

                $.ajax({
                    type: 'POST',
                    url: urlLocal,
                    data: formData,
                    success: SubmitSuccesful,
                    error: AjaxError,
                    contentType: false,
                    processData: false
                });
            } else if (selected == 'annotateSuperconductorsPDF') {
                // we will have JSON annotations to be layered on the PDF

                // request for the annotation information
                var form = document.getElementById('gbdForm');
                var formData = new FormData(form);
                var xhr = new XMLHttpRequest();
                var url = $('#gbdForm').attr('action');
                xhr.responseType = 'json';
                xhr.open('POST', url, true);

                var nbPages = -1;

                // display the local PDF
                if ((document.getElementById("input").files[0].type == 'application/pdf') ||
                    (document.getElementById("input").files[0].name.endsWith(".pdf")) ||
                    (document.getElementById("input").files[0].name.endsWith(".PDF")))
                    var reader = new FileReader();
                reader.onloadend = function () {
                    // to avoid cross origin issue
                    //PDFJS.disableWorker = true;
                    var pdfAsArray = new Uint8Array(reader.result);
                    // Use PDFJS to render a pdfDocument from pdf array
                    PDFJS.getDocument(pdfAsArray).then(function (pdf) {
                        // Get div#container and cache it for later use
                        var container = document.getElementById("requestResult");
                        // enable hyperlinks within PDF files.
                        //var pdfLinkService = new PDFJS.PDFLinkService();
                        //pdfLinkService.setDocument(pdf, null);

                        //$('#requestResult').html('');
                        nbPages = pdf.numPages;

                        // Loop from 1 to total_number_of_pages in PDF document
                        for (var i = 1; i <= nbPages; i++) {

                            // Get desired page
                            pdf.getPage(i).then(function (page) {
                                var table = document.createElement("table");
                                var tr = document.createElement("tr");
                                var td1 = document.createElement("td");
                                var td2 = document.createElement("td");

                                tr.appendChild(td1);
                                tr.appendChild(td2);
                                table.appendChild(tr);

                                var div0 = document.createElement("div");
                                div0.setAttribute("style", "text-align: center; margin-top: 1cm; width:80%;");
                                var pageInfo = document.createElement("p");
                                var t = document.createTextNode("page " + (page.pageIndex + 1) + "/" + (nbPages));
                                pageInfo.appendChild(t);
                                div0.appendChild(pageInfo);

                                td1.appendChild(div0);

                                var scale = 1.5;
                                var viewport = page.getViewport(scale);
                                var div = document.createElement("div");

                                // Set id attribute with page-#{pdf_page_number} format
                                div.setAttribute("id", "page-" + (page.pageIndex + 1));

                                // This will keep positions of child elements as per our needs, and add a light border
                                div.setAttribute("style", "position: relative; ");


                                // Create a new Canvas element
                                var canvas = document.createElement("canvas");
                                canvas.setAttribute("style", "border-style: solid; border-width: 1px; border-color: gray;");

                                // Append Canvas within div#page-#{pdf_page_number}
                                div.appendChild(canvas);

                                // Append div within div#container
                                td1.appendChild(div);

                                var annot = document.createElement("div");
                                annot.setAttribute('style', 'vertical-align:top;');
                                annot.setAttribute('id', 'detailed_annot-' + (page.pageIndex + 1));
                                td2.setAttribute('style', 'vertical-align:top;');
                                td2.appendChild(annot);

                                container.appendChild(table);

                                var context = canvas.getContext('2d');
                                canvas.height = viewport.height;
                                canvas.width = viewport.width;

                                var renderContext = {
                                    canvasContext: context,
                                    viewport: viewport
                                };

                                // Render PDF page
                                page.render(renderContext).then(function () {
                                    // Get text-fragments
                                    return page.getTextContent();
                                })
                                    .then(function (textContent) {
                                        // Create div which will hold text-fragments
                                        var textLayerDiv = document.createElement("div");

                                        // Set it's class to textLayer which have required CSS styles
                                        textLayerDiv.setAttribute("class", "textLayer");

                                        // Append newly created div in `div#page-#{pdf_page_number}`
                                        div.appendChild(textLayerDiv);

                                        // Create new instance of TextLayerBuilder class
                                        var textLayer = new TextLayerBuilder({
                                            textLayerDiv: textLayerDiv,
                                            pageIndex: page.pageIndex,
                                            viewport: viewport
                                        });

                                        // Set text-fragments
                                        textLayer.setTextContent(textContent);

                                        // Render text-fragments
                                        textLayer.render();
                                    });
                            });
                        }
                    });
                };
                reader.readAsArrayBuffer(document.getElementById("input").files[0]);

                xhr.onreadystatechange = function (e) {
                    if (xhr.readyState == 4 && xhr.status == 200) {
                        var response = e.target.response;
                        //var response = JSON.parse(xhr.responseText);
                        //console.log(response);
                        setupAnnotations(response);
                    } else if (xhr.status != 200) {
                        AjaxError2("Response " + xhr.status + ": ");
                    }
                };
                xhr.send(formData);
            }
        }

        function SubmitSuccesful(responseText, statusText) {
            var selected = $('#selectedService option:selected').attr('value');

            if (selected == 'processSuperconductorsText') {
                SubmitSuccesfulText(responseText, statusText);
            }
        }

        function SubmitSuccesfulText(responseText, statusText) {
            responseJson = responseText;
            //console.log(responseJson);
            $('#infoResult').html('');
            if ((responseJson == null) || (responseJson.length == 0)) {
                $('#requestResult')
                    .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
                return;
            }

            //responseJson = jQuery.parseJSON(responseJson);

            var display = '<div class=\"note-tabs\"> \
            <ul id=\"resultTab\" class=\"nav nav-tabs\"> \
                <li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li> \
                <li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
            </ul> \
            <div class="tab-content"> \
            <div class="tab-pane active" id="navbar-fixed-annotation">\n';

            display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';

            var string = $('#inputTextArea').val();
            var newString = "";
            var lastMaxIndex = string.length;

            display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">';
            //var string = responseJson.text;

            display += '<tr style="background-color:#FFF;">';
            var superconductors = responseJson.superconductors;
            if (superconductors) {
                var pos = 0; // current position in the text

                for (var currentSuperconIndex = 0; currentSuperconIndex < superconductors.length; currentSuperconIndex++) {
                    var currentSuperconductor = superconductors[currentSuperconIndex];
                    if (currentSuperconductor) {
                        var name = currentSuperconductor.name;
                        var startUnit = -1;
                        var endUnit = -1;
                        var start = parseInt(currentSuperconductor.offsetStart, 10);
                        var end = parseInt(currentSuperconductor.offsetEnd, 10);
                        if ((startUnit != -1) && ((startUnit == end) || (startUnit == end + 1)))
                            end = endUnit;
                        if ((endUnit != -1) && ((endUnit == start) || (endUnit + 1 == start)))
                            start = startUnit;

                        if (start < pos) {
                            // we have a problem in the initial sort of the quantities
                            // the server response is not compatible with the present client
                            console.log("Sorting of quantities as present in the server's response not valid for this client.");
                            // note: this should never happen?
                        } else {
                            newString += string.substring(pos, start)
                                + ' <span id="annot-' + currentSuperconIndex + '" rel="popover" data-color="interval">'
                                + '<span class="label interval" style="cursor:hand;cursor:pointer;" >'
                                + string.substring(start, end) + '</span></span>';
                            pos = end;
                        }
                        superconMap[currentSuperconIndex] = currentSuperconductor;
                    }
                }
                newString += string.substring(pos, string.length);
            }

            newString = "<p>" + newString.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
            //string = string.replace("<p></p>", "");

            display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>' + newString + '</p></td>';
            display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0" /></td>';

            display += '</tr>';


            display += '</table>\n';


            display += '</pre>\n';


            display += '</div> \
                    <div class="tab-pane " id="navbar-fixed-json">\n';


            display += "<pre class='prettyprint' id='jsonCode'>";

            display += "<pre class='prettyprint lang-json' id='xmlCode'>";
            var testStr = vkbeautify.json(responseText);

            display += htmll(testStr);

            display += "</pre>";
            display += '</div></div></div>';

            $('#requestResult').html(display);
            window.prettyPrint && prettyPrint();

            if (superconductors) {
                for (var measurementIndex = 0; measurementIndex < superconductors.length; measurementIndex++) {
                    // var measurement = measurements[measurementIndex];

                    $('#annot_supercon-' + measurementIndex).bind('hover', viewSuperconductor);
                    $('#annot_supercon-' + measurementIndex).bind('click', viewSuperconductor);
                }
            }
            /*for (var key in quantityMap) {
             if (entityMap.hasOwnProperty(key)) {
             $('#annot-'+key).bind('hover', viewQuantity);
             $('#annot-'+key).bind('click', viewQuantity);
             }
             }*/

            $('#detailed_annot-0').hide();

            $('#requestResult').show();
        }

        function setupAnnotations(response) {
            // TBD: we must check/wait that the corresponding PDF page is rendered at this point
            if ((response == null) || (response.length == 0)) {
                $('#infoResult')
                    .html("<font color='red'>Error encountered while receiving the server's answer: response is empty.</font>");
                return;
            } else {
                $('#infoResult').html('');
            }

            var json = response;
            var pageInfo = json['pages'];

            var page_height = 0.0;
            var page_width = 0.0;

            // Superconductors
            var superconductors = json.superconductors;
            if (superconductors) {
                // hey bro, this must be asynchronous to avoid blocking the brothers
                superconductors.forEach(function (superconductor, superconIdx) {
                    superconMap[superconIdx] = superconductor;

                    //var theId = measurement.type;
                    var theUrl = null;
                    //var theUrl = annotation.url;
                    var pos = superconductor.boundingBoxes;
                    if ((pos != null) && (pos.length > 0)) {
                        pos.forEach(function (thePos, positionIdx) {
                            // get page information for the annotation
                            var pageNumber = thePos.p;
                            if (pageInfo[pageNumber - 1]) {
                                page_height = pageInfo[pageNumber - 1].page_height;
                                page_width = pageInfo[pageNumber - 1].page_width;
                            }
                            annotateSuperconductor(thePos, theUrl, page_height, page_width, superconIdx, positionIdx);
                        });
                    }
                });
            }

            // Abbreviations
            var abbreviations = json.abbreviations;
            if (abbreviations) {
                // hey bro, this must be asynchronous to avoid blocking the brothers
                abbreviations.forEach(function (abbreviation, abbreviationIdx) {
                    abbreviationsMap[abbreviationIdx] = abbreviation;

                    //var theId = measurement.type;
                    var theUrl = null;
                    //var theUrl = annotation.url;
                    var pos = abbreviation.boundingBoxes;
                    if ((pos != null) && (pos.length > 0)) {
                        pos.forEach(function (thePos, positionIdx) {
                            // get page information for the annotation
                            var pageNumber = thePos.p;
                            if (pageInfo[pageNumber - 1]) {
                                page_height = pageInfo[pageNumber - 1].page_height;
                                page_width = pageInfo[pageNumber - 1].page_width;
                            }
                            annotateAbbreviation(thePos, theUrl, page_height, page_width, abbreviationIdx, positionIdx);
                        });
                    }
                });
            }


            // Temperatures
            var temperatures = json.temperatures;
            if (temperatures) {
                // hey bro, this must be asynchronous to avoid blocking the brothers
                temperatures.forEach(function (measurement, n) {
                    var measurementType = measurement.type;
                    var quantities = [];
                    var substance = measurement.quantified;

                    if (measurementType == "value") {
                        var quantity = measurement.quantity;
                        if (quantity)
                            quantities.push(quantity)
                    } else if (measurementType == "interval") {
                        var quantityLeast = measurement.quantityLeast;
                        if (quantityLeast)
                            quantities.push(quantityLeast);
                        var quantityMost = measurement.quantityMost;
                        if (quantityMost)
                            quantities.push(quantityMost);

                        if (!quantityLeast && !quantityMost) {
                            var quantityBase = measurement.quantityBase;
                            if (quantityBase)
                                quantities.push(quantityBase);
                            var quantityRange = measurement.quantityRange;
                            if (quantityRange)
                                quantities.push(quantityRange);
                        }
                    } else {
                        quantities = measurement.quantities;
                    }

                    var quantityType = null;
                    if (quantities) {
                        var quantityMap = new Array();
                        for (var currentQuantityIndex = 0; currentQuantityIndex < quantities.length; currentQuantityIndex++) {
                            var quantity = quantities[currentQuantityIndex];
                            quantity['quantified'] = substance;
                            quantityMap[currentQuantityIndex] = quantity;
                            if (quantityType == null)
                                quantityType = quantity.type;
                        }
                    }

                    measurementMap[n] = quantities;

                    //var theId = measurement.type;
                    var theUrl = null;
                    //var theUrl = annotation.url;
                    var pos = measurement.boundingBoxes;
                    if ((pos != null) && (pos.length > 0)) {
                        pos.forEach(function (thePos, m) {
                            // get page information for the annotation
                            var pageNumber = thePos.p;
                            if (pageInfo[pageNumber - 1]) {
                                page_height = pageInfo[pageNumber - 1].page_height;
                                page_width = pageInfo[pageNumber - 1].page_width;
                            }
                            annotateQuantity(quantityType, thePos, theUrl, page_height, page_width, n, m);
                        });
                    }
                });
            }
        }

        function annotateQuantity(theId, thePos, theUrl, page_height, page_width, measurementIndex, positionIndex) {
            var page = thePos.p;
            var pageDiv = $('#page-' + page);
            var canvas = pageDiv.children('canvas').eq(0);

            var canvasHeight = canvas.height();
            var canvasWidth = canvas.width();
            var scale_x = canvasHeight / page_height;
            var scale_y = canvasWidth / page_width;

            var x = thePos.x * scale_x - 1;
            var y = thePos.y * scale_y - 1;
            var width = thePos.w * scale_x + 1;
            var height = thePos.h * scale_y + 1;

            //make clickable the area
            theId = "" + theId;
            if (theId)
                theId = theId.replace(" ", "_");
            var element = document.createElement("a");
            var attributes = "display:block; width:" + width + "px; height:" + height + "px; position:absolute; top:" +
                y + "px; left:" + x + "px;";
            element.setAttribute("style", attributes + "border:2px solid; border-color: " + getColor(theId) + ";");
            element.setAttribute("class", theId);
            element.setAttribute("id", 'annot_quantity-' + measurementIndex + '-' + positionIndex);
            element.setAttribute("page", page);

            pageDiv.append(element);

            $('#annot_quantity-' + measurementIndex + '-' + positionIndex).bind('hover', viewQuantityPDF);
            $('#annot_quantity-' + measurementIndex + '-' + positionIndex).bind('click', viewQuantityPDF);
        }

        function annotateAbbreviation(thePos, theUrl, page_height, page_width, superconIdx, positionIdx) {
            var page = thePos.p;
            var pageDiv = $('#page-' + page);
            var canvas = pageDiv.children('canvas').eq(0);
            //var canvas = pageDiv.find('canvas').eq(0);;

            var canvasHeight = canvas.height();
            var canvasWidth = canvas.width();
            var scale_x = canvasHeight / page_height;
            var scale_y = canvasWidth / page_width;

            var x = thePos.x * scale_x - 1;
            var y = thePos.y * scale_y - 1;
            var width = thePos.w * scale_x + 1;
            var height = thePos.h * scale_y + 1;

            //make clickable the area
            var element = document.createElement("a");
            var attributes = "display:block; width:" + width + "px; height:" + height + "px; position:absolute; top:" +
                y + "px; left:" + x + "px;";
            element.setAttribute("style", attributes + "border:2px solid; border-color: " + getColor('volume') + ";");
            element.setAttribute("class", 'volume');
            element.setAttribute("id", 'annot_abbreviation-' + superconIdx + '-' + positionIdx);
            element.setAttribute("page", page);

            pageDiv.append(element);

            $('#annot_abbreviation-' + superconIdx + '-' + positionIdx).bind('hover', abbreviationsMap, viewEntityPDF);
            $('#annot_abbreviation-' + superconIdx + '-' + positionIdx).bind('click', abbreviationsMap, viewEntityPDF);
        }

        function annotateSuperconductor(thePos, theUrl, page_height, page_width, superconIdx, positionIdx) {
            var page = thePos.p;
            var pageDiv = $('#page-' + page);
            var canvas = pageDiv.children('canvas').eq(0);
            //var canvas = pageDiv.find('canvas').eq(0);;

            var canvasHeight = canvas.height();
            var canvasWidth = canvas.width();
            var scale_x = canvasHeight / page_height;
            var scale_y = canvasWidth / page_width;

            var x = thePos.x * scale_x - 1;
            var y = thePos.y * scale_y - 1;
            var width = thePos.w * scale_x + 1;
            var height = thePos.h * scale_y + 1;

            //make clickable the area
            var element = document.createElement("a");
            var attributes = "display:block; width:" + width + "px; height:" + height + "px; position:absolute; top:" +
                y + "px; left:" + x + "px;";
            element.setAttribute("style", attributes + "border:2px solid; border-color: " + getColor('area') + ";");
            element.setAttribute("class", 'area');
            element.setAttribute("id", 'annot_supercon-' + superconIdx + '-' + positionIdx);
            element.setAttribute("page", page);

            pageDiv.append(element);

            $('#annot_supercon-' + superconIdx + '-' + positionIdx).bind('hover', superconMap, viewEntityPDF);
            $('#annot_supercon-' + superconIdx + '-' + positionIdx).bind('click', superconMap, viewEntityPDF);
        }

        function viewSuperconductor() {

            var localID = $(this).attr('id');

            if (responseJson.superconductors == null) {
                return;
            }

            var ind1 = localID.indexOf('-');
            var localMeasurementNumber = parseInt(localID.substring(ind1 + 1));
            if ((superconMap[localMeasurementNumber] == null) || (superconMap[localMeasurementNumber].length === 0)) {
                // this should never be the case
                console.log("Error for visualising annotation measurement with id " + localMeasurementNumber
                    + ", empty list of measurement");
            } else if ((superconMap[localMeasurementNumber] == null)) {
                // this should never be the case
                console.log("Error for visualising annotation of id " + localMeasurementNumber
                    + ", empty superconductor");
            }

            var superconductor = superconMap[localMeasurementNumber];
            var string = toHtmlSemiconductor(superconductor, -1);


            $('#detailed_annot-0-0').html(string);
            $('#detailed_annot-0-0').show();
        }

        function viewEntityPDF(param) {
            map = param.data;

            var pageIndex = $(this).attr('page');
            var localID = $(this).attr('id');

            // console.log('viewQuanityPDF ' + pageIndex + ' / ' + localID);

            var ind1 = localID.indexOf('-');
            var ind2 = localID.indexOf('-', ind1 + 1);
            var localMeasurementNumber = parseInt(localID.substring(ind1 + 1, ind2));
            //var localMeasurementNumber = parseInt(localID.substring(ind1 + 1, localID.length));
            if ((map[localMeasurementNumber] === null) || (map[localMeasurementNumber].length === 0)) {
                // this should never be the case
                console.log("Error for visualising annotation with id " + localMeasurementNumber
                    + ", empty list of measurement");
            }

            var string = toHtmlSemiconductor(map[localMeasurementNumber], $(this).position().top);

            $('#detailed_annot-' + pageIndex).html(string);
            $('#detailed_annot-' + pageIndex).show();
        }

        function viewQuantityPDF() {
            var pageIndex = $(this).attr('page');
            var localID = $(this).attr('id');

            console.log('viewQuanityPDF ' + pageIndex + ' / ' + localID);

            var ind1 = localID.indexOf('-');
            var ind2 = localID.indexOf('-', ind1 + 1);
            var localMeasurementNumber = parseInt(localID.substring(ind1 + 1, ind2));
            //var localMeasurementNumber = parseInt(localID.substring(ind1 + 1, localID.length));
            if ((measurementMap[localMeasurementNumber] == null) || (measurementMap[localMeasurementNumber].length == 0)) {
                // this should never be the case
                console.log("Error for visualising annotation measurement with id " + localMeasurementNumber
                    + ", empty list of measurement");
            }

            var quantityMap = measurementMap[localMeasurementNumber];
            console.log(quantityMap);
            var measurementType = null;
            var string = "";
            if (quantityMap.length == 1) {
                measurementType = "Atomic value";
                string = toHtml(quantityMap, measurementType, $(this).position().top);
            } else if (quantityMap.length == 2) {
                measurementType = "Interval";
                string = intervalToHtml(quantityMap, measurementType, $(this).position().top);
            } else {
                measurementType = "List";
                string = toHtml(quantityMap, measurementType, $(this).position().top);
            }

            $('#detailed_annot-' + pageIndex).html(string);
            $('#detailed_annot-' + pageIndex).show();
        }

        function toHtmlSemiconductor(superconductor, topPos) {
            var string = "";
            var first = true;

            colorLabel = superconductor.name;
            var name = superconductor.name;

            string += "<div class='info-sense-box " + colorLabel + "'";
            if (topPos != -1)
                string += " style='vertical-align:top; position:relative; top:" + topPos + "'";

            string += "<div class='container-fluid' style='background-color:#FFF;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                "<table style='width:100%;display:inline-table;'><tr style='display:inline-table;'><td>";

            if (name) {
                string += "<p>name: <b>" + name + "</b></p>";
            }

            if (superconductor.tc) {
                string += "<p>Tc: <b>" + superconductor.tc + "</b></p>";
            }

            string += "</td></tr>";
            string += "</table></div>";

            string += "</div>";

            return string;
        }

        function intervalToHtml(quantityMap, measurementType, topPos) {
            var string = "";
            var rawUnitName = null;

            // LEAST value
            var quantityLeast = quantityMap[0];
            var type = quantityLeast.type;

            var colorLabel = null;
            if (type) {
                colorLabel = type;
            } else {
                colorLabel = quantityLeast.rawName;
            }
            if (colorLabel)
                colorLabel = colorLabel.replace(" ", "_");
            var leastValue = quantityLeast.rawValue;
            var startUniLeast = -1;
            var endUnitLeast = -1;

            var unitLeast = quantityLeast.rawUnit;
            if (unitLeast) {
                rawUnitName = unitLeast.name;
                startUniLeast = parseInt(quantityLeast.offsetStart, 10);
                endUnitLeast = parseInt(quantityLeast.offsetEnd, 10);
            }
            var normalizedQuantityLeast = quantityLeast.normalizedQuantity;
            var normalizedUnit = quantityLeast.normalizedUnit;

            var substance = quantityLeast.quantified;

            // MOST value
            var quantityMost = quantityMap[1];
            var mostValue = quantityMost.rawValue;
            var startUniMost = -1;
            var endUnitMost = -1;

            var unitMost = quantityMost.rawUnit;
            if (unitMost) {
                startUniMost = parseInt(quantityMost.offsetStart, 10);
                endUnitMost = parseInt(quantityMost.offsetEnd, 10);
            }
            var normalizedQuantityMost = quantityMost.normalizedQuantity;

            if (!substance)
                substance = quantityMost.quantified;

            string += "<div class='info-sense-box " + colorLabel + "'";
            if (topPos != -1)
                string += " style='vertical-align:top; position:relative; top:" + topPos + "'";
            string += "><h2 style='color:#FFF;padding-left:10px;font-size:16;'>" + measurementType;
            string += "</h2>";
            string += "<div class='container-fluid' style='background-color:#FFF;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                "<table style='width:100%;display:inline-table;'><tr style='display:inline-table;'><td>";

            if (type) {
                string += "<p>quantity type: <b>" + type + "</b></p>";
            }

            if (leastValue || mostValue) {
                string += "<p>raw: from <b>" + leastValue + "</b> to <b>" + mostValue + "</b></p>";
            }

            if (rawUnitName) {
                string += "<p>raw unit name: <b>" + rawUnitName + "</b></p>";
            }

            if (normalizedQuantityLeast || normalizedQuantityMost) {
                string += "<p>normalized: from <b>" + normalizedQuantityLeast + "</b> to <b>"
                    + normalizedQuantityMost + "</b></p>";
            }

            if (normalizedUnit) {
                string += "<p>normalized unit: <b>" + normalizedUnit.name + "</b></p>";
            }

            if (substance) {
                string += "</td></tr><tr style='width:100%;display:inline-table;'><td style='border-top-width:1px;width:100%;border-top:1px solid #ddd;display:inline-table;'>";
                string += "<p style='display:inline-table;'>quantified (experimental):"
                string += "<table style='width:100%;display:inline-table;'><tr><td>";
                string += "<p>raw: <b>" + substance.rawName;
                string += "</b></p>";
                string += "<p>normalized: <b>" + substance.normalizedName;
                string += "</b></p></td></tr></table>";
                string += "</p>";
            }

            string += "</td><td style='align:right;bgcolor:#fff'></td></tr>";
            string += "</table></div>";

            return string;

        }

        function toHtml(quantityMap, measurementType, topPos) {
            var string = "";
            var first = true;
            for (var quantityListIndex = 0; quantityListIndex < quantityMap.length; quantityListIndex++) {

                var quantity = quantityMap[quantityListIndex];
                var type = quantity.type;

                var colorLabel = null;
                if (type) {
                    colorLabel = type;
                } else {
                    colorLabel = quantity.rawName;
                }

                var rawValue = quantity.rawValue;
                var unit = quantity.rawUnit;

                var parsedValue = quantity.parsedValue;
                var parsedValueStructure = quantity.parsedValue.structure;
                // var parsedUnit = quantity.parsedUnit;

                var normalizedQuantity = quantity.normalizedQuantity;
                var normalizedUnit = quantity.normalizedUnit;

                var substance = quantity.quantified;

                var rawUnitName = null;
                var startUnit = -1;
                var endUnit = -1;
                if (unit) {
                    rawUnitName = unit.name;
                    startUnit = parseInt(unit.offsetStart, 10);
                    endUnit = parseInt(unit.offsetEnd, 10);
                }

                if (first) {
                    string += "<div class='info-sense-box " + colorLabel + "'";
                    if (topPos != -1)
                        string += " style='vertical-align:top; position:relative; top:" + topPos + "'";
                    string += "><h2 style='color:#FFF;padding-left:10px;font-size:16;'>" + measurementType;
                    string += "</h2>";
                    first = false;
                }

                string += "<div class='container-fluid' style='background-color:#FFF;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                    "<table style='width:100%;display:inline-table;'><tr style='display:inline-table;'><td>";

                if (type) {
                    string += "<p>quantity type: <b>" + type + "</b></p>";
                }

                if (rawValue) {
                    string += "<p>raw value: <b>" + rawValue + "</b></p>";
                }

                if (parsedValue) {
                    if (parsedValue.numeric && parsedValue.numeric !== rawValue) {
                        string += "<p>parsed value: <b>" + parsedValue.numeric + "</b></p>";
                    } else if (parsedValue.parsed && parsedValue.parsed !== rawValue) {
                        string += "<p>parsed value: <b>" + parsedValue.parsed + "</b></p>";
                    }
                }

                if (parsedValueStructure) {
                    string += "<p>&nbsp;&nbsp; - type: <b>" + parsedValueStructure.type + "</b></p>";
                    string += "<p>&nbsp;&nbsp; - formatted: <b>" + parsedValueStructure.formatted + "</b></p>";
                }


                if (rawUnitName) {
                    string += "<p>raw unit name: <b>" + rawUnitName + "</b></p>";
                }

                if (normalizedQuantity) {
                    string += "<p>normalized value: <b>" + normalizedQuantity + "</b></p>";
                }

                if (normalizedUnit) {
                    string += "<p>normalized unit name: <b>" + normalizedUnit.name + "</b></p>";
                }

                if (substance) {
                    string += "</td></tr><tr style='width:100%;display:inline-table;'><td style='border-top-width:1px;width:100%;border-top:1px solid #ddd;display:inline-table;'>";
                    string += "<p style='display:inline-table;'>quantified (experimental):"
                    string += "<table style='width:100%;display:inline-table;'><tr><td>";
                    string += "<p>raw: <b>" + substance.rawName;
                    string += "</b></p>";
                    string += "<p>normalized: <b>" + substance.normalizedName;
                    string += "</b></p></td></tr></table>";
                    string += "</p>";
                }

                string += "</td></tr>";
                string += "</table></div>";
            }
            string += "</div>";

            return string;
        }

        function processChange() {
            var selected = $('#selectedService option:selected').attr('value');

            if (selected === 'processSuperconductorsText') {
                createInputTextArea();
                //$('#consolidateBlock').show();
                setBaseUrl('processSuperconductorsText');
            } else if (selected === 'annotateSuperconductorsPDF') {
                createInputFile(selected);
                //$('#consolidateBlock').hide();
                setBaseUrl('annotateSuperconductorsPDF');
            }
        }

        function createInputFile(selected) {
            $('#textInputDiv').hide();
            $('#fileInputDiv').show();

            $('#gbdForm').attr('enctype', 'multipart/form-data');
            $('#gbdForm').attr('method', 'post');
        }

        function createInputTextArea() {
            $('#fileInputDiv').hide();
            $('#textInputDiv').show();
        }

        var mapColor = {
            'area': '#87A1A8',
            'volume': '#c43c35',
            'velocity': '#c43c35',
            'fraction': '#c43c35',
            'length': '#01A9DB',
            'time': '#f89406',
            'mass': '#c43c35',
            'temperature': '#398739',
            'frequency': '#8904B1;',
            'concentration': '#31B404'
        };

        /* return a color based on the quantity type */
        function getColor(type) {
            return mapColor[type];
        }

        var examples = [
            "We report X-ray diffraction, magnetization and transport measurements for polycrystalline samples of the new layered superconductor Bi4−xAgxO4S3 (0 < x < 0.2). " +
            "The superconducting transition temperature (TC) decreases gradually and finally suppressed when x < 0.10. " +
            "Accordingly, the resistivity changes from a metallic behavior for x < 0.1 to a semiconductor-like behavior for x > 0.1. " +
            "The analysis of Seebeck coefficient shows there are two types of electron-like carriers dominate at different temperature regions, indicative of a multiband effect responsible for the transport properties. " +
            "The suppression of superconductivity and the increased resistivity can be attributed to a shift of the Fermi level to the lower-energy side upon doping, which reduces the density of states at EF. Further, our result indicates the superconductivity in Bi4O4S3 is intrinsic and the dopant Ag prefers to enter the BiS2 layers, which may essentially modify the electronic structure.",

            "Uranium compounds U6X (X 1⁄4 Mn, Fe, Co, and Ni) are superconductors with relatively high superconducting (SC) transition temperatures Tc among the uranium-based compounds. " +
            "Contrary to other uranium-based heavy-fermion systems including ferromagnetic superconductors, 5f electrons in U6X compounds exhibit an itinerant nature even at room temperature, and do not exhibit magnetic ground states. " +
            "The SC properties are consistent with the conventional one: for instance, the full-gap superconductivity of U6Co (Tc 1⁄4 2:33 K) has been indicated by the penetration depth, nuclear spin–lattice relaxation rate 1=T1, and more recently, by specific heat measurements. " +
            "Thus, at present, these compounds are good reference systems for the uranium-based unconventional superconductors.\n\n" +
            "It is also noteworthy that SC U6Co has a large upper critical field Hc2 for Tc. In spin-singlet superconductors, the superconductivity is limited by the Pauli-paramagnetic effect under a magnetic field. This is because the spin susceptibility decreases in the SC state, and the energy difference between the SC and the normal states reduces under a larger magnetic field due to the magnetic energy in the normal state. The Pauli-limiting field HP is expressed as \"0 HP =T 1⁄4 1:86ð2=gÞTc=K in the weak-coupling BCS model, where g is the g-factor. " +
            "If this is applied to U6Co assuming g 1⁄4 2, \"0HP 1⁄4 4:3 T is obtained. However, \"0Hc2 along the [001] and [110] directions in U6Co in the tetragonal structure is 7.85 and 6.56T, respectively,6) and both of them exceed 4.3T. The Pauli-paramagnetic effect is actually absent in U6Co. " +
            "The large Hc2 value has been considered to originate from the small g-factor.",

            "The flux line lattice (FLL) that is established in the mixed state of a type-II superconductor leads to a distinctive field distribution in the sample. " +
            "The positive muon can be employed as an extremely sensitive probe of local magnetic environments and directly measures the distribution of fields associated with the FLL. " +
            "In this way, μSR is used to calculate the temperature evolution of the magnetic penetration depth λ and thus can determine the presence of nodes in the superconducting order parameter. " +
            "The technique is also sensitive to the very small magnetic moments associated with the formation of spin-triplet electron pairs, and measurements in zero field provide one of the most unambiguous methods of detecting this broken time- reversal symmetry [11]. " +
            "Time-reversal symmetry breaking (TRSB) is an extremely rare phenomenon, which has only been reported for a handful of unconventional supercon- ductors: the candidate chiral p-wave superconductor Sr2 RuO4 [12,13], the heavy fermion superconductors UPt3 and ðU;ThÞBe13 [14–17], the filled skutterudites ðPr; LaÞðRu; OsÞ4Sb12 [18,19], PrPt4Ge12 [20] and centrosymmetric LaNiGa2 [21], and recently the caged- type superconductor Lu5 Rh6 Sn18 [22]. " +
            "μSR studies have been carried out on many other noncentrosymmetric superconductors (NCSs), including CaðIr; PtÞSi3 [23], LaðRh; Pt; Pd; IrÞSi3 [24–26], Mg10Ir19B16 [27], and Re3W [28]. No spontaneous magnetization has been observed in these materials, implying that the supercon- ductivity in these systems occurs predominantly in a spin- singlet channel.",

            "The newly discovered superconductivity in iron oxypnictide superconductors has stimulated intensive research on high- temperature superconductivity outside the cuprate system. In just a few months, the superconducting transition temperature (Tc) was increased to 55 K in the electron-doped system [1–6], as well as 25 K in hole-doped La1−x Srx OFeAs compound [7]. " +
            "Because of the layered structure, the doping behavior and many other properties of the iron-based system are very similar to those of the copper oxides, and it has been thus expected that higher Tc values may be found in multi-layer systems. Soon after, single crystals of LnFeAs(O1−x Fx ) (Ln = Pr, Nd, Sm) were grown successfully by the NaCl/KCl flux method [8–10], though the sub-millimeter sizes limit the experimental studies on them [11, 12]. " +
            "Therefore, FeAs-based single crystals with high crystalline quality, homogeneity and large sizes are highly desired for precise measurements of the properties.\n" +
            "Very recently, the BaFe2As2 compound in a tetragonal ThCr2Si2-type structure with infinite Fe–As layers was reported [13]." +
            "By replacing the alkaline earth elements (Ba and Sr) with alkali elements (Na, K, and Cs), superconductivity\n" +
            "up to 38 K was discovered both in hole-doped and electron-doped samples [14–17]. " +
            "Tc varies from 2.7 K in CsFe2As2 to 38 K in A1−x Kx Fe2 As2 (A = Ba, Sr) [14, 18]. Meanwhile, superconductivity could also be induced in the parent phase by high pressure [19, 20] or by replacing some of the Fe by Co [21, 22]. " +
            "More excitingly, large single crystals could be obtained by the Sn flux method in this family to study the rather low melting temperature and the intermetallic characteristics [23–25]. " +
            "However, single crystals with high homogeneity and low contamination are still hard to obtain by this method [26]. To avoid these problems, the FeAs self-flux method may be more appropriate."]

    }

)(jQuery);



