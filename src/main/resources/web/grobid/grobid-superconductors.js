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
        var annotationsMap = new Array();


        // Transformers to HTML


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
                    success: SubmitSuccesfulText,
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

        function annotateTextAsHtml(inputText, annotationList) {
            var newString = "";
            var lastMaxIndex = inputText.length;
            if (annotationList) {
                var pos = 0; // current position in the text

                for (var annotationIndex = 0; annotationIndex < annotationList.length; annotationIndex++) {
                    var currentAnnotation = annotationList[annotationIndex];
                    if (currentAnnotation) {
                        var startUnit = -1;
                        var endUnit = -1;
                        var start = parseInt(currentAnnotation.offsetStart, 10);
                        var end = parseInt(currentAnnotation.offsetEnd, 10);
                        var type = currentAnnotation.type;
                        if ((startUnit !== -1) && ((startUnit === end) || (startUnit === end + 1)))
                            end = endUnit;
                        if ((endUnit !== -1) && ((endUnit === start) || (endUnit + 1 === start)))
                            start = startUnit;

                        if (start < pos) {
                            // we have a problem in the initial sort of the quantities
                            // the server response is not compatible with the present client
                            console.log("Sorting of quantities as present in the server's response not valid for this client.");
                            // note: this should never happen?
                        } else {
                            newString += inputText.substring(pos, start)
                                + ' <span id="annot_supercon-' + annotationIndex + '" rel="popover" data-color="interval">'
                                + '<span class="label ' + type + ' style="cursor:hand;cursor:pointer;" >'
                                + inputText.substring(start, end) + '</span></span>';
                            pos = end;
                        }
                        // superconMap[currentSuperconIndex] = currentAnnotation;
                        annotationsMap[annotationIndex] = currentAnnotation;
                    }
                }
                newString += inputText.substring(pos, inputText.length);
            }

            return newString;
        }


        function extractOffsetsFromAtomic(quantity) {
            offsetStart = -1;
            offsetEnd = -1;

            if (quantity.rawUnit) {
                if (quantity.offsetStart < quantity.rawUnit.offsetStart) {
                    offsetStart = quantity.offsetStart;
                } else {
                    offsetStart = quantity.rawUnit.offsetStart;
                }

                if (quantity.offsetEnd > quantity.rawUnit.offsetEnd) {
                    offsetEnd = quantity.offsetEnd;
                } else {
                    offsetEnd = quantity.rawUnit.offsetEnd;
                }
            } else {
                offsetStart = quantity.offsetStart;
                offsetStart = quantity.offsetEnd;
            }

            return {'offsetStart': offsetStart, 'offsetEnd': offsetEnd};
        }

        function extractOffsetsFromIntervals(quantityLow, quantityHigh) {
            offsets = {'offsetStart': -1, 'offsetEnd': -1};
            offsetLeast = undefined;
            offsetMost = undefined;

            if (quantityLow) {
                offsetLeast = extractOffsetsFromAtomic(quantityLow);
            }

            if (quantityHigh) {
                offsetMost = extractOffsetsFromAtomic(quantityHigh);
            }

            if (offsetLeast) {
                offsets['offsetStart'] = offsetLeast['offsetStart'];
                if (offsetMost) {
                    offsets['offsetEnd'] = offsetMost['offsetEnd'];
                } else {
                    offsets['offsetEnd'] = offsetLeast['offsetEnd'];
                }
            } else {
                if (offsetMost) {
                    offsets['offsetStart'] = offsetMost['offsetStart'];
                    offsets['offsetEnd'] = offsetMost['offsetEnd'];
                } else {
                    console.log("Something very wrong here.");
                }
            }

            return offsets;
        }

        function adjustTemperatureObjcts(temperatures) {
            for (tmpIdx in temperatures) {
                var temperature = temperatures[tmpIdx];

                var offsets;

                if (temperature.type === 'value') {
                    let quantity = temperature.quantity;
                    offsets = extractOffsetsFromAtomic(quantity);

                } else if (temperature.type === 'interval') {
                    if (temperature.quantityLeast || temperature.quantityMost) {
                        offsets = extractOffsetsFromIntervals(temperature.quantityLeast, temperature.quantityMost);
                    } else if (temperature.quantityRange || temperature.quantityBase) {
                        offsets = extractOffsetsFromIntervals(temperature.quantityBase, temperature.quantityRange);
                    }
                } else if (temperature.type === 'list') {
                    console.log("For now I'm not implementing this. ")
                }

                temperature.offsetStart = offsets['offsetStart'];
                temperature.offsetEnd = offsets['offsetEnd'];
            }
            return temperatures;
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

            var display = '<div class=\"note-tabs\"> \
            <ul id=\"resultTab\" class=\"nav nav-tabs\"> \
                <li class="active"><a href=\"#navbar-fixed-annotation\" data-toggle=\"tab\">Annotations</a></li> \
                <li><a href=\"#navbar-fixed-json\" data-toggle=\"tab\">Response</a></li> \
            </ul> \
            <div class="tab-content"> \
            <div class="tab-pane active" id="navbar-fixed-annotation">\n';

            display += '<pre style="background-color:#FFF;width:95%;" id="displayAnnotatedText">';

            var inputText = $('#inputTextArea').val();

            display += '<table id="sentenceNER" style="width:100%;table-layout:fixed;" class="table">';
            //var string = responseJson.text;

            display += '<tr style="background-color:#FFF;">';
            var superconductors = responseJson.superconductors;

            annotationList = [];

            function addAnnotations(responseAnnotations, type, annotationList) {
                if (responseAnnotations) {
                    for (idx in responseAnnotations) {
                        annotation = {
                            'obj': responseAnnotations[idx],
                            'type': type,
                            'offsetStart': responseAnnotations[idx].offsetStart,
                            'offsetEnd': responseAnnotations[idx].offsetEnd
                        };
                        annotationList.push(annotation);
                    }
                }
                return annotationList
            }

            // Custom for measurements
            addAnnotations(responseJson.superconductors, 'superconductor', annotationList);
            var temperaturesList = adjustTemperatureObjcts(responseJson.temperatures);

            addAnnotations(temperaturesList, 'measurement', annotationList);
            addAnnotations(responseJson.abbreviations, 'abbreviation', annotationList);

            annotationList = annotationList.sort(function (a, b) {
                if (a.offsetStart > b.offsetStart) return 1;
                else if (a.offsetStart < b.offsetStart) return -1;
                else return 0;
            });

            var annotatedTextAsHtml = annotateTextAsHtml(inputText, annotationList);

            annotatedTextAsHtml = "<p>" + annotatedTextAsHtml.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "</p>";
            //string = string.replace("<p></p>", "");

            display += '<td style="font-size:small;width:60%;border:1px solid #CCC;"><p>' + annotatedTextAsHtml + '</p></td>';
            display += '<td style="font-size:small;width:40%;padding:0 5px; border:0"><span id="detailed_annot-0-0" /></td>';

            display += '</tr>';
            display += '</table>\n';
            display += '</pre>\n';
            display += '</div> \
                    <div class="tab-pane " id="navbar-fixed-json">\n';


            //JSON Pretty print box
            display += "<pre class='prettyprint' id='jsonCode'>";

            display += "<pre class='prettyprint lang-json' id='xmlCode'>";
            var testStr = vkbeautify.json(responseText);

            display += htmll(testStr);

            display += "</pre>";
            display += '</div></div></div>';

            $('#requestResult').html(display);
            window.prettyPrint && prettyPrint();

            // Adding events
            if (annotationList) {
                for (var annotationIdx = 0; annotationIdx < annotationList.length; annotationIdx++) {
                    // var measurement = measurements[measurementIndex];

                    $('#annot_supercon-' + annotationIdx).bind('hover', annotationList, viewAnnotation);
                    $('#annot_supercon-' + annotationIdx).bind('click', annotationList, viewAnnotation);
                }
            }

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

                    if (measurementType === "value") {
                        var quantity = measurement.quantity;
                        if (quantity)
                            quantities.push(quantity)
                    } else if (measurementType === "interval") {
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

            $('#annot_quantity-' + measurementIndex + '-' + positionIndex).bind('hover', {
                'type': 'quantity',
                'map': measurementMap
            }, viewEntityPDF);
            $('#annot_quantity-' + measurementIndex + '-' + positionIndex).bind('click', {
                'type': 'quantity',
                'map': measurementMap
            }, viewEntityPDF);
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

            $('#annot_abbreviation-' + superconIdx + '-' + positionIdx).bind('hover', {
                'type': 'abbreviation',
                'map': abbreviationsMap
            }, viewEntityPDF);
            $('#annot_abbreviation-' + superconIdx + '-' + positionIdx).bind('click', {
                'type': 'abbreviation',
                'map': abbreviationsMap
            }, viewEntityPDF);
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

            $('#annot_supercon-' + superconIdx + '-' + positionIdx).bind('hover', {
                'type': 'superconductor',
                'map': superconMap
            }, viewEntityPDF);
            $('#annot_supercon-' + superconIdx + '-' + positionIdx).bind('click', {
                'type': 'superconductor',
                'map': superconMap
            }, viewEntityPDF);
        }


        function viewAnnotation(annotationsList) {

            if (annotationsList.length === 0) {
                return;
            }
            var localID = $(this).attr('id');

            var ind1 = localID.indexOf('-');
            var localAnnotationID = parseInt(localID.substring(ind1 + 1));
            if ((annotationsMap[localAnnotationID] == null) || (annotationsMap[localAnnotationID].length === 0)) {
                // this should never be the case
                console.log("Error for visualising annotation measurement with id " + localAnnotationID
                    + ", empty list of measurement");
            }

            var annotation = annotationsMap[localAnnotationID];
            var string = "";
            if (annotation.type === 'superconductor') {
                string = toHtmlSemiconductor(annotation.obj, -1);
            } else if (annotation.type === 'measurement') {
                string = toHtmlMeasurement(annotation.obj, -1)
            } else if (annotation.type === 'abbreviation') {
                string = toHtmlAbbreviation(annotation.obj, -1)
            }

            $('#detailed_annot-0-0').html(string);
            $('#detailed_annot-0-0').show();
        }

        function viewEntityPDF(param) {
            var type = param.data.type;
            var map = param.data.map;

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
            var string = "";
            if (type === 'superconductor') {
                string = toHtmlSemiconductor(map[localMeasurementNumber], $(this).position().top);
            } else if (type === 'abbreviation') {
                string = toHtmlAbbreviation(map[localMeasurementNumber], $(this).position().top);
            } else if (type === 'quantity') {
                var quantityMap = map[localMeasurementNumber];
                var measurementType = null;

                if (map.length == 1) {
                    measurementType = "Atomic value";
                    string = toHtml(quantityMap, measurementType, $(this).position().top);
                } else if (quantityMap.length == 2) {
                    measurementType = "Interval";
                    string = intervalToHtml(quantityMap, measurementType, $(this).position().top);
                } else {
                    measurementType = "List";
                    string = toHtml(quantityMap, measurementType, $(this).position().top);
                }
            }
            if (type === null || string === "") {
                console.log("Error in viewing annotation, type unknown");
            }

            $('#detailed_annot-' + pageIndex).html(string).show();
        }


        // Transformation to HTML
        function toHtmlSemiconductor(superconductor, topPos) {
            var string = "";
            var first = true;

            colorLabel = 'superconductor';
            var name = superconductor.name;

            string += "<div class='info-sense-box " + colorLabel + "'";
            if (topPos !== -1)
                string += " style='vertical-align:top; position:relative; top:" + topPos + "'";

            string += ">";
            string += "<h2 style='color:#FFF;padding-left:10px;font-size:16pt;'>Material</h2>";

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

        function toHtmlAbbreviation(abbreviation, topPos) {
            var string = "";
            var first = true;

            colorLabel = 'abbreviation';
            var name = abbreviation.name;

            string += "<div class='info-sense-box " + colorLabel + "'";
            if (topPos != -1)
                string += " style='vertical-align:top; position:relative; top:" + topPos + "'";

            string += ">";
            string += "<h2 style='color:#FFF;padding-left:10px;font-size:16pt;'>Abbreviation</h2>";

            string += "<div class='container-fluid' style='background-color:#FFF;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                "<table style='width:100%;display:inline-table;'><tr style='display:inline-table;'><td>";

            if (name) {
                string += "<p>name: <b>" + name + "</b></p>";
            }

            string += "</td></tr>";
            string += "</table></div>";

            string += "</div>";

            return string;
        }

        function toHtmlMeasurement(measurement, topPos) {
            var string = "";
            var first = true;

            colorLabel = 'measurement';

            string += "<div class='info-sense-box " + colorLabel + "'";
            if (topPos != -1)
                string += " style='vertical-align:top; position:relative; top:" + topPos + "'";

            string += ">";
            string += "<h2 style='color:#FFF;padding-left:10px;font-size:16pt;'>Measurement</h2>";

            string += "<div class='container-fluid' style='background-color:#FFF;color:#70695C;border:padding:5px;margin-top:5px;'>" +
                "<table style='width:100%;display:inline-table;'><tr style='display:inline-table;'><td>";

            quantityMap = [];
            if (measurement.type === 'value') {
                measurementType = "Atomic value";
                measurement.quantity['quantified'] = measurement.quantified;
                quantityMap.push(measurement.quantity);
                string = toHtml(quantityMap, measurementType, -1);
            } else if (measurement.type === 'interval') {
                measurementType = "Interval";
                if (measurement.quantityLeast) {
                    measurement.quantityLeast['quantified'] = measurement.quantified;
                    quantityMap.push(measurement.quantityLeast)
                }

                if (measurement.quantityBase) {
                    measurement.quantityBase['quantified'] = measurement.quantified;
                    quantityMap.push(measurement.quantityBase)
                }

                if (measurement.quantityMost) {
                    measurement.quantityMost['quantified'] = measurement.quantified;
                    quantityMap.push(measurement.quantityMost)
                }

                if (measurement.quantityRange) {
                    measurement.quantityMost['quantified'] = measurement.quantified;
                    quantityMap.push(measurement.quantityRange)
                }

                if (quantityMap.length > 1)
                    string = intervalToHtml(quantityMap, measurementType, -1);
                else
                    string = toHtml(quantityMap, measurementType, -1);
            } else {
                measurementType = "List";
                quantityMap.push(measurement.list);
                string = toHtml(quantityMap, measurementType, -1);
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
            "Uranium compounds U6X (X 1⁄4 Mn, Fe, Co, and Ni) are superconductors with relatively high superconducting (SC) transition temperatures Tc among the uranium-based compounds. Contrary to other uranium-based heavy-fermion systems including ferromagnetic superconductors, 5f electrons in U6X compounds exhibit an itinerant nature even at room temperature, and do not exhibit magnetic ground states. The SC properties are consistent with the conventional one: for instance, the full-gap superconductivity of U6Co (Tc 1⁄4 2:33 K) has been indicated by the penetration depth, nuclear spin–lattice relaxation rate 1=T1, and more recently, by specific heat measurements. Thus, at present, these compounds are good reference systems for the uranium-based unconventional superconductors. ",
            "We report X-ray diffraction, magnetization and transport measurements for polycrystalline samples of the new layered superconductor Bi4−xAgxO4S3 (0 < x < 0.2). The superconducting transition temperature (TC) decreases gradually and finally suppressed when x < 0.10. Accordingly, the resistivity changes from a metallic behavior for x < 0.1 to a semiconductor-like behavior for x > 0.1. The analysis of Seebeck coefficient shows there are two types of electron-like carriers dominate at different temperature regions, indicative of a multiband effect responsible for the transport properties. The suppression of superconductivity and the increased resistivity can be attributed to a shift of the Fermi level to the lower-energy side upon doping, which reduces the density of states at EF. Further, our result indicates the superconductivity in Bi4O4S3 is intrinsic and the dopant Ag prefers to enter the BiS2 layers, which may essentially modify the electronic structure.",
            "The flux line lattice (FLL) that is established in the mixed state of a type-II superconductor leads to a distinctive field distribution in the sample. The positive muon can be employed as an extremely sensitive probe of local magnetic environments and directly measures the distribution of fields associated with the FLL. Time-reversal symmetry breaking (TRSB) is an extremely rare phenomenon, which has only been reported for a handful of unconventional superconductors: the candidate chiral p-wave superconductor Sr2RuO4, the heavy fermion superconductors UPt3 and (U;Th)Be13, the filled skutterudites (Pr; La) (Ru; Os) 4Sb12, PrPt4Ge12 and centrosymmetric LaNiGa2, and recently the caged-type superconductor Lu5Rh6Sn18. μSR studies have been carried out on many other noncentrosymmetric superconductors (NCSs), including Ca(Ir; Pt)Si3, La(Rh; Pt; Pd; Ir)Si3, Mg10Ir19B16, and Re3W. No spontaneous magnetization has been observed in these materials, implying that the superconductivity in these systems occurs predominantly in a spinsinglet channel.",
            "In just a few months, the superconducting transition temperature (Tc) was increased to 55 K in the electron-doped system, as well as 25 K in hole-doped La1−x SrxOFeAs compound. Soon after, single crystals of LnFeAs(O1−x Fx) (Ln = Pr, Nd, Sm) were grown successfully by the NaCl/KCl flux method, though the sub-millimeter sizes limit the experimental studies on them. Therefore, FeAs-based single crystals with high crystalline quality, homogeneity and large sizes are highly desired for precise measurements of the properties. Very recently, the BaFe2As2 compound in a tetragonal ThCr2Si2-type structure with infinite Fe–As layers was reported. By replacing the alkaline earth elements (Ba and Sr) with alkali elements (Na, K, and Cs), superconductivity up to 38 K was discovered both in hole-doped and electron-doped samples. Tc varies from 2.7 K in CsFe2As2 to 38 K in A1−xKxFe2As2 (A = Ba, Sr). Meanwhile, superconductivity could also be induced in the parent phase by high pressure or by replacing some of the Fe by Co. More excitingly, large single crystals could be obtained by the Sn flux method in this family to study the rather low melting temperature and the intermetallic characteristics."]

    }

)(jQuery);



