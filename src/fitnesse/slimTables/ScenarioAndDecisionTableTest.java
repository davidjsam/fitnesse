// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.slimTables;

import fitnesse.slim.SlimClient;
import static fitnesse.testutil.RegexTestCase.assertSubString;
import static fitnesse.util.ListUtility.list;
import fitnesse.wiki.InMemoryPage;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.responders.run.slimResponder.MockSlimTestContext;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScenarioAndDecisionTableTest extends MockSlimTestContext {
  private WikiPage root;
  private List<Object> instructions;
  private ScenarioTable st;
  private DecisionTable dt;
  private Map<String, String> symbols = new HashMap<String, String>();
  private Map<String, ScenarioTable> scenarios = new HashMap<String, ScenarioTable>();

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("root");
    instructions = new ArrayList<Object>();
  }

  private void makeTables(String tableText) throws Exception {
    WikiPageUtil.setPageContents(root, tableText);
    TableScanner ts = new HtmlTableScanner(root.getData().getHtml());
    Table t = ts.getTable(0);
    st = new ScenarioTable(t, "s_id", this);
    t = ts.getTable(1);
    dt = new DecisionTable(t, "did", this);
    st.appendInstructions(instructions);
    dt.appendInstructions(instructions);
  }

  @Test
  public void oneInput() throws Exception {
    makeTables(
      "!|scenario|myScenario|input|\n" +
        "|function|@input|\n" +
        "\n" +
        "!|DT:myScenario|\n" +
        "|input|\n" +
        "|7|\n"
    );
    List<Object> expectedInstructions =
      list(
        list("scriptTable_did.0_0", "call", "scriptTableActor", "function", "7")
      );
    assertEquals(expectedInstructions, instructions);
  }

  @Test
  public void manyInputsAndRows() throws Exception {
    makeTables(
      "!|scenario|login|user name|password|password|pin|pin|\n" +
        "|login|@userName|with password|@password|and pin|@pin|\n" +
        "\n" +
        "!|DT:LoginPasswordPin|\n" +
        "|user name|password|pin|\n" +
        "|bob|xyzzy|7734|\n" +
        "|bill|yabba|8892|\n"
    );
    List<Object> expectedInstructions =
      list(
        list("scriptTable_did.0_0", "call", "scriptTableActor", "loginWithPasswordAndPin", "bob", "xyzzy", "7734"),
        list("scriptTable_did.1_0", "call", "scriptTableActor", "loginWithPasswordAndPin", "bill", "yabba", "8892")
      );
    assertEquals(expectedInstructions, instructions);
  }

  @Test
  public void simpleInputAndOutputPassing() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|output|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|7|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("scriptTable_did.0_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);

    String scriptTable = dt.getChild(0).getTable().toString();
    String expectedScript =
      "[[scenario, echo, input, giving, output], [check, echo, 7, pass(7)]]";
    assertEquals(expectedScript, scriptTable);
    String dtHtml = dt.getTable().toString();
    assertSubString("<span id=\"test_status\" class=pass>Scenario</span>", dtHtml);
    assertEquals(1, dt.getTestSummary().right);
    assertEquals(0, dt.getTestSummary().wrong);
    assertEquals(0, dt.getTestSummary().ignores);
    assertEquals(0, dt.getTestSummary().exceptions);
  }

  @Test
  public void simpleInputAndOutputFailing() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|output|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|8|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("scriptTable_did.0_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);

    String scriptTable = dt.getChild(0).getTable().toString();
    String expectedScript =
      "[[scenario, echo, input, giving, output], [check, echo, 7, [7] fail(expected [8])]]";
    assertEquals(expectedScript, scriptTable);
    String dtHtml = dt.getTable().toString();
    assertSubString("<span id=\"test_status\" class=fail>Scenario</span>", dtHtml);
    assertEquals(0, dt.getTestSummary().right);
    assertEquals(1, dt.getTestSummary().wrong);
    assertEquals(0, dt.getTestSummary().ignores);
    assertEquals(0, dt.getTestSummary().exceptions);
  }

  @Test
  public void scenarioHasTooFewArguments() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|8|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("scriptTable_did.0_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);
    String dtHtml = dt.getTable().toString();
    assertSubString("<span class=\"fail\">DT:EchoGiving: Bad table:", dtHtml);
    assertSubString("The argument output is not an input to the scenario.", dtHtml);
  }

  @Test
  public void scenarioHasExtraArgumentsThatAreIgnored() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|output||output2|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|7|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("scriptTable_did.0_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);

    String scriptTable = dt.getChild(0).getTable().toString();
    String expectedScript =
      "[[scenario, echo, input, giving, output, , output2], [check, echo, 7, pass(7)]]";
    assertEquals(expectedScript, scriptTable);
    String dtHtml = dt.getTable().toString();
    assertSubString("<span id=\"test_status\" class=pass>Scenario</span>", dtHtml);
  }
}
