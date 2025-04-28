/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.report.items;

public class HtmlBuilder {
    private static String bvHighlighterBulkValidatorStyles = " .IDENTICAL { color:black;  background-color: "
            + "lightgreen; } .SIMILAR { color: #000000; background-color: #FFFF77; } .NORMAL { color:blue; } .CHANGED {"
            + " color:#ff9999; } .MODIFIED { background-color:#ff9999; } .EXTRA { background-color: orange; } .MISSED {"
            + " background-color: #C7EDFC; } .ERROR { background-color:yellow; color:red } .highlight-container "
            + ".line-numbers .line-number { background-color:#EEEEEE; color: #B7B7B7; border-right: 1px solid #ccc; "
            + "padding-left: 20px; text-align: right; /* make line number as unselectable */ -webkit-user-select: none;"
            + " /* Chrome/Safari */ -moz-user-select: none; /* Firefox */ -ms-user-select: none; /* IE10+ */ /* Rules "
            + "below not implemented in browsers yet */ -o-user-select: none; user-select: none; } .highlight-container"
            + " .text-content-wrapper { overflow : hidden; overflow-x : scroll; } .highlight-container .text-content { "
            + "margin:0; width:100%; } .highlight-container .text-content span.modified { background-color:#F2DEDE; } "
            + ".highlight-container .text-content span.skip { background-color:#EEEEEE; text-decoration:line-through; }"
            + " /* END :: XML XPath highlighter */ /* Bootstrap */ /* Add margin for button toolbal in tab,"
            + "callout */ .callout .btn-toolbar:first-child, .tab-pane .btn-toolbar:first-child { margin-top: 10px; "
            + "margin-bottom: 10px; } /* Add bold text and background for optgroup */ .form-control optgroup { "
            + "font-weight: bold; } .form-control optgroup option { font-weight: normal; } /* Change font size for PRE "
            + "*/ .container-step-details pre { font-size: 10px; } /* XLarge modal Size */ .modal-xl { width: 1300px; }"
            + " .modal-full-width { width: 100%; margin-left: 0px; margin-right: 0px; } .testrun-container .popover{ "
            + "z-index: 3000; } /* Custom Label Color */ .label.label-none { border:1px solid #616161; color: #616161; "
            + "} /* Show spinner by center page */ .spinner-center { position: fixed; top: 50%; left: 50%; /* bring "
            + "your own prefixes */ transform: translate(-50%, -50%); } /* COMPARATOR RESULTS */ "
            + ".comparator-result-identical { background-color:#DFF0D8 !important; } "
            + ".comparator-result-identical-with-wrong-step { background: repeating-linear-gradient( 135deg, #FBBD7F, "
            + "#FBBD7F 2px, #DFF0D8 3px, #DFF0D8 20px ); } .comparator-result-changed { background-color:#F2DEDE "
            + "!important; } .comparator-result-missed-step { background: repeating-linear-gradient( 135deg, #EDD3D3, "
            + "#EDD3D3 10px, #F2DEDE 10px, #F2DEDE 20px ); } .comparator-result-changed-with-wrong-step { background: "
            + "repeating-linear-gradient( 135deg, #FBBD7F, #FBBD7F 5px, #F2DEDE 5px, #F2DEDE 20px ); } "
            + ".comparator-result-similar { background-color: #FCFC90 !important; } "
            + ".comparator-result-similar-with-wrong-step { background: repeating-linear-gradient( 135deg, #FBBD7F, "
            + "#FBBD7F 5px, #FCFC90 5px, #FCFC90 20px ); } .comparator-result-wrong-step { background-color:#FBBD7F "
            + "!important; }\n";
    // Styles copied from itf.css - for future use if we change ATP table-view into div-view for Validation results;
    // commented.
    /*
    private static String bvHighlighterItfStyles = "\n" +
            ".highlighter-container .hl-step-er,\n" +
            ".highlighter-container .hl-step-ar {\n" +
            "    font-size: 12px; border: 1px solid #ccc; padding: 3px; font-family: Menlo,Monaco,Consolas,\"Courier
            New\",monospace;\n" +
            "}\n" +
            ".highlighter-container .HIDDEN {\n" +
            "    color: lightgrey; background-color: lightgrey;\n" +
            "}\n" +
            ".highlighter-container .SIMILAR {\n" +
            "    color: #000000; background-color: #FFFF77;\n" +
            "}\n" +
            ".highlighter-container .NORMAL {\n" +
            "    color:blue;\n" +
            "}\n" +
            ".highlighter-container .CHANGED {\n" +
            "    color:#ff9999;\n" +
            "}\n" +
            ".highlighter-container .IDENTICAL {\n" +
            "    color:black; background-color:lightgreen;\n" +
            "}\n" +
            ".highlighter-container .MODIFIED {\n" +
            "    background-color:#ff9999;\n" +
            "}\n" +
            ".highlighter-container .MISSED {\n" +
            "    background-color:#C7EDFC;\n" +
            "}\n" +
            ".highlighter-container .EXTRA {\n" +
            "    background-color:orange;\n" +
            "}\n" +
            ".highlighter-container .FAILED {\n" +
            "    background-color:#ff0000; font-style:italic\n" +
            "}\n" +
            ".highlighter-container .ERROR {\n" +
            "    background-color:yellow; color:red\n" +
            "}\n" +
            ".highlighter-container .BROKEN_STEP_INDEX {\n" +
            "    background-color:yellow; color:red\n" +
            "}\n" +
            ".highlighter-container .EMPTY_ROW {\n" +
            "    background: lightgrey;\n" +
            "}";
    */
    private StringBuilder builder = new StringBuilder();

    public HtmlBuilder() {
        builder.append("<html>");
        builder.append("<script type='application/javascript' src='/scripts/jquery.js'></script>");
        builder.append("<style>").append("table {\n")
                .append("    border-collapse: collapse;\n")
                .append("}\n")
                .append("\n")
                .append("table, th, td {\n")
                .append("    border: 1px solid black;\n")
                .append("}\n")
                .append('\n')
                .append("td {\n")
                .append("    white-space: pre-wrap;\n")
                .append("}\n")
                .append(bvHighlighterBulkValidatorStyles)
                //.append(bvHighlighterItfStyles)
                .append("</style>");
        builder.append("<script type='application/javascript'>").append("function toggle(root) {\n" +
                "\tjQuery(root).parent().find('pre').toggle();\n" +
                "\tjQuery(root).text(jQuery(root).text() == 'Collapse'?'Expand':'Collapse');\n" +
                "}").append("</script>");
    }

    public HtmlBuilder beginTable(String title) {
        builder.append("<h3>").append(title).append("</h3>").append("<br/>").append("<table>");
        return this;
    }

    public HtmlBuilder beginTable() {
        builder.append("<table>");
        return this;
    }

    public HtmlBuilder endTable() {
        builder.append("</table>");
        return this;
    }

    public HtmlBuilder beginRow() {
        builder.append("<tr>");
        return this;
    }

    public HtmlBuilder endRow() {
        builder.append("</tr>");
        return this;
    }

    public HtmlBuilder addRow(String... values) {
        beginRow();
        for (String value : values) {
            cell(value);
        }
        endRow();
        return this;
    }

    public HtmlBuilder beginDiv() {
        builder.append("<div>");
        return this;
    }

    public HtmlBuilder endDiv() {
        builder.append("</div>");
        return this;
    }

    public HtmlBuilder addHeaders(String... values) {
        beginRow();
        for (String value : values) {
            header(value);
        }
        endRow();
        return this;
    }

    public HtmlBuilder cell(String value) {
        beginCell().addValue(value).endCell();
        return this;
    }

    public HtmlBuilder beginCell() {
        builder.append("<td>");
        return this;
    }

    public HtmlBuilder endCell() {
        builder.append("</td>");
        return this;
    }

    public HtmlBuilder header(String header) {
        builder.append("<th>").append(header).append("</th>");
        return this;
    }

    public HtmlBuilder addValue(String element) {
        builder.append(element);
        return this;
    }

    public HtmlBuilder element(String element) {
        builder.append(element);
        return this;
    }

    public String build() {
        return builder.append("</html>").toString();
    }


}
