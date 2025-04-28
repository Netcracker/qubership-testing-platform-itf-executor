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

package org.qubership.automation.itf.report;

public class RAM2ReportAdapterTest {

    private final String SIMPLESNAPSHOT = "<html><script type='application/javascript' src='/scripts/jquery" +
            ".js'></script><style>table {\n" +
            "    border-collapse: collapse;\n" +
            "}\n" +
            "\n" +
            "table, th, td {\n" +
            "    border: 1px solid black;\n" +
            "}\n" +
            "\n" +
            "td {\n" +
            "    white-space: pre-wrap;\n" +
            "}\n" +
            "</style><script type='application/javascript'>function toggle(root) {\n" +
            "\tjQuery(root).parent().find('pre').toggle();\n" +
            "\tjQuery(root).text(jQuery(root).text() == 'Collapse'?'Expand':'Collapse');\n" +
            "}</script><div><h3>Properties</h3><br/><table><tr><th>Key</th><th>Value</th></tr><tr><td>Message</td><td" +
            "><a href='#' onclick='toggle(this)'>Collapse</a><pre><pre></pre></td></tr><tr><td>Response " +
            "Message</td><td><a href='#' onclick='toggle(this)" +
            "'>Collapse</a><pre><pre>incomingMessage</pre></td></tr><tr><td>Response " +
            "Headers</td><td><table></table></td></tr><tr><td>Step Context</td><td><pre>{\n" +
            "  &quot;sp2&quot;: 2,\n" +
            "  &quot;sp1&quot;: 1,\n" +
            "  &quot;messageParam1&quot;: &quot;value1&quot;\n" +
            "}</pre></td></tr><tr><td>Testcase Context</td><td><a href='#' onclick='toggle(this)" +
            "'>Collapse</a><pre>{\n" +
            "  &quot;portnumber&quot;: [\n" +
            "    23,\n" +
            "    24,\n" +
            "    25,\n" +
            "    26\n" +
            "  ]\n" +
            "}</pre></td></tr></table></div></html>";
/*
    @Test
    public void createMapWithSnapshot_stringWithHtml_returnedMapHasFileWithExpectedHtml() throws IOException {
        RAM2ReportAdapter ram2ReportAdapter = new RAM2ReportAdapter();
        HashMap<String, Object> mapWithSnapshot = null;
        String stringOfFile;
        File snapshot_file = null;

        try {
            mapWithSnapshot = ram2ReportAdapter.createMapWithSnapshot(SIMPLESNAPSHOT);
            snapshot_file = (File) mapWithSnapshot.get("screenshot_file");
            stringOfFile = FileUtils.readFileToString(snapshot_file);
        } finally {
            FileUtils.deleteQuietly(snapshot_file);
        }

        Assert.assertEquals("ram2Screenshot", mapWithSnapshot.get("screenshot_name"));
        Assert.assertEquals("text/html", mapWithSnapshot.get("screenshot_type"));
        Assert.assertEquals(SIMPLESNAPSHOT, stringOfFile);
    }
 */
}
