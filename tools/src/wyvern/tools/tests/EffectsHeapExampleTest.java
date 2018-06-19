package wyvern.tools.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import wyvern.target.corewyvernIL.expression.StringLiteral;
import wyvern.target.corewyvernIL.support.Util;
import wyvern.tools.imports.extensions.WyvernResolver;
import wyvern.tools.parsing.coreparser.ParseException;
import wyvern.tools.tests.suites.RegressionTests;

/**
 * This class is separate from EffectSystemTests because the PATH has to be
 * different for each example to run.
 *
 * For some reason, this test fails if placed after the CoreParserTests in
 * AntRegressionTestSuite.
 *
 * @author justinlubin
 */
@Category(RegressionTests.class)
public class EffectsHeapExampleTest {
    private static final String BASE_PATH = TestUtil.BASE_PATH;
    private static final String PATH = BASE_PATH + "effectsHeap/";

    @BeforeClass
    public static void setupResolver() {
        TestUtil.setPaths();
        WyvernResolver.getInstance().addPath(PATH);
    }

    @Test
    public void testHeapExample() throws ParseException {
        TestUtil.doTestScriptModularly(PATH, "main", Util.stringType(), new StringLiteral("goodbye"));
    }
}