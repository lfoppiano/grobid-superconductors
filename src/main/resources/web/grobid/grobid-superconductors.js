/**
 *  Javascript functions for the front end.
 *
 *  Author: Patrice Lopez
 */

let grobid = (function ($) {

        // for components view
        let responseJson = null;

        // for associating several quantities to a measurement
        let spansMap = [];
        let configuration = {};

        function getUrl(action) {
            let lastIndexOfSlash = $(location).attr('href').lastIndexOf("/");
            let baseUrl = $(location).attr('href').substring(0, lastIndexOfSlash);

            if (configuration['url_mapping'][action] !== null) {
                return baseUrl + configuration['url_mapping'][action]
            } else {
                onError("Action " + action + " was not found in configuration. ");
            }
        }

        function loadTextExamples(examples_list) {
            for (let idx_example in examples_list) {
                $('#example' + idx_example).unbind('click')
                $('#example' + idx_example).bind('click', {id: idx_example}, function (event) {
                    event.preventDefault();
                    $('#inputTextArea').val(examples_list[event.data.id]);
                });
            }
        }

        function loadMaterialsExamples(examples_list) {
            for (let idx_example in examples_list) {
                $('#exampleMaterial' + idx_example).unbind('click')
                $('#exampleMaterial' + idx_example).bind('click', {id: idx_example}, function (event) {
                    event.preventDefault();
                    $('#inputMaterialArea').val(examples_list[event.data.id]);
                });
            }
        }

        function copyTextToClipboard(text) {
            let textArea = document.createElement("textarea");

            //
            // *** This styling is an extra step which is likely not required. ***
            //
            // Why is it here? To ensure:
            // 1. the element is able to have focus and selection.
            // 2. if element was to flash render it has minimal visual impact.
            // 3. less flakyness with selection and copying which **might** occur if
            //    the textarea element is not visible.
            //
            // The likelihood is the element won't even render, not even a
            // flash, so some of these are just precautions. However in
            // Internet Explorer the element is visible whilst the popup
            // box asking the user for permission for the web page to
            // copy to the clipboard.
            //

            // Place in top-left corner of screen regardless of scroll position.
            textArea.style.position = 'fixed';
            textArea.style.top = 0;
            textArea.style.left = 0;

            // Ensure it has a small width and height. Setting to 1px / 1em
            // doesn't work as this gives a negative w/h on some browsers.
            textArea.style.width = '2em';
            textArea.style.height = '2em';

            // We don't need padding, reducing the size if it does flash render.
            textArea.style.padding = 0;

            // Clean up any borders.
            textArea.style.border = 'none';
            textArea.style.outline = 'none';
            textArea.style.boxShadow = 'none';

            // Avoid flash of white box if rendered for any reason.
            textArea.style.background = 'transparent';
            textArea.value = text;

            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();

            try {
                let successful = document.execCommand('copy');
                let msg = successful ? 'successful' : 'unsuccessful';
                console.log('Copying text command was ' + msg);
            } catch (err) {
                console.log('Oops, unable to copy');
            }

            document.body.removeChild(textArea);
        }

        function copyOnClipboard() {
            console.log("Copying data on clipboard! ");
            let tableResultsBody = $('#tableResultsBody');

            let textToBeCopied = "";

            let rows = tableResultsBody.find("tr");
            $.each(rows, function () {
                let tds = $(this).children();
                let material = tds[2].textContent;
                let tc = tds[3].textContent;

                textToBeCopied += material + "\t" + tc + "\n";
            });
            copyTextToClipboard(textToBeCopied);

        }

        /** Download buttons **/
        function downloadRDF() {
            let fileName = "export.xml";
            let a = document.createElement("a");
            let xml_header = '<?xml version="1.0"?>';
            let rdf_header = '<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:supercon="http://falcon.nims.go.jp/supercuration">';
            let rdf_header_end = '</rdf:RDF>';

            let outputXML = xml_header + "\n" + rdf_header + "\n";

            let tableResultsBody = $('#tableResultsBody');

            let rows = tableResultsBody.find("tr");
            $.each(rows, function () {
                let tds = $(this).children();
                let material = tds[2].textContent;
                let tcValue = tds[3].textContent;
                let id = $(this).attr("id").replaceAll("row", "");

                outputXML += "\t" + '<rdf:Description rdf:about="http://falcon.nims.go.jp/supercon/' + id + '">';

                outputXML += "\t\t" + '<supercon:material>' + material + '</supercon:material>' + "\n";
                outputXML += "\t\t" + '<supercon:tcValue>' + tcValue + '</supercon:tcValue>' + "\n";

                outputXML += "\t" + '</rdf:Description>';
            });

            outputXML += rdf_header_end;

            let file = new Blob([outputXML], {type: 'application/xml'});
            a.href = URL.createObjectURL(file);
            a.download = fileName;

            document.body.appendChild(a);

            $(a).ready(function () {
                a.click();
                return true;
            });
        }

        function downloadCSV() {
            let fileName = "export.csv";
            let a = document.createElement("a");
            let header = 'material, tcValue';
            let outputCSV = header + "\n";

            let tableResultsBody = $('#tableResultsBody');

            let rows = tableResultsBody.find("tr");
            $.each(rows, function () {
                let tds = $(this).children();
                let material = tds[2].textContent;
                let tcValue = tds[3].textContent;
                // let id = $(this).attr("id").replaceAll("row", "");

                outputCSV += material + ',' + tcValue + "\n";
            });

            let file = new Blob([outputCSV], {type: 'text/csv'});
            a.href = URL.createObjectURL(file);
            a.download = fileName;

            document.body.appendChild(a);

            $(a).ready(function () {
                a.click();
                return true;
            });
        }

        $(document).ready(function () {
            $('#requestResultPdf').hide();
            $('#requestResultText').hide();
            $('#requestResultMaterial').hide();
            // $('#tableResults').hide();

            configuration = {
                "url_mapping": {
                    "processPDF": "/service/process/pdf",
                    "processText": "/service/process/text",
                    "processMaterial": "/service/material/parser",
                    "feedback": "/service/annotations/feedback",
                    "processMaterial": "/service/material/parse"
                }
            }
            $('#submitRequestText').bind('click', 'processText', processText);
            $('#submitRequestMaterial').bind('click', 'processMaterial', processMaterial);
            $('#submitRequestPdf').bind('click', 'processPDF', processPdf);
            $('#copy-button').bind('click', copyOnClipboard);
            $('#add-button').bind('click', addRow);
            $('#download-rdf-button').bind('click', downloadRDF);
            $('#download-csv-button').bind('click', downloadCSV);

            //this mess avoid that pressing the tabs down in the text we reset the wrong div
            $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
                if (e.target) {
                    if (e.target.parentElement) {
                        if (e.target.parentElement.parentElement) {
                            if (e.target.parentElement.parentElement.id === "top-tab") {
                                $('#requestResultPdf').hide();
                                $('#requestResultText').hide();
                                $('#requestResultMaterial').hide();
                            }
                        }
                    }
                }

            });

            // Collapse icon
            $('a[data-toggle="collapse"]').click(function () {
                let currentIcon = $(this).find('img').attr("src")
                let newIcon = currentIcon === 'resources/icons/chevron-right.svg' ? 'resources/icons/chevron-down.svg' : 'resources/icons/chevron-right.svg';
                $(this).find('img').attr("src", newIcon);
            })

            $('#file-upload').on('change', function () {
                //get the file name
                let fileName = $(this).val();
                //replace the "Choose a file" label
                $(this).next('.custom-file-label').html(fileName);
            })

            loadTextExamples(examples_superconductors);
            loadMaterialsExamples(examples_materials);

            //turn to inline mode
            $.fn.editable.defaults.mode = 'inline';
        });

        function onError(message) {
            if (!message)
                message = "The Text or the PDF document cannot be processed. Please check the server logs.";

            $('#infoResultMessage').html("<p class='text-danger'>Error encountered while requesting the server.<br/>" + message + "</p>");
            return true;
        }

        function cleanupHtml(s) {
            return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }

        /* jquery-based movement to an anchor, without modifying the displayed url and a bit smoother */
        function goToByScroll(id) {
            console.log("Selecting id " + id.data);
            $('html,body').animate({scrollTop: $("#" + id.data).offset().top - 100}, 'slow');
        }

        function scrollUp() {
            console.log("Scrolling back up");
            $('html,body').animate({scrollTop: 0}, 'slow');
        }

        function processText(action) {
            $('#infoResultMessage').html('<p class="text-secondary">Requesting server...</p>');
            let formData = new FormData();
            formData.append("text", $('#inputTextArea').val());

            let disableLinking = $('#check-disable-linking-text')[0].checked;
            if (disableLinking === true) {
                formData.append("disableLinking", disableLinking);
            }

            $.ajax({
                type: 'POST',
                url: getUrl(action.data),
                data: formData,
                success: onSuccessText,
                error: onError,
                contentType: false,
                processData: false
            });
        }

        function processMaterial(action) {
            $('#infoResultMessage').html('<p class="text-secondary">Requesting server...</p>');
            let formData = new FormData();
            formData.append("text", $('#inputMaterialArea').val());

            $.ajax({
                type: 'POST',
                url: getUrl(action.data),
                data: formData,
                success: onSuccessMaterial,
                error: onError,
                contentType: false,
                processData: false
            });
        }

        function onSuccessMaterial(materials, status) {
            $('#infoResultMessage').html('');

            let cumulativeOutput = "";
            if (materials) {
                materials.forEach(function (material, materialIdx) {
                    let annotationStartReplaced = material.rawTaggedValue.replace(/<[^\/<> ]+>/gm, function (matching) {
                        let label = matching.replace("<", "")
                            .replace(">", "");

                        return '<span class="label material ' + label + '">';
                    });

                    let annotationEndReplaced = annotationStartReplaced.replace(/<\/[^\/<> ]+>/gm, function (matching) {
                        return '</span>';
                    })

                    cumulativeOutput += "<p>";
                    for (let prop in material) {
                        if (prop === 'rawTaggedValue') {
                        } else if (prop === 'resolvedFormulas') {
                            let resolvedFormulas = material[prop].join(", ");
                            cumulativeOutput += "<strong>" + prop + "</strong>: " + resolvedFormulas + " <br>";
                        } else {
                            cumulativeOutput += "<strong>" + prop + "</strong>: " + material[prop] + " <br>";
                        }
                    }

                    cumulativeOutput += "<strong>Original Tags</strong>: " + annotationEndReplaced.replace(/(\r\n|\n|\r)/gm, "</p><p>") + "<br>";
                    cumulativeOutput += "</p>"

                });
            }

            $('#requestResultMaterialContent').html(cumulativeOutput);

            let testStr = vkbeautify.json(materials);

            $('#jsonCodeMaterial').html(cleanupHtml(testStr));
            window.prettyPrint && prettyPrint();

            $('#detailed_annot-0-0').hide();
            $('#requestResultPdf').hide();
            $('#requestResultText').hide();
            $('#requestResultMaterial').show();
        }

        function showSpanOnText_event(event_data) {
            let span = event_data.data;
            // console.log(span.id);

            var string = spanToHtml(span, -1);

            $('#detailed_annot-0-0').html(string);
            $('#detailed_annot-0-0').show();
        }

        function onSuccessText(responseText, statusText) {
            $('#infoResultMessage').html('');

            let paragraphs = responseText.paragraphs;
            let cumulativeOutput = "";
            if (paragraphs) {
                paragraphs.forEach(function (paragraph, paragraphIdx) {
                    let text = paragraph.text;
                    let spans = [];
                    if (paragraph.spans) {
                        spans = paragraph.spans;
                    }
                    //TODO: find a better solution
                    // spans.forEach(function (span, spanIdx) {
                    //     spansMap[span.id] = span;
                    // });
                    //
                    // spans.forEach(function (span, spanIdx) {
                    //
                    //     if (span.links !== undefined && span.links.length > 0) {
                    //         span.links.forEach(function (link, linkIdx) {
                    //             let link_entity = spansMap[link.targetId];
                    //             link['targetText'] = link_entity.text;
                    //         });
                    //     }
                    // });

                    cumulativeOutput += annotateTextAsHtml(text, spans);
                })
            }

            $('#requestResultTextContent').html(cumulativeOutput);

            //Adding events, unfortunately I need to wait when the HTML tree is updated
            if (paragraphs) {
                paragraphs.forEach(function (paragraph, paragraphIdx) {
                    let spans = [];
                    if (paragraph.spans) {
                        spans = paragraph.spans;
                    }

                    // Adding events
                    for (let annotationIdx = 0; annotationIdx < spans.length; annotationIdx++) {
                        let annotationBlock = $('#annot_supercon-' + spans[annotationIdx].id);
                        annotationBlock.bind('hover', spans[annotationIdx], showSpanOnText_event);
                        annotationBlock.bind('click', spans[annotationIdx], showSpanOnText_event);
                    }
                });
            }

            let testStr = vkbeautify.json(responseText);

            $('#jsonCode').html(cleanupHtml(testStr));
            window.prettyPrint && prettyPrint();

            $('#detailed_annot-0-0').hide();
            $('#requestResultPdf').hide();
            $('#requestResultMaterial').hide();
            $('#requestResultText').show();
        }

        function processPdf(action) {
            spansMap = [];

            let resultMessageBlock = $('#infoResultMessage');
            resultMessageBlock.html('<p class="text-secondary">Requesting server...</p>');
            let requestResult = $('#requestResultPdfContent');
            requestResult.html('');
            requestResult.show();

            $('#tableResultsBody').html('');


            // we will have JSON annotations to be layered on the PDF
            let nbPages = -1;

            // display the local PDF
            let inputElement = document.getElementById("file-upload");

            if (inputElement.files.length === 0
                || inputElement.files[0] === undefined
                || inputElement.files[0].type !== 'application/pdf'
                || inputElement.files[0].name === undefined
                || !inputElement.files[0].name.toLowerCase().endsWith(".pdf")) {

                onError("No file or wrong file type selected. Please select a PDF file before pressing 'Submit'");
                //No file was selected in the form
                return
            }

            let reader = new FileReader();
            reader.onloadend = function () {
                // to avoid cross origin issue
                //PDFJS.disableWorker = true;
                let pdfAsArray = new Uint8Array(reader.result);
                // Use PDFJS to render a pdfDocument from pdf array
                PDFJS.getDocument(pdfAsArray).then(function (pdf) {
                    // Get div#container and cache it for later use
                    let container = document.getElementById("requestResultPdfContent");
                    // enable hyperlinks within PDF files.
                    //let pdfLinkService = new PDFJS.PDFLinkService();
                    //pdfLinkService.setDocument(pdf, null);

                    //$('#requestResult').html('');
                    nbPages = pdf.numPages;

                    // Loop from 1 to total_number_of_pages in PDF document
                    for (let i = 1; i <= nbPages; i++) {

                        // Get desired page
                        pdf.getPage(i).then(function (page) {
                            let table = document.createElement("table");
                            let tr = document.createElement("tr");
                            let td1 = document.createElement("td");
                            let td2 = document.createElement("td");

                            tr.appendChild(td1);
                            tr.appendChild(td2);
                            table.appendChild(tr);

                            let div0 = document.createElement("div");
                            div0.setAttribute("style", "text-align: center; margin-top: 1cm; width:80%;");
                            let pageInfo = document.createElement("p");
                            let t = document.createTextNode("page " + (page.pageIndex + 1) + "/" + (nbPages));
                            pageInfo.appendChild(t);
                            div0.appendChild(pageInfo);

                            td1.appendChild(div0);

                            let scale = 1.5;
                            let viewport = page.getViewport(scale);
                            let div = document.createElement("div");

                            // Set id attribute with page-#{pdf_page_number} format
                            div.setAttribute("id", "page-" + (page.pageIndex + 1));

                            // This will keep positions of child elements as per our needs, and add a light border
                            div.setAttribute("style", "position: relative; ");

                            // Create a new Canvas element
                            let canvas = document.createElement("canvas");
                            canvas.setAttribute("style", "border-style: solid; border-width: 1px; border-color: gray;");

                            // Append Canvas within div#page-#{pdf_page_number}
                            div.appendChild(canvas);

                            // Append div within div#container
                            td1.appendChild(div);

                            let annot = document.createElement("div");
                            annot.setAttribute('style', 'vertical-align:top;');
                            annot.setAttribute('id', 'detailed_annot-' + (page.pageIndex + 1));
                            td2.setAttribute('style', 'vertical-align:top;');
                            td2.appendChild(annot);

                            container.appendChild(table);

                            let context = canvas.getContext('2d');
                            canvas.height = viewport.height;
                            canvas.width = viewport.width;

                            let renderContext = {
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
                                    let textLayerDiv = document.createElement("div");

                                    // Set it's class to textLayer which have required CSS styles
                                    textLayerDiv.setAttribute("class", "textLayer");

                                    // Append newly created div in `div#page-#{pdf_page_number}`
                                    div.appendChild(textLayerDiv);

                                    // Create new instance of TextLayerBuilder class
                                    let textLayer = new TextLayerBuilder({
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
            reader.readAsArrayBuffer(inputElement.files[0]);

            // request for the annotation information
            let form = document.getElementById('gbdForm');
            let formData = new FormData(form);
            let xhr = new XMLHttpRequest();
            let url = getUrl(action.data);
            $('#gbdForm').attr('action', url);
            xhr.responseType = 'json';
            xhr.open('POST', url, true);

            xhr.onreadystatechange = function (e) {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    let response = e.target.response;
                    onSuccessPdf(response);
                } else if (xhr.status !== 200) {
                    onError("Response: " + xhr.status);
                }
            };
            xhr.send(formData);
        }

        function onSuccessPdf(response) {
            // TBD: we must check/wait that the corresponding PDF page is rendered at this point
            if ((response == null) || (0 === response.length)) {
                onError("The response is empty.")
                return;
            } else {
                $('#infoResultMessage').html('');
                $('#requestResultPdf').show()
            }

            let json = response;
            let pages = json['pages'];
            let paragraphs = json.paragraphs;

            let spanGlobalIndex = 0;
            let copyButtonElement = $('#copy-button');

            paragraphs.forEach(function (paragraph, paragraphIdx) {
                let spans = paragraph.spans;
                // hey bro, this must be asynchronous to avoid blocking the brothers

                if (spans) {
                    spans.forEach(function (span, spanIdx) {
                        let annotationId = span.id;
                        spansMap[annotationId] = span;
                        let entity_type = getPlainType(span['type']);

                        let boundingBoxes = span.boundingBoxes;
                        if ((boundingBoxes != null) && (boundingBoxes.length > 0)) {
                            boundingBoxes.forEach(function (boundingBox, boundingBoxId) {
                                let pageNumber = boundingBox.page;
                                let pageInfo = pages[pageNumber - 1];

                                entity_type = transformToLinkableType(entity_type, span.links);
                                annotateSpanOnPdf(annotationId, boundingBoxId, boundingBox, entity_type, pageInfo);

                                $('#' + (annotationId + '' + boundingBoxId)).bind('click', {
                                    'type': 'entity',
                                    'item': span
                                }, showSpanOnPDF);
                            });
                        }
                        spanGlobalIndex++;
                    });


                    spans.filter(function(span){
                        return getPlainType(span.type) === "material";
                    }).forEach(function (span, spanIdx) {
                        if (span.links !== undefined && span.links.length > 0) {
                            copyButtonElement.show();
                            span.links.forEach(function (link, linkIdx) {
                                let link_entity = spansMap[link.targetId];

                                // span.text == material
                                // link.targetText == tcValue
                                let {row_id, element_id, mat_element_id, tc_element_id, html_code} =
                                    createRowHtml(span.id, span.text, link.targetText, link.type,true);

                                $('#tableResultsBody').append(html_code);

                                // in case of multiple bounding boxes, we will have multiple IDs, in this case we can point
                                // to the first box
                                $("#" + element_id).bind('click', span.id + '0', goToByScroll);
                                $("#" + mat_element_id).editable();
                                $("#" + tc_element_id).editable();
                                appendRemoveButton(row_id);

                                let paragraph_popover = annotateTextAsHtml(paragraph.text, [span, link_entity]);

                                $("#" + row_id).popover({
                                    content: function () {
                                        return paragraph_popover;
                                    },
                                    html: true,
                                    // container: 'body',
                                    trigger: 'hover',
                                    placement: 'top',
                                    animation: true
                                });
                            });
                        }
                    });
                }
            });
        }

        function annotateSpanOnPdf(annotationId, boundingBoxId, boundingBox, type, pageInfo) {
            let page = boundingBox.page;
            let pageDiv = $('#page-' + page);
            let canvas = pageDiv.children('canvas').eq(0);

            // get page information for the annotation
            let page_height = 0.0;
            let page_width = 0.0;
            if (pageInfo) {
                page_height = pageInfo.page_height;
                page_width = pageInfo.page_width;
            }

            let canvasHeight = canvas.height();
            let canvasWidth = canvas.width();
            let scale_x = canvasHeight / page_height;
            let scale_y = canvasWidth / page_width;

            let x = boundingBox.x * scale_x - 1;
            let y = boundingBox.y * scale_y - 1;
            let width = boundingBox.width * scale_x + 1;
            let height = boundingBox.height * scale_y + 1;

            //make clickable the area
            let element = document.createElement("a");
            let attributes = "display:block; width:" + width + "px; height:" + height + "px; position:absolute; top:" +
                y + "px; left:" + x + "px;";
            element.setAttribute("style", attributes + "border:2px solid; box-sizing: content-box;");
            element.setAttribute("class", 'area' + ' ' + type);
            element.setAttribute("id", (annotationId + '' + boundingBoxId));
            element.setAttribute("page", page);

            pageDiv.append(element);
        }

        /** Summary table **/
        function createRowHtml(id, material = "", tcValue = "", type = "", viewInPDF = false) {

            let viewInPDFIcon = "";
            if (viewInPDF === true) {
                viewInPDFIcon = "<img src='resources/icons/arrow-down.svg' alt='View in PDF' title='View in PDF'></a>";
            }

            let row_id = "row" + id + type;
            let element_id = "e" + id + type;
            let mat_element_id = "mat" + id + type;
            let tc_element_id = "tc" + id + type;

            let html_code = "<tr class='d-flex' id=" + row_id + " style='cursor:hand;cursor:pointer;' >" +
                "<td class='col-1'><a href='#' id=" + element_id + ">" + viewInPDFIcon + "</td>" +
                "<td class='col-1'><img src='resources/icons/trash.svg' alt='-' id='remove-button'/></td>" +
                "<td class='col-5'><a href='#' id=" + mat_element_id + " data-pk='" + mat_element_id + "' data-url='" + getUrl('feedback') + "' data-type='text'>" + material + "</a></td>" +
                "<td class='col-2'><a href='#' id=" + tc_element_id + " data-pk='" + tc_element_id + "' data-url='" + getUrl('feedback') + "' data-type='text'>" + tcValue + "</a></td>" +
                "<td class='col-1'>" + type + "</td>" +
                "</tr>";

            return {row_id, element_id, mat_element_id, tc_element_id, html_code};
        }

        function appendRemoveButton(row_id) {
            let remove_button = $("#" + row_id).find("img#remove-button");
            remove_button.bind("click", function () {
                // console.log("Removing row with id " + row_id);
                let item = $("#" + row_id);
                // Remove eventual popups
                $("#" + item.attr("aria-describedby")).html("").hide();
                item.remove();
            });
        }

        function addRow() {
            console.log("Adding new row. ");

            let random_number = '_' + Math.random().toString(36).substr(2, 9);

            let {row_id, element_id, mat_element_id, tc_element_id, html_code} = createRowHtml(random_number);
            $('#tableResultsBody').append(html_code);

            $("#" + mat_element_id).editable();
            $("#" + tc_element_id).editable();

            appendRemoveButton(row_id);
        }

        /** Visualisation **/

        function showSpanOnPDF(param) {
            let type = param.data.type;
            let span = param.data.item;

            let pageIndex = $(this).attr('page');
            let string = spanToHtml(span, $(this).position().top);

            if (type === null || string === "") {
                console.log("Error in viewing annotation, type unknown or null: " + type);
            }

            let annotationHook = $('#detailed_annot-' + pageIndex);
            //Reset the click event before adding a new one - not so clean, but would do for the moment...
            annotationHook.off('click');
            annotationHook.html(string).show();
            annotationHook.on('click', scrollUp);
        }

        function transformToLinkableType(type, links) {
            if (links === undefined || links.length === 0) {
                return type;
            }

            if (type === "material") {
                links.forEach(function (link, linkIdx) {
                    if (getPlainType(link['targetType']) === 'tcValue') {
                        type = 'material-tc';
                    }
                });

            } else if (type === "tcValue") {
                links.forEach(function (link, linkIdx) {
                    if (getPlainType(link['targetType']) === 'material') {
                        type = 'temperature-tc';
                    } else if (getPlainType(link['targetType']) === 'pressure') {
                        type = 'temperature-pressure';
                    } else if (getPlainType(link['targetType']) === 'me_method') {
                        type = 'temperature-me_method';
                    }
                });
            }

            return type;
        }

        function annotateTextAsHtml(inputText, annotationList) {
            let outputString = "";
            let pos = 0;

            annotationList.sort(function (a, b) {
                let startA = parseInt(a.offsetStart, 10);
                let startB = parseInt(b.offsetStart, 10);

                return startA - startB;
            });

            annotationList.forEach(function (annotation, annotationIdx) {
                let start = parseInt(annotation.offsetStart, 10);
                let end = parseInt(annotation.offsetEnd, 10);

                let type = getPlainType(annotation.type);
                let links = annotation.links
                type = transformToLinkableType(type, links)
                let id = annotation.id;

                outputString += inputText.substring(pos, start)
                    + ' <span id="annot_supercon-' + id + '" rel="popover" data-color="interval">'
                    + '<span class="label ' + type + '" style="cursor:hand;cursor:pointer;" >'
                    + inputText.substring(start, end) + '</span></span>';
                pos = end;
            });

            outputString += inputText.substring(pos, inputText.length);

            return outputString;
        }


        function getPlainType(type) {
            return type.replace("<", "").replace(">", "");
        }

        // Transformation to HTML
        function spanToHtml(span, topPos) {
            let string = "";

            //We remove the < and > to avoid messing up with HTML
            let type = getPlainType(span.type);

            let text = span.text;
            let formattedText = span.formattedText;

            string += "<div class='info-sense-box ___TYPE___'";
            if (topPos !== -1)
                string += " style='vertical-align:top; position:relative; top:" + topPos + ";cursor:hand;cursor:pointer;'";
            else
                string += " style='cursor:hand;cursor:pointer;'";

            string += ">";
            if (span.links && topPos > -1) {
                let infobox_id = "infobox" + span.id;
                string += "<h2 class='ml-1' style='color:#FFF;font-size:16pt;'>" + type + "<img id='" + infobox_id + "' src='resources/icons/arrow-up.svg'/></h2>";
            } else {
                string += "<h2 class='ml-1' style='color:#FFF;font-size:16pt;'>" + type + "</h2>";
            }

            string += "<div class='container-fluid border' style='background-color:#FFF;color:#70695C'>";
            // "<table style='width:100%;display:inline-table;'><tr style='display:inline-table;'><td>";

            if (formattedText) {
                string += "<p>name: <b>" + formattedText + "</b></p>";
            } else {
                string += "<p>name: <b>" + text + "</b></p>";
            }

            if (span.links) {
                let colorLabel = transformToLinkableType(type, span.links)
                string = string.replace("___TYPE___", colorLabel);
                let linkedEntities = "";
                let first = true;
                span.links.forEach(function (link, linkIdx) {
                    if (!first) {
                        linkedEntities += ", ";
                    }
                    first = false;
                    linkedEntities += "<b>" + link.targetText + "</b> (" + getPlainType(link.targetType) + ") [" + link.type + "]";

                });
                string += "<p>Linked: " + linkedEntities + "</p>";
            }

            string = string.replace("___TYPE___", type);

            if (span.attributes) {
                let previousPrefix = "";
                let resolvedFormulas = [];
                let formula = "";
                let attributeHtmlString = "<div class='border col-12 p-0'>";
                Object.keys(span.attributes).sort().forEach(function (key) {
                    let splits = key.split("_");
                    let prefix = splits[0];
                    let propertyName = splits[1];

                    if (propertyName === "formula") {
                        formula = span.attributes[key];
                        attributeHtmlString += "<row><div class='col-12'>" + propertyName + ": <strong>" + span.attributes[key] + "</strong></div></row>";
                    } else if (propertyName === 'rawTaggedValue') {
                        //Ignoring

                    } else if (propertyName === 'resolvedFormula') {
                        resolvedFormulas.push(span.attributes[key])
                    } else {
                        attributeHtmlString += "<row><div class='col-12'>" + propertyName + ": <strong>" + span.attributes[key] + "</strong></div></row>";
                    }
                    previousPrefix = prefix;
                });

                if (resolvedFormulas.length > 0 && resolvedFormulas[0] !== formula) {
                    attributeHtmlString += "<row><div class='col-12'>resolvedFormula: <strong>" + resolvedFormulas.join(", ") + "</strong></div></row>";
                }
                attributeHtmlString += "</div>";

                string += attributeHtmlString;
            }

            string += "</div>";
            string += "</div>";

            return string;
        }

        let examples_superconductors = [
            "The critical temperature T C = 4.7 K discovered for La 3 Ir 2 Ge 2 in this work is by about 1.2 K higher than that found for La 3 Rh 2 Ge 2 .",
            "For intercalated graphite, T c is reported to be 11.4 K for CaC 6 , 4 and for alkalidoped fullerides, T c ¼ 33 K in RbCs 2 C 60 .",
            "In just a few months, the superconducting transition temperature (Tc) was increased to 55 K in the electron-doped system, as well as 25 K in hole-doped La1−x SrxOFeAs compound. Soon after, single crystals of LnFeAs(O1−x Fx) (Ln = Pr, Nd, Sm) were grown successfully by the NaCl/KCl flux method, though the sub-millimeter sizes limit the experimental studies on them. Therefore, FeAs-based single crystals with high crystalline quality, homogeneity and large sizes are highly desired for precise measurements of the properties. Very recently, the BaFe2As2 compound in a tetragonal ThCr2Si2-type structure with infinite Fe–As layers was reported. By replacing the alkaline earth elements (Ba and Sr) with alkali elements (Na, K, and Cs), superconductivity up to 38 K was discovered both in hole-doped and electron-doped samples. Tc leties from 2.7 K in CsFe2As2 to 38 K in A1−xKxFe2As2 (A = Ba, Sr). Meanwhile, superconductivity could also be induced in the parent phase by high pressure or by replacing some of the Fe by Co. More excitingly, large single crystals could be obtained by the Sn flux method in this family to study the rather low melting temperature and the intermetallic characteristics.",
            "The crystal structure of (Sr, Na)Fe 2 As 2 has been refined for polycrystalline samples in the range of 0 ⩽ x ⩽ 0.42 with a maximum T c of 26 K ."
        ]

        let examples_materials = [
            "hole-doped La1−x SrxOyFe1-yAs compound with (x = 0.1, 0.2 and 0.3 and y = 0.1, 0.4 and 0.5)",
            "polycrystalline samples in the range of 0 < x < 0.42",
            "(Sr, Na)Fe 2 As 2 thin wire",
            "hole-doped La1−x SrxOFeAs with x = 0.2, 0.3 and 0.6"
        ]
    }

)(jQuery);



